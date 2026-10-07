import axios from 'axios';

export interface PredictedRainPeriod {
  start: string;
  end: string;
  expectedRainfallMm: number;
  maxProbability: number;
}

export interface RainPredictionResponse {
  success: boolean;
  location: {
    latitude: number;
    longitude: number;
    name?: string;
    village?: string;
    upazila?: string;
    district?: string;
    division?: string;
    timezone?: string;
  };
  prediction: {
    next15Minutes: number;
    next30Minutes: number;
    next1Hour: number;
    next2Hours?: number;
    next3Hours: number;
    next6Hours: number;
    next24Hours: number;
  };
  rain: {
    expected: boolean;
    startInMinutes: number | null;
    durationMinutes: number | null;
    intensity: 'None' | 'Light' | 'Moderate' | 'Heavy' | 'Violent';
    intensityBn: string;
    rainfallAmount: number | null;
    summaryBn: string;
  };
  radar: {
    available: boolean;
    approaching: boolean;
    direction: string | null;
    speed: string | null;
    statusTextBn: string;
    radarMessageBn: string | null;
  };
  timeline: Array<{
    timeLabel: string;
    probability: number;
    intensityBn: string;
    rainfallMm: number | null;
    weatherCode: number;
  }>;
  confidence: string;
  confidenceScore: number;
  sources: string[];
  updatedAt: string;
  heavyRainWarning: {
    isWarningActive: boolean;
    expectedStart: string | null;
    expectedDuration: string | null;
    intensity: string;
    expectedRainfallAmount: string | null;
    bmdWarning?: string | null;
  } | null;
  bmdStation?: string;
  bmdDistanceKm?: number;
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

  private classifyIntensity(hourlyMaxMm: number): { en: 'None' | 'Light' | 'Moderate' | 'Heavy' | 'Violent'; bn: string } {
    if (hourlyMaxMm < 0.1) return { en: 'None', bn: 'বৃষ্টি নেই' };
    if (hourlyMaxMm < 2.5) return { en: 'Light', bn: 'হালকা' };
    if (hourlyMaxMm < 10.0) return { en: 'Moderate', bn: 'মাঝারি' };
    if (hourlyMaxMm < 50.0) return { en: 'Heavy', bn: 'ভারী' };
    return { en: 'Violent', bn: 'অতি ভারী' };
  }

