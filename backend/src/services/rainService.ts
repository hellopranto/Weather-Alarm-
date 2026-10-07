import axios from 'axios';

export interface PredictedRainPeriod {
  start: string;
  end: string;
  expectedRainfallMm: number;
  maxProbability: number;
}

export interface RainPredictionResponse {
  location: {
    latitude: number;
    longitude: number;
    timezone: string;
  };
  current: {
    precipitationMm: number;
    rainMm: number;
    showersMm: number;
    weatherCode: number;
    condition: string;
    temperature: number;
    humidity: number;
    windSpeed: number;
    windDirection: number;
  };
  summary24h: {
    maxRainProbability: number;
    totalExpectedRainfallMm: number;
    rainExpected: boolean;
    intensity: 'None' | 'Light' | 'Moderate' | 'Heavy' | 'Violent';
    predictedRainPeriods: PredictedRainPeriod[];
    advisoryEn: string;
    advisoryBn: string;
  };
  hourly: Array<{
    time: string;
    precipitationProbability: number;
    precipitationMm: number;
    rainMm: number;
    showersMm: number;
    weatherCode: number;
    condition: string;
    temperature: number;
    humidity: number;
    windSpeed: number;
  }>;
  daily: Array<{
    date: string;
    maxRainProbability: number;
    totalRainfallMm: number;
    weatherCode: number;
    condition: string;
    tempMax: number;
    tempMin: number;
  }>;
  source: string;
  updatedAt: string;
}

export class RainService {
  private cache = new Map<string, { data: RainPredictionResponse; expiry: number }>();
  private readonly CACHE_TTL_MS = 10 * 60 * 1000; // 10 minutes

  private getWeatherCondition(code: number): string {
    const map: Record<number, string> = {
      0: 'Clear sky',
      1: 'Mainly clear',
      2: 'Partly cloudy',
      3: 'Overcast',
      45: 'Foggy',
      48: 'Depositing rime fog',
      51: 'Light drizzle',
      53: 'Moderate drizzle',
      55: 'Dense drizzle',
      61: 'Slight rain',
      63: 'Moderate rain',
      65: 'Heavy rain',
      71: 'Slight snow',
      80: 'Slight rain showers',
      81: 'Moderate rain showers',
      82: 'Violent rain showers',
      95: 'Thunderstorm',
      96: 'Thunderstorm with slight hail',
      99: 'Thunderstorm with heavy hail',
    };
    return map[code] || 'Cloudy';
  }

  private classifyIntensity(hourlyMaxMm: number): 'None' | 'Light' | 'Moderate' | 'Heavy' | 'Violent' {
    if (hourlyMaxMm <= 0.05) return 'None';
    if (hourlyMaxMm < 2.5) return 'Light';
    if (hourlyMaxMm < 10.0) return 'Moderate';
    if (hourlyMaxMm < 50.0) return 'Heavy';
    return 'Violent';
  }

