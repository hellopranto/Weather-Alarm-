import axios from 'axios';
import {
  HourlyForecastItem,
  DailyForecastItem,
  AirQualityInfo,
  SunMoonInfo,
} from '../types/weather';
import {
  mapConditionToBengali,
  getAqiCategory,
  getMoonPhaseName,
  toBengaliNumerals,
} from '../utils/bengali';

interface ForecastData {
  hourly: HourlyForecastItem[];
  daily: DailyForecastItem[];
  airQuality: AirQualityInfo;
  sunMoon: SunMoonInfo;
  currentFallback?: {
    temperature: number | null;
    feelsLike: number | null;
    humidity: number | null;
    pressure: number | null;
    windSpeed: number | null;
    windDirection: number | null;
    visibility: number | null;
    rainfall: number | null;
    uvIndex: number | null;
    condition: string | null;
    conditionBn: string | null;
    weatherCode: number;
    icon: string;
  };
}

export class ForecastService {
  private cache = new Map<string, { data: ForecastData; timestamp: number }>();
  private readonly CACHE_TTL_MS = 30 * 60 * 1000; // 30 minutes

  async getForecast(lat: number, lon: number): Promise<ForecastData> {
    const cacheKey = `${lat.toFixed(2)}_${lon.toFixed(2)}`;
    const now = Date.now();
    const cached = this.cache.get(cacheKey);

    if (cached && now - cached.timestamp < this.CACHE_TTL_MS) {
      return cached.data;
    }

    try {
      // 1. Fetch real weather forecast from Open-Meteo (10-day forecast + 24h hourly)
      const forecastUrl =
        `https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lon}` +
        `&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,surface_pressure,wind_speed_10m,wind_direction_10m` +
        `&hourly=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation_probability,precipitation,weather_code,surface_pressure,wind_speed_10m,wind_direction_10m` +
        `&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,precipitation_sum,precipitation_probability_max,uv_index_max` +
        `&forecast_days=10&timezone=Asia%2FDhaka`;

      // 2. Fetch real Air Quality from Open-Meteo Air Quality API
      const aqiUrl =
        `https://air-quality-api.open-meteo.com/v1/air-quality?latitude=${lat}&longitude=${lon}` +
        `&current=us_aqi,pm10,pm2_5&timezone=Asia%2FDhaka`;

      const [forecastRes, aqiRes] = await Promise.allSettled([
        axios.get(forecastUrl, { timeout: 6000 }),
        axios.get(aqiUrl, { timeout: 4000 }),
      ]);

      const hourly: HourlyForecastItem[] = [];
      const daily: DailyForecastItem[] = [];
      let airQuality: AirQualityInfo = {
        aqi: null,
        category: null,
        categoryBn: null,
        pm25: null,
        pm10: null,
      };
      let sunMoon: SunMoonInfo = {
        sunrise: null,
        sunset: null,
        moonrise: null,
        moonset: null,
        moonPhase: null,
        moonPhaseBn: null,
      };

      let currentFallback: ForecastData['currentFallback'] = undefined;

      if (forecastRes.status === 'fulfilled' && forecastRes.value.data) {
        const data = forecastRes.value.data;

        // Current fallback values
        if (data.current) {
          const cur = data.current;
          const conditionMeta = this.mapWmoCode(cur.weather_code);
          currentFallback = {
            temperature: Math.round(cur.temperature_2m * 10) / 10,
            feelsLike: Math.round(cur.apparent_temperature * 10) / 10,
            humidity: cur.relative_humidity_2m,
            pressure: Math.round(cur.surface_pressure),
            windSpeed: Math.round((cur.wind_speed_10m || 0) * 10) / 10,
            windDirection: cur.wind_direction_10m || 0,
            visibility: 10000,
            rainfall: cur.precipitation || 0,
            uvIndex: data.daily?.uv_index_max?.[0] ? Math.round(data.daily.uv_index_max[0] * 10) / 10 : null,
            condition: conditionMeta.condition,
            conditionBn: conditionMeta.conditionBn,
            weatherCode: cur.weather_code,
            icon: conditionMeta.icon,
          };
        }

        // Parse hourly (next 24 hours)
        if (data.hourly && Array.isArray(data.hourly.time)) {
          const currentHourIso = new Date().toISOString().substring(0, 13);
          const startIndex = Math.max(
            0,
            data.hourly.time.findIndex((t: string) => t.startsWith(currentHourIso))
          );
          const limit = Math.min(data.hourly.time.length, startIndex + 24);

          for (let i = startIndex; i < limit; i++) {
            const timeIso = data.hourly.time[i];
            const ts = new Date(timeIso).getTime() / 1000;
            const wCode = data.hourly.weather_code[i] || 0;
            const meta = this.mapWmoCode(wCode);

            // Time string in Bangladesh format (e.g., 03:00 PM)
            const d = new Date(timeIso);
            const timeStr = d.toLocaleTimeString('en-US', {
              hour: '2-digit',
              minute: '2-digit',
              hour12: true,
            });

            const dateStr = d.toLocaleDateString('bn-BD', {
              day: 'numeric',
              month: 'short',
            });

            hourly.push({
              timestamp: ts,
              timeString: timeStr,
              dateString: dateStr,
              temperature: Math.round(data.hourly.temperature_2m[i] * 10) / 10,
              feelsLike: Math.round(data.hourly.apparent_temperature[i] * 10) / 10,
              humidity: data.hourly.relative_humidity_2m[i],
              pressure: Math.round(data.hourly.surface_pressure[i]),
              condition: meta.condition,
              conditionBn: meta.conditionBn,
              description: meta.condition,
              icon: meta.icon,
              weatherCode: wCode,
              rainProbability: data.hourly.precipitation_probability[i] || 0,
              precipitationMm: Math.round((data.hourly.precipitation[i] || 0) * 10) / 10,
              windSpeed: Math.round((data.hourly.wind_speed_10m[i] || 0) * 10) / 10,
              windDirection: data.hourly.wind_direction_10m[i] || 0,
            });
          }
        }

        // Parse 10-day daily forecast
        if (data.daily && Array.isArray(data.daily.time)) {
          for (let i = 0; i < data.daily.time.length; i++) {
            const dStr = data.daily.time[i];
            const dObj = new Date(dStr);
            const wCode = data.daily.weather_code[i] || 0;
            const meta = this.mapWmoCode(wCode);

            const dayNameEn = dObj.toLocaleDateString('en-US', { weekday: 'long' });
            const dayNameBn = this.getDayNameBn(dObj.getDay());
            const dateFormattedBn = this.formatBengaliDate(dObj);

            daily.push({
              date: dStr,
              dayName: dayNameEn,
              dayNameBn,
              dateFormattedBn,
              tempMin: Math.round(data.daily.temperature_2m_min[i]),
              tempMax: Math.round(data.daily.temperature_2m_max[i]),
              condition: meta.condition,
              conditionBn: meta.conditionBn,
              icon: meta.icon,
              weatherCode: wCode,
              rainProbability: data.daily.precipitation_probability_max[i] || 0,
              precipitationMm: Math.round((data.daily.precipitation_sum[i] || 0) * 10) / 10,
            });
          }

          // Extract Sun & Moon
          if (data.daily.sunrise?.[0] && data.daily.sunset?.[0]) {
            const sunriseDate = new Date(data.daily.sunrise[0]);
            const sunsetDate = new Date(data.daily.sunset[0]);

            const sunriseStr = sunriseDate.toLocaleTimeString('en-US', {
              hour: '2-digit',
              minute: '2-digit',
              hour12: true,
            });
            const sunsetStr = sunsetDate.toLocaleTimeString('en-US', {
              hour: '2-digit',
              minute: '2-digit',
              hour12: true,
            });

            // Calculate astronomical moon phase
            const moonPhaseFrac = this.calculateMoonPhase(new Date());
            const phaseName = getMoonPhaseName(moonPhaseFrac);

            sunMoon = {
              sunrise: sunriseStr,
              sunset: sunsetStr,
              moonrise: this.estimateMoonrise(sunriseDate, moonPhaseFrac),
              moonset: this.estimateMoonset(sunsetDate, moonPhaseFrac),
              moonPhase: phaseName.en,
              moonPhaseBn: phaseName.bn,
            };
          }
        }
      }

      // Air Quality
      if (aqiRes.status === 'fulfilled' && aqiRes.value.data?.current) {
        const curAqi = aqiRes.value.data.current;
        const usAqi = typeof curAqi.us_aqi === 'number' ? Math.round(curAqi.us_aqi) : null;
        const aqiCategory = getAqiCategory(usAqi);

        airQuality = {
          aqi: usAqi,
          category: aqiCategory.category,
          categoryBn: aqiCategory.categoryBn,
          pm25: typeof curAqi.pm2_5 === 'number' ? Math.round(curAqi.pm2_5 * 10) / 10 : null,
          pm10: typeof curAqi.pm10 === 'number' ? Math.round(curAqi.pm10 * 10) / 10 : null,
        };
      }

      const result: ForecastData = {
        hourly,
        daily,
        airQuality,
        sunMoon,
        currentFallback,
      };

      this.cache.set(cacheKey, { data: result, timestamp: now });
      return result;
    } catch (err: any) {
      // Return empty structures if network call fails completely (no fabricated data)
      return {
        hourly: [],
        daily: [],
        airQuality: { aqi: null, category: null, categoryBn: null, pm25: null, pm10: null },
        sunMoon: { sunrise: null, sunset: null, moonrise: null, moonset: null, moonPhase: null },
      };
    }
  }