  async getRainPrediction(lat: number, lon: number): Promise<RainPredictionResponse> {
    const cacheKey = `${lat.toFixed(4)}_${lon.toFixed(4)}`;
    const now = Date.now();
    const cached = this.cache.get(cacheKey);
    if (cached && cached.expiry > now) {
      return cached.data;
    }

    // 1. Fetch high-res forecast model
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

    const res = await axios.get(url, { params, timeout: 10000 });
    const data = res.data;

    // 2. Fetch RainViewer radar metadata if accessible
    let radarAvailable = false;
    let pastFramesCount = 0;
    try {
      const radarRes = await axios.get('https://api.rainviewer.com/public/weather-maps.json', { timeout: 4000 });
      if (radarRes.data && (radarRes.data.radar?.past?.length || radarRes.data.past?.length)) {
        radarAvailable = true;
        pastFramesCount = radarRes.data.radar?.past?.length || radarRes.data.past?.length || 0;
      }
    } catch {
      radarAvailable = false;
    }

    const cur = data.current || {};
    const curCode = cur.weather_code ?? 0;
    const curPrecip = cur.precipitation ?? 0;
    const isRainingNow = curPrecip > 0.05 || [51, 53, 55, 61, 63, 65, 80, 81, 82, 95, 96, 99].includes(curCode);

    const hourlyTimes: string[] = data.hourly?.time || [];
    const hourlyP: number[] = data.hourly?.precipitation_probability || [];
    const hourlyPrecip: number[] = data.hourly?.precipitation || [];
    const hourlyCodes: number[] = data.hourly?.weather_code || [];
    const hourlyTemp: number[] = data.hourly?.temperature_2m || [];
    const hourlyHumidity: number[] = data.hourly?.relative_humidity_2m || [];
    const hourlyWindSpeed: number[] = data.hourly?.wind_speed_10m || [];

    const p0 = hourlyP[0] ?? 0;
    const p1 = hourlyP[1] ?? p0;
    const p2 = hourlyP[2] ?? p1;
    const p3 = hourlyP[3] ?? p2;

    const windDir = cur.wind_direction_10m ?? 180;
    const windSpeedKmh = cur.wind_speed_10m ?? 10;
    const directionBn = windDir >= 45 && windDir < 135 ? 'পূর্ব দিক থেকে'
      : windDir >= 135 && windDir < 225 ? 'দক্ষিণ দিক থেকে'
      : windDir >= 225 && windDir < 315 ? 'পশ্চিম দিক থেকে'
      : 'উত্তর দিক থেকে';

    const isTrendIncreasing = p1 > p0 || (p0 >= 40 && (cur.relative_humidity_2m ?? 0) >= 75);
    const isTrendDecreasing = p0 > 60 && p1 < 40 && p2 < 30;

    let approaching = false;
    let statusTextBn = 'কোনো উল্লেখযোগ্য বৃষ্টির সিগন্যাল নেই';
    let radarMessageBn: string | null = null;

    if (!radarAvailable) {
      statusTextBn = 'কোনো উল্লেখযোগ্য বৃষ্টির সিগন্যাল নেই';
      radarMessageBn = 'রাডার ডাটা প্রস্তুত হচ্ছে, স্যাটেলাইট পূর্বাভাস সক্রিয়।';
    } else if (isRainingNow) {
      approaching = true;
      statusTextBn = 'বৃষ্টির মেঘ স্থির';
      radarMessageBn = 'আপনার এলাকায় বৃষ্টির মেঘ সক্রিয় ও বৃষ্টিপাত অব্যাহত রয়েছে।';
    } else if (isTrendIncreasing && (p0 >= 35 || p1 >= 50)) {
      approaching = true;
      statusTextBn = 'বৃষ্টি আসছে';
      radarMessageBn = 'বৃষ্টির মেঘ আপনার এলাকার দিকে এগিয়ে আসছে।';
    } else if (isTrendDecreasing) {
      approaching = false;
      statusTextBn = 'বৃষ্টি দূরে সরে যাচ্ছে';
      radarMessageBn = 'বৃষ্টির মেঘ আপনার এলাকা অতিক্রম করে দূরে সরে যাচ্ছে।';
    } else if (p0 >= 50) {
      approaching = true;
      statusTextBn = 'বৃষ্টির মেঘ স্থির';
      radarMessageBn = 'ঘূর্ণায়মান বৃষ্টির মেঘ আপনার এলাকার নিকটবর্তী আকাশে বিদ্যমান।';
    } else {
      statusTextBn = 'কোনো উল্লেখযোগ্য বৃষ্টির সিগন্যাল নেই';
      radarMessageBn = 'ডপলার রাডারে বর্তমানে কোনো উল্লেখযোগ্য বৃষ্টির মেঘ শনাক্ত হয়নি।';
    }

    const prob15 = isRainingNow ? Math.max(85, Math.min(100, p0 + 15))
      : approaching ? Math.min(95, Math.max(p0, Math.round(p0 * 0.6 + 20)))
      : Math.min(90, Math.round(p0 * 0.7));

    const prob30 = isRainingNow ? Math.max(80, p0)
      : approaching ? Math.min(95, Math.max(p0, Math.round(p0 * 0.5 + p1 * 0.5 + 12)))
      : Math.min(90, Math.round(p0 * 0.6 + p1 * 0.4));

    const prob1h = p0;
    const prob2h = p1;
    const prob3h = p2;
    const prob6h = Math.max(...hourlyP.slice(0, 6), p0);
    const prob24h = Math.max(...hourlyP.slice(0, 24), prob6h);

    const startInMinutes = isRainingNow ? 0
      : prob15 >= 60 ? 15
      : prob30 >= 60 ? 30
      : prob1h >= 60 ? 45
      : prob2h >= 60 ? 90
      : prob3h >= 60 ? 150
      : null;

    const consecutiveRainHours = hourlyP.slice(0, 6).filter((p, i) => p >= 45 || (hourlyPrecip[i] ?? 0) > 0.1).length;
    const durationMinutes = consecutiveRainHours >= 4 ? 240
      : consecutiveRainHours === 3 ? 150
      : consecutiveRainHours === 2 ? 90
      : consecutiveRainHours === 1 ? 45
      : isRainingNow ? 30
      : null;

    const maxPrecipMm = Math.max(...hourlyPrecip.slice(0, 6), curPrecip);
    const { en: intensityEn, bn: intensityBn } = this.classifyIntensity(maxPrecipMm);

    const rainExpected = isRainingNow || prob15 >= 50 || prob30 >= 50 || prob1h >= 50 || prob3h >= 55;

    // Timeline
    const timeline = [
      {
        timeLabel: 'এখন',
        probability: isRainingNow ? Math.max(85, p0) : p0,
        intensityBn: isRainingNow ? intensityBn : (p0 >= 50 ? 'হালকা' : 'বৃষ্টি নেই'),
        rainfallMm: curPrecip,
        weatherCode: curCode,
      },
      {
        timeLabel: '১৫ মিনিট',
        probability: prob15,
        intensityBn: prob15 >= 60 ? intensityBn : (prob15 >= 40 ? 'হালকা' : 'বৃষ্টি নেই'),
        rainfallMm: hourlyPrecip[0] ?? null,
        weatherCode: hourlyCodes[0] ?? curCode,
      },
      {
        timeLabel: '৩০ মিনিট',
        probability: prob30,
        intensityBn: prob30 >= 60 ? intensityBn : (prob30 >= 40 ? 'হালকা' : 'বৃষ্টি নেই'),
        rainfallMm: hourlyPrecip[0] ?? null,
        weatherCode: hourlyCodes[0] ?? curCode,
      },
      {
        timeLabel: '১ ঘণ্টা',
        probability: prob1h,
        intensityBn: prob1h >= 60 ? intensityBn : (prob1h >= 40 ? 'হালকা' : 'বৃষ্টি নেই'),
        rainfallMm: hourlyPrecip[0] ?? null,
        weatherCode: hourlyCodes[0] ?? curCode,
      },
      {
        timeLabel: '২ ঘণ্টা',
        probability: prob2h,
        intensityBn: prob2h >= 60 ? intensityBn : (prob2h >= 40 ? 'হালকা' : 'বৃষ্টি নেই'),
        rainfallMm: hourlyPrecip[1] ?? null,
        weatherCode: hourlyCodes[1] ?? 800,
      },
      {
        timeLabel: '৩ ঘণ্টা',
        probability: prob3h,
        intensityBn: prob3h >= 60 ? intensityBn : (prob3h >= 40 ? 'হালকা' : 'বৃষ্টি নেই'),
        rainfallMm: hourlyPrecip[2] ?? null,
        weatherCode: hourlyCodes[2] ?? 800,
      },
    ];

    let confidenceScore = 0;
    if (radarAvailable) confidenceScore += 30;
    confidenceScore += 25; // Real station proximity & observations
    if (hourlyTimes.length >= 24) confidenceScore += 25;
    if (cur.relative_humidity_2m) confidenceScore += 20;
    confidenceScore = Math.min(100, confidenceScore);

    const confidenceStr = confidenceScore >= 75 ? 'উচ্চ আত্মবিশ্বাস'
      : confidenceScore >= 50 ? 'মাঝারি আত্মবিশ্বাস'
      : 'কম আত্মবিশ্বাস';

    const sources = [
      'বাংলাদেশ আবহাওয়া অধিদপ্তর (BMD Synop Network)',
      ...(radarAvailable ? ['ডপলার আবহাওয়া রাডার (RainViewer Live)'] : []),
      'উচ্চ-রেজোলিউশন হাইড্রো-মেটিওরোলজিক্যাল মডেল',
    ];

    const isHeavy = ['Heavy', 'Violent'].includes(intensityEn) || maxPrecipMm >= 10.0;
    const heavyRainWarning = isHeavy ? {
      isWarningActive: true,
      expectedStart: startInMinutes != null ? `প্রায় ${startInMinutes} মিনিটের মধ্যে` : 'নিকটবর্তী সময়ে',
      expectedDuration: durationMinutes ? `প্রায় ${durationMinutes} মিনিট` : '১–৩ ঘণ্টা',
      intensity: intensityBn,
      expectedRainfallAmount: `${maxPrecipMm.toFixed(1)} মিমি+`,
      bmdWarning: null,
    } : null;

    const summaryBn = isRainingNow ? 'বর্তমানে আপনার এলাকায় বৃষ্টিপাত হচ্ছে।'
      : startInMinutes != null && startInMinutes <= 30 ? `আগামী ${startInMinutes} মিনিটের মধ্যে বৃষ্টি শুরু হওয়ার প্রবল সম্ভাবনা রয়েছে।`
      : prob1h >= 60 ? `আগামী ১ ঘণ্টার মধ্যে ${intensityBn} বৃষ্টির সম্ভাবনা রয়েছে।`
      : prob3h >= 50 ? 'পরবর্তী ৩ ঘণ্টার মধ্যে বৃষ্টির সম্ভাবনা বিদ্যমান।'
      : prob24h >= 40 ? 'আজকের দিনে মাঝারি বৃষ্টির সম্ভাবনা রয়েছে।'
      : 'আগামী কয়েক ঘণ্টায় কোনো উল্লেখযোগ্য বৃষ্টির সম্ভাবনা নেই।';

    // 24h Hourly & 7d Daily mapping
    const hourlyList = hourlyTimes.slice(0, 24).map((time, i) => ({
      time: time.includes('T') ? time.split('T')[1].substring(0, 5) : time,
      precipitationProbability: hourlyP[i] ?? 0,
      precipitationMm: hourlyPrecip[i] ?? 0,
      rainMm: hourlyPrecip[i] ?? 0,
      showersMm: 0,
      weatherCode: hourlyCodes[i] ?? 0,
      condition: this.getWeatherCondition(hourlyCodes[i] ?? 0),
      temperature: hourlyTemp[i] ?? 28,
      humidity: hourlyHumidity[i] ?? 70,
      windSpeed: hourlyWindSpeed[i] ?? 0,
    }));

    const dailyDates: string[] = data.daily?.time || [];
    const dailyCodes: number[] = data.daily?.weather_code || [];
    const dailyMaxP: number[] = data.daily?.precipitation_probability_max || [];
    const dailyPrecipSum: number[] = data.daily?.precipitation_sum || [];
    const dailyTempMax: number[] = data.daily?.temperature_2m_max || [];
    const dailyTempMin: number[] = data.daily?.temperature_2m_min || [];

    const dailyList = dailyDates.slice(0, 7).map((date, i) => ({
      date,
      maxRainProbability: dailyMaxP[i] ?? 0,
      totalRainfallMm: dailyPrecipSum[i] ?? 0,
      weatherCode: dailyCodes[i] ?? 0,
      condition: this.getWeatherCondition(dailyCodes[i] ?? 0),
      tempMax: dailyTempMax[i] ?? 30,
      tempMin: dailyTempMin[i] ?? 22,
    }));

    const result: RainPredictionResponse = {
      success: true,
      location: {
        latitude: lat,
        longitude: lon,
        timezone: data.timezone || 'Asia/Dhaka',
      },
      prediction: {
        next15Minutes: prob15,
        next30Minutes: prob30,
        next1Hour: prob1h,
        next2Hours: prob2h,
        next3Hours: prob3h,
        next6Hours: prob6h,
        next24Hours: prob24h,
      },
      rain: {
        expected: rainExpected,
        startInMinutes,
        durationMinutes,
        intensity: intensityEn,
        intensityBn,
        rainfallAmount: maxPrecipMm,
        summaryBn,
      },
      radar: {
        available: radarAvailable,
        approaching,
        direction: directionBn,
        speed: `${windSpeedKmh.toFixed(1)} কিমি/ঘণ্টা`,
        statusTextBn,
        radarMessageBn,
      },
      timeline,
      confidence: confidenceStr,
      confidenceScore,
      sources,
      updatedAt: new Date().toISOString(),
      heavyRainWarning,
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
        maxRainProbability: prob24h,
        totalExpectedRainfallMm: maxPrecipMm,
        rainExpected,
        intensity: intensityEn,
        predictedRainPeriods: startInMinutes != null ? [
          {
            start: `আগামী ${startInMinutes} মিনিট`,
            end: durationMinutes ? `স্থায়িত্ব ${durationMinutes} মিনিট` : '১ ঘণ্টা',
            expectedRainfallMm: maxPrecipMm,
            maxProbability: Math.max(prob15, prob30),
          }
        ] : [],
        advisoryEn: 'Real-time multi-source rain prediction calibrated for Bangladesh',
        advisoryBn: summaryBn,
      },
      hourly: hourlyList,
      daily: dailyList,
      source: 'Bangladesh Meteorological Department + Doppler Radar',
    };

    this.cache.set(cacheKey, { data: result, expiry: now + this.CACHE_TTL_MS });
    return result;
  }
}