  async getRainPrediction(lat: number, lon: number): Promise<RainPredictionResponse> {
    const cacheKey = `${lat.toFixed(4)}_${lon.toFixed(4)}`;
    const now = Date.now();
    const cached = this.cache.get(cacheKey);
    if (cached && cached.expiry > now) {
      return cached.data;
    }

    const url = 'https://api.open-meteo.com/v1/forecast';
    const params = {
      latitude: lat,
      longitude: lon,
      current: [
        'temperature_2m',
        'relative_humidity_2m',
        'precipitation',
        'rain',
        'showers',
        'weather_code',
        'wind_speed_10m',
        'wind_direction_10m',
      ].join(','),
      hourly: [
        'precipitation_probability',
        'precipitation',
        'rain',
        'showers',
        'weather_code',
        'temperature_2m',
        'relative_humidity_2m',
        'wind_speed_10m',
        'wind_direction_10m',
      ].join(','),
      daily: [
        'weather_code',
        'temperature_2m_max',
        'temperature_2m_min',
        'precipitation_sum',
        'precipitation_probability_max',
        'rain_sum',
        'showers_sum',
      ].join(','),
      timezone: 'auto',
    };

    const response = await axios.get(url, { params, timeout: 5000 });
    const data = response.data;

    const hourlyTimes: string[] = data.hourly?.time || [];
    const hourlyProb: number[] = data.hourly?.precipitation_probability || [];
    const hourlyPrecip: number[] = data.hourly?.precipitation || [];
    const hourlyRain: number[] = data.hourly?.rain || [];
    const hourlyShowers: number[] = data.hourly?.showers || [];
    const hourlyCodes: number[] = data.hourly?.weather_code || [];
    const hourlyTemps: number[] = data.hourly?.temperature_2m || [];
    const hourlyHumid: number[] = data.hourly?.relative_humidity_2m || [];
    const hourlyWind: number[] = data.hourly?.wind_speed_10m || [];

    // Slice next 48 hours
    const limit = Math.min(hourlyTimes.length, 48);
    const hourlyList: RainPredictionResponse['hourly'] = [];
    for (let i = 0; i < limit; i++) {
      const code = hourlyCodes[i] ?? 0;
      hourlyList.push({
        time: hourlyTimes[i],
        precipitationProbability: hourlyProb[i] ?? 0,
        precipitationMm: Math.round((hourlyPrecip[i] ?? 0) * 10) / 10,
        rainMm: Math.round((hourlyRain[i] ?? 0) * 10) / 10,
        showersMm: Math.round((hourlyShowers[i] ?? 0) * 10) / 10,
        weatherCode: code,
        condition: this.getWeatherCondition(code),
        temperature: Math.round((hourlyTemps[i] ?? 25) * 10) / 10,
        humidity: Math.round(hourlyHumid[i] ?? 70),
        windSpeed: Math.round((hourlyWind[i] ?? 0) * 10) / 10,
      });
    }

    // 24-hour summary calculations
    const next24 = hourlyList.slice(0, 24);
    let maxProb = 0;
    let sumPrecip = 0;
    let maxHourlyRate = 0;

    next24.forEach((h) => {
      if (h.precipitationProbability > maxProb) maxProb = h.precipitationProbability;
      sumPrecip += h.precipitationMm;
      if (h.precipitationMm > maxHourlyRate) maxHourlyRate = h.precipitationMm;
    });

    sumPrecip = Math.round(sumPrecip * 10) / 10;
    const intensity = this.classifyIntensity(maxHourlyRate);
    const rainExpected = maxProb >= 40 || sumPrecip >= 1.0;

    // Detect predicted rain periods (consecutive hours with probability >= 40% or precip >= 0.2mm)
    const periods: PredictedRainPeriod[] = [];
    let currentPeriod: { start: string; end: string; sumMm: number; maxP: number } | null = null;

    for (const h of next24) {
      const isRainingHour = h.precipitationProbability >= 40 || h.precipitationMm >= 0.2;
      if (isRainingHour) {
        if (!currentPeriod) {
          currentPeriod = {
            start: h.time,
            end: h.time,
            sumMm: h.precipitationMm,
            maxP: h.precipitationProbability,
          };
        } else {
          currentPeriod.end = h.time;
          currentPeriod.sumMm += h.precipitationMm;
          if (h.precipitationProbability > currentPeriod.maxP) {
            currentPeriod.maxP = h.precipitationProbability;
          }
        }
      } else if (currentPeriod) {
        periods.push({
          start: currentPeriod.start,
          end: currentPeriod.end,
          expectedRainfallMm: Math.round(currentPeriod.sumMm * 10) / 10,
          maxProbability: currentPeriod.maxP,
        });
        currentPeriod = null;
      }
    }

    if (currentPeriod) {
      const lastPeriod: { start: string; end: string; sumMm: number; maxP: number } = currentPeriod;
      periods.push({
        start: lastPeriod.start,
        end: lastPeriod.end,
        expectedRainfallMm: Math.round(lastPeriod.sumMm * 10) / 10,
        maxProbability: lastPeriod.maxP,
      });
    }

    // Advisories
    let advisoryEn = 'No significant rainfall expected over the next 24 hours. Clear travel conditions.';
    let advisoryBn = 'আগামী ২৪ ঘণ্টায় ভারী বৃষ্টির কোনো আশঙ্কা নেই। স্বাভাবিক চলাচল অব্যাহত রাখা যাবে।';

    if (intensity === 'Violent' || sumPrecip > 50) {
      advisoryEn = 'Heavy downpour warning! Waterlogging in low-lying areas and dangerous roads possible. Keep an umbrella and avoid waterlogged streets.';
      advisoryBn = 'ভারী বৃষ্টির সতর্কতা! নিম্নাঞ্চলে জলাবদ্ধতা ও পিচ্ছিল রাস্তার ঝুঁকি রয়েছে। ছাতা সাথে রাখুন এবং সাবধানে চলাচল করুন।';
    } else if (intensity === 'Heavy' || sumPrecip >= 25) {
      advisoryEn = 'Significant rain forecast over next 24 hours. Be prepared with rain gear and plan outdoor activities accordingly.';
      advisoryBn = 'আগামী ২৪ ঘণ্টায় উল্লেখযোগ্য বৃষ্টিপাতের পূর্বাভাস। বাইরে বের হওয়ার সময় ছাতা বা রেইনকোট সাথে রাখুন।';
    } else if (intensity === 'Moderate' || sumPrecip >= 5) {
      advisoryEn = 'Moderate showers predicted. Carry an umbrella when commuting.';
      advisoryBn = 'মাঝারি ধরনের বৃষ্টির সম্ভাবনা রয়েছে। যাতায়াতের সময় ছাতা সাথে রাখা শ্রেয়।';
    } else if (rainExpected) {
      advisoryEn = 'Passing light showers or drizzle possible. Low impact on daily routine.';
      advisoryBn = 'হালকা গুঁড়ি গুঁড়ি বৃষ্টি বা সাময়িক পশলা বৃষ্টির সম্ভাবনা। দৈনন্দিন কাজে তেমন বিঘ্ন ঘটবে না।';
    }

    // Daily 7 days
    const dailyDates: string[] = data.daily?.time || [];
    const dailyCodes: number[] = data.daily?.weather_code || [];
    const dailyMaxP: number[] = data.daily?.precipitation_probability_max || [];
    const dailyPrecipSum: number[] = data.daily?.precipitation_sum || [];
    const dailyTempMax: number[] = data.daily?.temperature_2m_max || [];
    const dailyTempMin: number[] = data.daily?.temperature_2m_min || [];

    const dailyList: RainPredictionResponse['daily'] = [];
    for (let i = 0; i < Math.min(dailyDates.length, 7); i++) {
      const code = dailyCodes[i] ?? 0;
      dailyList.push({
        date: dailyDates[i],
        maxRainProbability: dailyMaxP[i] ?? 0,
        totalRainfallMm: Math.round((dailyPrecipSum[i] ?? 0) * 10) / 10,
        weatherCode: code,
        condition: this.getWeatherCondition(code),
        tempMax: Math.round((dailyTempMax[i] ?? 30) * 10) / 10,
        tempMin: Math.round((dailyTempMin[i] ?? 22) * 10) / 10,
      });
    }

    const cur = data.current || {};
    const curCode = cur.weather_code ?? 0;

    const result: RainPredictionResponse = {
      location: {
        latitude: lat,
        longitude: lon,
        timezone: data.timezone || 'Asia/Dhaka',
      },
      current: {
        precipitationMm: Math.round((cur.precipitation ?? 0) * 10) / 10,
        rainMm: Math.round((cur.rain ?? 0) * 10) / 10,
        showersMm: Math.round((cur.showers ?? 0) * 10) / 10,
        weatherCode: curCode,
        condition: this.getWeatherCondition(curCode),
        temperature: Math.round((cur.temperature_2m ?? 28) * 10) / 10,
        humidity: Math.round(cur.relative_humidity_2m ?? 75),
        windSpeed: Math.round((cur.wind_speed_10m ?? 0) * 10) / 10,
        windDirection: Math.round(cur.wind_direction_10m ?? 0),
      },
      summary24h: {
        maxRainProbability: maxProb,
        totalExpectedRainfallMm: sumPrecip,
        rainExpected,
        intensity,
        predictedRainPeriods: periods,
        advisoryEn,
        advisoryBn,
      },
      hourly: hourlyList,
      daily: dailyList,
      source: 'Open-Meteo High-Resolution Weather Model',
      updatedAt: new Date().toISOString(),
    };

    this.cache.set(cacheKey, { data: result, expiry: now + this.CACHE_TTL_MS });
    return result;
  }
}
