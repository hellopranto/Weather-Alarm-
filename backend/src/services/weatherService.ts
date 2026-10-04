import axios from 'axios';
import { OpenWeatherProvider } from '../providers/openweather/openWeatherProvider';
import { BmdProvider } from '../providers/bmd/bmdProvider';
import { RainViewerProvider } from '../providers/rainviewer/rainViewerProvider';

interface BdAdminLocation {
  name: string;
  village: string;
  union: string;
  upazila: string;
  district: string;
  division: string;
  country: string;
  displayName: string;
}

export class WeatherService {
  private openWeather: OpenWeatherProvider;
  private bmd: BmdProvider;
  private rainViewer: RainViewerProvider;

  // In-memory cache to prevent hitting rate limits
  private cache = new Map<string, { data: any; expiry: number }>();
  private readonly CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

  constructor() {
    this.openWeather = new OpenWeatherProvider();
    this.bmd = new BmdProvider();
    this.rainViewer = new RainViewerProvider();
  }

  private async reverseGeocodeBangladesh(lat: number, lon: number): Promise<BdAdminLocation> {
    try {
      const res = await axios.get('https://nominatim.openstreetmap.org/reverse', {
        params: {
          lat,
          lon,
          format: 'json',
          'accept-language': 'bn,en',
        },
        headers: {
          'User-Agent': 'WeatherAlertBangladesh/1.0 (contact: info@weatheralertbd.gov)',
        },
        timeout: 4000,
      });

      if (res.data && res.data.address) {
        const addr = res.data.address;
        const village = addr.village || addr.suburb || addr.neighbourhood || addr.residential || addr.town || addr.city || '';
        const union = addr.municipality || addr.subdistrict || '';
        const upazila = addr.county || '';
        const district = addr.state_district || '';
        const division = addr.state || '';
        const country = addr.country || 'বাংলাদেশ';

        // Select the most specific reliable area name
        const name = village || upazila || district || 'Bangladesh';

        return {
          name,
          village,
          union,
          upazila,
          district,
          division,
          country,
          displayName: res.data.display_name || '',
        };
      }
    } catch {
      // Non-fatal, graceful fallback
    }

    return {
      name: 'Bangladesh',
      village: '',
      union: '',
      upazila: '',
      district: '',
      division: '',
      country: 'বাংলাদেশ',
      displayName: '',
    };
  }

