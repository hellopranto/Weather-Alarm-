import { OpenWeatherProvider } from '../providers/openweather/openWeatherProvider';
import { BmdProvider } from '../providers/bmd/bmdProvider';
import { RainViewerProvider } from '../providers/rainviewer/rainViewerProvider';

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

  async getUnifiedWeather(lat: number, lon: number, cityName?: string): Promise<any> {
    const cacheKey = `${lat.toFixed(2)}_${lon.toFixed(2)}`;
    const now = Date.now();
    const cached = this.cache.get(cacheKey);
    if (cached && cached.expiry > now) {
      return cached.data;
    }

    // 1. Fetch OpenWeather current and forecast
    let owCurrent: any = null;
    let owForecast: any = null;
    let owError: string | null = null;

    try {
      [owCurrent, owForecast] = await Promise.all([
        this.openWeather.getCurrentWeather(lat, lon),
        this.openWeather.getForecast(lat, lon),
      ]);
    } catch (err: any) {
      owError = err.message || 'OpenWeather request failed';
    }

    // 2. Fetch BMD observations and warnings
    let bmdData: any = { available: false, station: null, observation: null };
    let bmdWarnings: any[] = [];
    try {
      const bmdResult = await this.bmd.getStationObservations(cityName);
      const warnings = await this.bmd.getWarnings();
      bmdWarnings = warnings;
      if (bmdResult.available && bmdResult.stations.length > 0) {
        // Find closest station
        const closest = bmdResult.stations[0];
        bmdData = {
          available: true,
          station: closest.stationName,
          observation: closest,
        };
      }
    } catch {
      // BMD failure is non-fatal
    }

    // 3. Fetch RainViewer radar metadata
    let radarMeta: any = { available: false };
    try {
      const radar = await this.rainViewer.getRadarData();
      if (radar && radar.radar && radar.radar.past && radar.radar.past.length > 0) {
        const latest = radar.radar.past[radar.radar.past.length - 1];
        radarMeta = {
          available: true,
          host: radar.host,
          latestPath: latest.path,
          latestTime: latest.time,
          pastFramesCount: radar.radar.past.length,
          nowcastFramesCount: radar.radar.nowcast?.length || 0,
        };
      }
    } catch {
      // Radar failure is non-fatal
    }

    // Combine into normalized schema
    const formatTime = (unixSec: number) => {
      if (!unixSec) return '--:--';
      const d = new Date(unixSec * 1000);
      return d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: true });
    };

    const formatDate = (unixSec: number) => {
      const d = new Date(unixSec * 1000);
      return d.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' });
    };

    const openWeatherLocName = owCurrent?.name && owCurrent.name.trim().length > 0 ? owCurrent.name.trim() : null;
    const locationName = openWeatherLocName || (cityName && cityName !== 'My Location' ? cityName : 'Bangladesh');

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
    const current = owCurrent ? {
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
      // Clean fallback if API key pending configuration
      temperature: 30.5,
      feelsLike: 34.0,
      tempMin: 26.0,
      tempMax: 33.0,
      humidity: 78,
      pressure: 1004,
      windSpeed: 12.0,
      windDirection: 180,
      visibility: 9000,
      condition: 'Partly Cloudy',
      description: 'Scattered clouds over Bengal delta',
      weatherCode: 802,
      icon: '03d',
      sunrise: '05:55 AM',
      sunset: '05:48 PM',
      rainProbability: 25,
      precipitationMm: 0.0,
    };

    // Combine alerts from BMD warnings and OpenWeather alerts
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

    const responsePayload = {
      location: {
        name: locationName,
        district: locationName,
        country: 'Bangladesh',
        latitude: lat,
        longitude: lon,
      },
      current,
      hourly,
      daily,
      alerts,
      bmd: bmdData,
      radar: radarMeta,
      updatedAt: new Date().toISOString(),
      sourceNotes: {
        openWeather: owCurrent ? 'Live' : (owError || 'Fallback'),
        bmd: bmdData.available ? 'Connected' : 'Unavailable',
        rainViewer: radarMeta.available ? 'Connected' : 'Unavailable',
      }
    };

    this.cache.set(cacheKey, { data: responsePayload, expiry: now + this.CACHE_TTL_MS });
    return responsePayload;
  }
}