  private mapWmoCode(code: number): { condition: string; conditionBn: string; icon: string } {
    switch (code) {
      case 0:
        return { condition: 'Clear Sky', conditionBn: 'পরিষ্কার আকাশ', icon: '01d' };
      case 1:
        return { condition: 'Mainly Clear', conditionBn: 'প্রধানত পরিষ্কার', icon: '02d' };
      case 2:
        return { condition: 'Partly Cloudy', conditionBn: 'আংশিক মেঘলা', icon: '03d' };
      case 3:
        return { condition: 'Overcast', conditionBn: 'প্রধানত মেঘলা', icon: '04d' };
      case 45:
      case 48:
        return { condition: 'Fog', conditionBn: 'কুয়াশা', icon: '50d' };
      case 51:
      case 53:
      case 55:
        return { condition: 'Drizzle', conditionBn: 'হালকা গুঁড়ি গুঁড়ি বৃষ্টি', icon: '09d' };
      case 61:
      case 63:
        return { condition: 'Rain', conditionBn: 'বৃষ্টি', icon: '10d' };
      case 65:
        return { condition: 'Heavy Rain', conditionBn: 'ভারী বৃষ্টি', icon: '10d' };
      case 80:
      case 81:
      case 82:
        return { condition: 'Rain Showers', conditionBn: 'বিক্ষিপ্ত বৃষ্টি', icon: '09d' };
      case 95:
      case 96:
      case 99:
        return { condition: 'Thunderstorm', conditionBn: 'বজ্রসহ বৃষ্টি', icon: '11d' };
      default:
        return { condition: 'Cloudy', conditionBn: 'মেঘলা আকাশ', icon: '03d' };
    }
  }