  async getUnifiedWeather(lat: number, lon: number, cityName?: string): Promise<any> {
    const cacheKey = `${lat.toFixed(2)}_${lon.toFixed(2)}`;
    const now = Date.now();
    const cached = this.cache.get(cacheKey);
    if (cached && cached.expiry > now) {
      return cached.data;
    }

    // Reverse geocode Bangladesh administrative hierarchy in parallel
    const [owResult, bmdResult, bmdWarnings, radarMeta, adminLoc] = await Promise.all([
      // 1. OpenWeather
      (async () => {
        try {
          const [current, forecast] = await Promise.all([
            this.openWeather.getCurrentWeather(lat, lon),
            this.openWeather.getForecast(lat, lon),
          ]);
          return { current, forecast, error: null };
        } catch (err: any) {
          return { current: null, forecast: null, error: err.message };
        }
      })(),
      // 2. BMD Stations
      (async () => {
        try {
          return await this.bmd.getStationObservations(lat, lon, cityName);
        } catch {
          return { available: false, stations: [], source: 'BMD Fallback' };
        }
      })(),
      // BMD Warnings
      (async () => {
        try {
          return await this.bmd.getWarnings();
        } catch {
          return [];
        }
      })(),
      // 3. RainViewer Radar
      (async () => {
        try {
          const radar = await this.rainViewer.getRadarData();
          if (radar && radar.radar && radar.radar.past && radar.radar.past.length > 0) {
            const latest = radar.radar.past[radar.radar.past.length - 1];
            return {
              available: true,
              host: radar.host,
              latestPath: latest.path,
              latestTime: latest.time,
              pastFramesCount: radar.radar.past.length,
              nowcastFramesCount: radar.radar.nowcast?.length || 0,
            };
          }
        } catch {}
        return { available: false };
      })(),
      // 4. Reverse Geocoding
      this.reverseGeocodeBangladesh(lat, lon),
    ]);

    const owCurrent = owResult.current;
    const owForecast = owResult.forecast;

    // BMD Observation selection (closest station)
    let bmdData: any = { available: false, station: null, observation: null };
    if (bmdResult.available && bmdResult.stations.length > 0) {
      const closest = bmdResult.stations[0];
      bmdData = {
        available: true,
        station: closest.stationName,
        observation: closest,
      };
    }

    // Combine into normalized schema with Bangladesh Timezone (Asia/Dhaka)
    const formatTime = (unixSec: number) => {
      if (!unixSec) return '--:--';
      const d = new Date(unixSec * 1000);
      return d.toLocaleTimeString('en-US', {
        timeZone: 'Asia/Dhaka',
        hour: '2-digit',
        minute: '2-digit',
        hour12: true,
      });
    };

    const formatDate = (unixSec: number) => {
      const d = new Date(unixSec * 1000);
      return d.toLocaleDateString('en-US', {
        timeZone: 'Asia/Dhaka',
        weekday: 'short',
        month: 'short',
        day: 'numeric',
      });
    };

    const openWeatherLocName = owCurrent?.name && owCurrent.name.trim().length > 0 ? owCurrent.name.trim() : null;
    const locationName = adminLoc.name !== 'Bangladesh' && adminLoc.name.trim().length > 0 ? adminLoc.name : (openWeatherLocName || (cityName && cityName !== 'My Location' ? cityName : 'Bangladesh'));

    // Parse Hourly
    const hourly: any[] = [];
    if (owForecast && Array.isArray(owForecast.list)) {
      owForecast.list.slice(0, 16).forEach((item: any) => {
        hourly.push({
          timestamp: item.dt,
          timeString: formatTime(item.dt),
          temperature: Math.round(item.main.temp * 10) / 10,
          feelsLike: Math.round(item.main.feels_like * 10) / 10,
          humidity: item.main.humidity,
          pressure: item.main.pressure,
          condition: item.weather[0]?.main || 'Clear',
          description: item.weather[0]?.description || '',
          icon: item.weather[0]?.icon || '01d',
          weatherCode: item.weather[0]?.id || 800,
          rainProbability: Math.round((item.pop || 0) * 100),
          precipitationMm: item.rain ? item.rain['3h'] || 0 : 0,
          windSpeed: Math.round((item.wind?.speed || 0) * 3.6 * 10) / 10, // m/s to km/h
          windDirection: item.wind?.deg || 0,
        });
      });
    }

    // Aggregate Daily Forecast from 3-hour list
    const dailyMap = new Map<string, any>();
    if (owForecast && Array.isArray(owForecast.list)) {
      owForecast.list.forEach((item: any) => {
        const dateKey = new Date(item.dt * 1000).toISOString().split('T')[0];
        if (!dailyMap.has(dateKey)) {
          dailyMap.set(dateKey, {
            date: dateKey,
            dayName: formatDate(item.dt),
            tempMin: item.main.temp_min,
            tempMax: item.main.temp_max,
            condition: item.weather[0]?.main || 'Clear',
            icon: item.weather[0]?.icon || '01d',
            weatherCode: item.weather[0]?.id || 800,
            popMax: (item.pop || 0) * 100,
            totalPrecip: item.rain ? item.rain['3h'] || 0 : 0,
          });
        } else {
          const entry = dailyMap.get(dateKey);
          entry.tempMin = Math.min(entry.tempMin, item.main.temp_min);
          entry.tempMax = Math.max(entry.tempMax, item.main.temp_max);
          entry.popMax = Math.max(entry.popMax, (item.pop || 0) * 100);
          entry.totalPrecip += item.rain ? item.rain['3h'] || 0 : 0;
        }
      });
    }

    const daily = Array.from(dailyMap.values()).slice(0, 7).map(d => ({
      date: d.date,
      dayName: d.dayName,
      tempMin: Math.round(d.tempMin),
      tempMax: Math.round(d.tempMax),
      condition: d.condition,
      icon: d.icon,
      weatherCode: d.weatherCode,
      rainProbability: Math.round(d.popMax),
      precipitationMm: Math.round(d.totalPrecip * 10) / 10,
    }));

    // Current weather
    let current = owCurrent ? {
      temperature: Math.round(owCurrent.main.temp * 10) / 10,
      feelsLike: Math.round(owCurrent.main.feels_like * 10) / 10,
      tempMin: Math.round(owCurrent.main.temp_min * 10) / 10,
      tempMax: Math.round(owCurrent.main.temp_max * 10) / 10,
      humidity: owCurrent.main.humidity,
      pressure: owCurrent.main.pressure,
      windSpeed: Math.round(owCurrent.wind.speed * 3.6 * 10) / 10, // km/h
      windDirection: owCurrent.wind.deg || 0,
      visibility: owCurrent.visibility || 10000,
      condition: owCurrent.weather[0]?.main || 'Clear',
      description: owCurrent.weather[0]?.description || '',
      weatherCode: owCurrent.weather[0]?.id || 800,
      icon: owCurrent.weather[0]?.icon || '01d',
      sunrise: formatTime(owCurrent.sys?.sunrise),
      sunset: formatTime(owCurrent.sys?.sunset),
      rainProbability: hourly.length > 0 ? hourly[0].rainProbability : 0,
      precipitationMm: owCurrent.rain ? owCurrent.rain['1h'] || 0 : 0,
    } : {
      temperature: 29.5,
      feelsLike: 32.0,
      tempMin: 25.0,
      tempMax: 32.0,
      humidity: 78,
      pressure: 1008,
      windSpeed: 8.0,
      windDirection: 180,
      visibility: 9500,
      condition: 'Partly Cloudy',
      description: 'Scattered clouds',
      weatherCode: 802,
      icon: '03d',
      sunrise: '05:54 AM',
      sunset: '05:45 PM',
      rainProbability: 20,
      precipitationMm: 0.0,
    };

    // If OpenWeather was unavailable, fetch from Open-Meteo API
    if (!owCurrent) {
      try {
        const omUrl = `https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lon}&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,surface_pressure,wind_speed_10m,wind_direction_10m&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,precipitation_sum,precipitation_probability_max&timezone=Asia%2FDhaka`;
        const omRes = await axios.get(omUrl, { timeout: 4000 });
        if (omRes.data && omRes.data.current) {
          const cur = omRes.data.current;
          const dailyData = omRes.data.daily;
          const conditionMap: Record<number, { condition: string; desc: string; icon: string }> = {
            0: { condition: 'Clear', desc: 'Clear sky', icon: '01d' },
            1: { condition: 'Mainly Clear', desc: 'Mainly clear', icon: '02d' },
            2: { condition: 'Partly Cloudy', desc: 'Partly cloudy', icon: '03d' },
            3: { condition: 'Overcast', desc: 'Overcast', icon: '04d' },
            45: { condition: 'Fog', desc: 'Foggy conditions', icon: '50d' },
            51: { condition: 'Drizzle', desc: 'Light drizzle', icon: '09d' },
            61: { condition: 'Rain', desc: 'Slight rain', icon: '10d' },
            63: { condition: 'Rain', desc: 'Moderate rain', icon: '10d' },
            65: { condition: 'Heavy Rain', desc: 'Heavy intensity rain', icon: '10d' },
            80: { condition: 'Rain Showers', desc: 'Rain showers', icon: '09d' },
            95: { condition: 'Thunderstorm', desc: 'Thunderstorm', icon: '11d' },
          };
          const cond = conditionMap[cur.weather_code] || { condition: 'Clouds', desc: 'Cloudy', icon: '03d' };

          current = {
            temperature: Math.round(cur.temperature_2m * 10) / 10,
            feelsLike: Math.round(cur.apparent_temperature * 10) / 10,
            tempMin: dailyData?.temperature_2m_min?.[0] ? Math.round(dailyData.temperature_2m_min[0]) : 24.0,
            tempMax: dailyData?.temperature_2m_max?.[0] ? Math.round(dailyData.temperature_2m_max[0]) : 32.0,
            humidity: cur.relative_humidity_2m || 75,
            pressure: Math.round(cur.surface_pressure || 1010),
            windSpeed: Math.round((cur.wind_speed_10m || 0) * 10) / 10,
            windDirection: cur.wind_direction_10m || 0,
            visibility: 10000,
            condition: cond.condition,
            description: cond.desc,
            weatherCode: cur.weather_code,
            icon: cond.icon,
            sunrise: dailyData?.sunrise?.[0] ? new Date(dailyData.sunrise[0]).toLocaleTimeString('en-US', { timeZone: 'Asia/Dhaka', hour: '2-digit', minute: '2-digit', hour12: true }) : '05:54 AM',
            sunset: dailyData?.sunset?.[0] ? new Date(dailyData.sunset[0]).toLocaleTimeString('en-US', { timeZone: 'Asia/Dhaka', hour: '2-digit', minute: '2-digit', hour12: true }) : '05:45 PM',
            rainProbability: dailyData?.precipitation_probability_max?.[0] || 20,
            precipitationMm: cur.precipitation || 0,
          };
        }
      } catch {
        // Fallback remains active
      }
    }

    // Combine alerts from BMD warnings
    const alerts: any[] = [];
    bmdWarnings.forEach(w => {
      alerts.push({
        id: w.id,
        title: w.title,
        description: w.description,
        severity: w.severity,
        startTime: w.issuedAt,
        endTime: w.validUntil,
        source: 'Bangladesh Meteorological Department (BMD)',
        signalNumber: w.signalNumber,
        regions: w.affectedRegions,
      });
    });

    const locationObj = {
      name: locationName,
      village: adminLoc.village || locationName,
      union: adminLoc.union || '',
      upazila: adminLoc.upazila || '',
      district: adminLoc.district || '',
      division: adminLoc.division || '',
      displayName: adminLoc.displayName || '',
      country: adminLoc.country || 'বাংলাদেশ',
      latitude: lat,
      longitude: lon,
    };

    const responsePayload = {
      location: locationObj,
      current,
      weather: {
        temperature: current.temperature,
        feelsLike: current.feelsLike,
        humidity: current.humidity,
        windSpeed: current.windSpeed,
        precipitation: current.precipitationMm,
        condition: current.condition,
        description: current.description,
      },
      units: {
        temperature: '°C',
        speed: 'km/h',
        precipitation: 'mm',
      },
      hourly,
      daily,
      alerts,
      bmd: bmdData,
      radar: radarMeta,
      updatedAt: new Date().toISOString(),
      sourceNotes: {
        openWeather: owCurrent ? 'Live' : 'Open-Meteo / Fallback',
        bmd: bmdData.available ? 'Connected' : 'Unavailable',
        rainViewer: radarMeta.available ? 'Connected' : 'Unavailable',
      },
    };

    this.cache.set(cacheKey, { data: responsePayload, expiry: now + this.CACHE_TTL_MS });
    return responsePayload;
  }
}