  private getDayNameBn(dayIndex: number): string {
    const days = ['রবিবার', 'সোমবার', 'মঙ্গলবার', 'বুধবার', 'বৃহস্পতিবার', 'শুক্রবার', 'শনিবার'];
    return days[dayIndex] || '';
  }

  private formatBengaliDate(d: Date): string {
    const day = toBengaliNumerals(d.getDate().toString().padStart(2, '0'));
    const months = [
      'জানুয়ারি', 'ফেব্রুয়ারি', 'মার্চ', 'এপ্রিল', 'মে', 'জুন',
      'জুলাই', 'আগস্ট', 'সেপ্টেম্বর', 'অক্টোবর', 'নভেম্বর', 'ডিসেম্বর',
    ];
    const month = months[d.getMonth()];
    const year = toBengaliNumerals(d.getFullYear());
    return `${day} ${month}, ${year}`;
  }

  private calculateMoonPhase(date: Date): number {
    // Known new moon: Jan 11, 2024 at 11:57 UTC
    const knownNewMoon = new Date('2024-01-11T11:57:00Z').getTime();
    const synodicMonth = 29.53058867 * 86400 * 1000;
    const diff = date.getTime() - knownNewMoon;
    const phase = (diff % synodicMonth) / synodicMonth;
    return phase < 0 ? phase + 1 : phase;
  }

  private estimateMoonrise(sunrise: Date, moonFraction: number): string {
    // Moonrise shifts by ~50 minutes per day relative to sun
    const base = new Date(sunrise.getTime() + moonFraction * 24 * 3600 * 1000);
    return base.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: true });
  }

  private estimateMoonset(sunset: Date, moonFraction: number): string {
    const base = new Date(sunset.getTime() + moonFraction * 24 * 3600 * 1000);
    return base.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: true });
  }
}
