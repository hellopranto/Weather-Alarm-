import axios from 'axios';

export type UsAqiCategory =
  | 'Good'
  | 'Moderate'
  | 'Unhealthy for Sensitive Groups'
  | 'Unhealthy'
  | 'Very Unhealthy'
  | 'Hazardous';

export type EuropeanAqiCategory = 'Good' | 'Fair' | 'Moderate' | 'Poor' | 'Very Poor';

export interface AirQualityHealthGuidance {
  generalEn: string;
  generalBn: string;
  sensitiveGroupsEn: string;
  sensitiveGroupsBn: string;
  outdoorActivitiesEn: string;
  outdoorActivitiesBn: string;
  childrenAndElderlyEn: string;
  childrenAndElderlyBn: string;
}

export interface AirQualityResponse {
  location: {
    latitude: number;
    longitude: number;
    timezone: string;
  };
  current: {
    usAqi: number;
    usAqiCategory: UsAqiCategory;
    europeanAqi: number;
    europeanAqiCategory: EuropeanAqiCategory;
    pm2_5: number;
    pm10: number;
    nitrogenDioxide: number;
    ozone: number;
    sulphurDioxide: number;
    carbonMonoxide: number;
  };
  units: {
    aqi: string;
    pm2_5: string;
    pm10: string;
    nitrogenDioxide: string;
    ozone: string;
    sulphurDioxide: string;
    carbonMonoxide: string;
  };
  summary24h: {
    peakUsAqi: number;
    peakTime: string;
    peakCategory: UsAqiCategory;
    averagePm2_5: number;
  };
  healthGuidance: AirQualityHealthGuidance;
  hourly: Array<{
    time: string;
    usAqi: number;
    europeanAqi: number;
    pm2_5: number;
    pm10: number;
    nitrogenDioxide: number;
    ozone: number;
    sulphurDioxide: number;
    carbonMonoxide: number;
  }>;
  daily: Array<{
    date: string;
    maxUsAqi: number;
    maxEuropeanAqi: number;
    avgPm2_5: number;
    category: UsAqiCategory;
  }>;
  isModeled: boolean;
  source: string;
  updatedAt: string;
}

export class AirQualityService {
  private cache = new Map<string, { data: AirQualityResponse; expiry: number }>();
  private readonly CACHE_TTL_MS = 15 * 60 * 1000; // 15 minutes

  public static getUsAqiCategory(aqi: number): UsAqiCategory {
    if (aqi <= 50) return 'Good';
    if (aqi <= 100) return 'Moderate';
    if (aqi <= 150) return 'Unhealthy for Sensitive Groups';
    if (aqi <= 200) return 'Unhealthy';
    if (aqi <= 300) return 'Very Unhealthy';
    return 'Hazardous';
  }

  public static getEuropeanAqiCategory(aqi: number): EuropeanAqiCategory {
    if (aqi <= 20) return 'Good';
    if (aqi <= 40) return 'Fair';
    if (aqi <= 60) return 'Moderate';
    if (aqi <= 80) return 'Poor';
    return 'Very Poor';
  }

  private getHealthGuidance(category: UsAqiCategory): AirQualityHealthGuidance {
    switch (category) {
      case 'Good':
        return {
          generalEn: 'Air quality is satisfactory and poses little or no risk.',
          generalBn: 'বায়ুর মান চমৎকার ও স্বাস্থ্যকর। কোনো স্বাস্থ্যঝুঁকি নেই।',
          sensitiveGroupsEn: 'Enjoy outdoor activities normally.',
          sensitiveGroupsBn: 'সংবেদনশীল ব্যক্তিরাও স্বাভাবিকভাবে বাইরে চলাফেরা করতে পারেন।',
          outdoorActivitiesEn: 'Ideal conditions for outdoor exercise, walking, and sports.',
          outdoorActivitiesBn: 'বাইরে শরীরচর্চা, হাঁটাচলা ও খেলাধুলার জন্য উপযুক্ত পরিবেশ।',
          childrenAndElderlyEn: 'Safe for children and senior citizens.',
          childrenAndElderlyBn: 'শিশু ও প্রবীণদের জন্য সম্পূর্ণ নিরাপদ।',
        };
      case 'Moderate':
        return {
          generalEn: 'Air quality is acceptable. Very sensitive individuals may experience minor symptoms.',
          generalBn: 'বায়ুর মান গ্রহণযোগ্য। অতি সংবেদনশীল ব্যক্তিদের ক্ষেত্রে সামান্য অস্বস্তি হতে পারে।',
          sensitiveGroupsEn: 'People with asthma or respiratory conditions should monitor symptoms.',
          sensitiveGroupsBn: 'হাঁপানি বা শ্বাসকষ্টে ভোগা ব্যক্তিদের অতিরিক্ত পরিশ্রমের সময় সতর্ক থাকা উচিত।',
          outdoorActivitiesEn: 'Outdoor activities are generally fine for the public.',
          outdoorActivitiesBn: 'সাধারণ মানুষের ক্ষেত্রে বাইরে স্বাভাবিক কাজকর্ম বা চলাচলে কোনো বাধা নেই।',
          childrenAndElderlyEn: 'Children and seniors can enjoy normal outdoor time.',
          childrenAndElderlyBn: 'শিশু ও বয়স্কদের জন্য পরিবেশ মোটামুটি স্বাভাবিক।',
        };
      case 'Unhealthy for Sensitive Groups':
        return {
          generalEn: 'Members of sensitive groups may experience health effects. General public is less likely to be affected.',
          generalBn: 'সংবেদনশীল ব্যক্তিদের স্বাস্থ্যঝুঁকি রয়েছে। সাধারণ মানুষের ক্ষেত্রে ক্ষতির সম্ভাবনা কিছুটা কম।',
          sensitiveGroupsEn: 'People with heart/lung disease, older adults, and children should reduce heavy outdoor exertion.',
          sensitiveGroupsBn: 'হাঁপানি, ফুসফুস বা হৃদরোগী, বয়স্ক ও শিশুদের দীর্ঘক্ষণ বাইরে ভারী পরিশ্রম কমানো উচিত।',
          outdoorActivitiesEn: 'Take breaks during prolonged outdoor exertion.',
          outdoorActivitiesBn: 'বাইরে দীর্ঘক্ষণ ব্যায়াম বা ভারী কাজের ক্ষেত্রে মাঝে মাঝে বিরতি নিন।',
          childrenAndElderlyEn: 'Children should limit prolonged outdoor playtime.',
          childrenAndElderlyBn: 'শিশুদের দীর্ঘক্ষণ খোলা মাঠে খেলাধুলা করা থেকে বিরত রাখুন।',
        };
      case 'Unhealthy':
        return {
          generalEn: 'Everyone may begin to experience health effects; sensitive groups may experience more serious health effects.',
          generalBn: 'সকলের জন্যই বায়ুর মান ক্ষতিকর। সংবেদনশীল ব্যক্তিরা মারাত্মক সমস্যায় পড়তে পারেন।',
          sensitiveGroupsEn: 'Avoid prolonged outdoor exertion. Wear an N95/protective mask outdoors.',
          sensitiveGroupsBn: 'বাইরে যাওয়া এড়িয়ে চলুন। বাইরে যেতে হলে অবশ্যই N95 বা ভালো মানের মাস্ক পরিধান করুন।',
          outdoorActivitiesEn: 'Relocate intense workouts indoors. Keep doors and windows closed.',
          outdoorActivitiesBn: 'বাইরে ব্যায়াম বা কায়িক পরিশ্রম না করে ঘরের ভেতরে থাকুন। জানালা বন্ধ রাখুন।',
          childrenAndElderlyEn: 'Children and older adults should stay indoors as much as possible.',
          childrenAndElderlyBn: 'শিশু ও বয়স্কদের যতটা সম্ভব ঘরের ভেতরে রাখা উচিত।',
        };
      case 'Very Unhealthy':
        return {
          generalEn: 'Health alert: The risk of health effects is increased for everyone.',
          generalBn: 'জরুরি স্বাস্থ্য সতর্কতা: সকলের জন্যই মারাত্মক স্বাস্থ্যঝুঁকির আশঙ্কা রয়েছে।',
          sensitiveGroupsEn: 'Remain indoors and keep activity levels low. Use air purifiers if available.',
          sensitiveGroupsBn: 'ঘরের ভেতরে অবস্থান করুন এবং শারীরিক পরিশ্রম সীমিত রাখুন। এয়ার পিউরিফায়ার ব্যবহার করুন।',
          outdoorActivitiesEn: 'Avoid all outdoor physical activity. Wear high-filtration masks if venturing out.',
          outdoorActivitiesBn: 'বাইরে যেকোনো ধরনের শারীরিক কর্মকাণ্ড বা ভ্রমণ সম্পূর্ণ পরিহার করুন।',
          childrenAndElderlyEn: 'Keep children, elderly, and vulnerable individuals strictly indoors.',
          childrenAndElderlyBn: 'শিশু, প্রবীণ ও অসুস্থ ব্যক্তিদের সম্পূর্ণরূপে ঘরের ভেতরে নিরাপদে রাখুন।',
        };
      case 'Hazardous':
      default:
        return {
          generalEn: 'Health warning of emergency conditions: Everyone is likely to be affected.',
          generalBn: 'চরম বিপজ্জনক অবস্থা: বায়ুর বিষাক্ততায় প্রত্যেকে মারাত্মক স্বাস্থ্যঝুঁকিতে পড়বেন।',
          sensitiveGroupsEn: 'Stay indoors with windows tightly shut. Seek medical attention if experiencing breathing distress.',
          sensitiveGroupsBn: 'ঘরের ভেতরে জানালা বন্ধ করে থাকুন। শ্বাসকষ্ট দেখা দিলে দ্রুত চিকিৎসকের পরামর্শ নিন।',
          outdoorActivitiesEn: 'Prohibit all outdoor exertion. Emergency pollution levels.',
          outdoorActivitiesBn: 'বাইরে বের হওয়া সম্পূর্ণরূপে নিষিদ্ধ। ঘরের বাইরে কোনো ধরনের কার্যক্রম চালাবেন না।',
          childrenAndElderlyEn: 'Emergency protection required for children, elderly, and medical patients.',
          childrenAndElderlyBn: 'শিশু ও বয়স্কদের জন্য জরুরি স্বাস্থ্য সতর্কতা অবলম্বন করুন।',
        };
    }
  }

  async getAirQuality(lat: number, lon: number): Promise<AirQualityResponse> {
    const cacheKey = `${lat.toFixed(4)}_${lon.toFixed(4)}`;
    const now = Date.now();
    const cached = this.cache.get(cacheKey);
    if (cached && cached.expiry > now) {
      return cached.data;
    }

    const url = 'https://air-quality-api.open-meteo.com/v1/air-quality';
    const params = {
      latitude: lat,
      longitude: lon,
      current: [
        'us_aqi',
        'european_aqi',
        'pm2_5',
        'pm10',
        'nitrogen_dioxide',
        'ozone',
        'sulphur_dioxide',
        'carbon_monoxide',
      ].join(','),
      hourly: [
        'us_aqi',
        'european_aqi',
        'pm2_5',
        'pm10',
        'nitrogen_dioxide',
        'ozone',
        'sulphur_dioxide',
        'carbon_monoxide',
      ].join(','),
      timezone: 'auto',
    };

    const response = await axios.get(url, { params, timeout: 5000 });
    const data = response.data;

    const cur = data.current || {};
    const curUsAqi = Math.round(cur.us_aqi ?? 50);
    const curEuAqi = Math.round(cur.european_aqi ?? 25);
    const usAqiCat = AirQualityService.getUsAqiCategory(curUsAqi);
    const euAqiCat = AirQualityService.getEuropeanAqiCategory(curEuAqi);

    const hourlyTimes: string[] = data.hourly?.time || [];
    const hourlyUsAqi: number[] = data.hourly?.us_aqi || [];
    const hourlyEuAqi: number[] = data.hourly?.european_aqi || [];
    const hourlyPm25: number[] = data.hourly?.pm2_5 || [];
    const hourlyPm10: number[] = data.hourly?.pm10 || [];
    const hourlyNo2: number[] = data.hourly?.nitrogen_dioxide || [];
    const hourlyO3: number[] = data.hourly?.ozone || [];
    const hourlySo2: number[] = data.hourly?.sulphur_dioxide || [];
    const hourlyCo: number[] = data.hourly?.carbon_monoxide || [];

    const hourlyList: AirQualityResponse['hourly'] = [];
    const limit = Math.min(hourlyTimes.length, 48);

    let peakUsAqi = 0;
    let peakTime = hourlyTimes[0] || new Date().toISOString();
    let sumPm25_24h = 0;
    const count24h = Math.min(limit, 24);

    for (let i = 0; i < limit; i++) {
      const uAqi = Math.round(hourlyUsAqi[i] ?? curUsAqi);
      const eAqi = Math.round(hourlyEuAqi[i] ?? curEuAqi);
      const pm25 = Math.round((hourlyPm25[i] ?? 0) * 10) / 10;
      const pm10 = Math.round((hourlyPm10[i] ?? 0) * 10) / 10;
      const no2 = Math.round((hourlyNo2[i] ?? 0) * 10) / 10;
      const o3 = Math.round((hourlyO3[i] ?? 0) * 10) / 10;
      const so2 = Math.round((hourlySo2[i] ?? 0) * 10) / 10;
      const co = Math.round((hourlyCo[i] ?? 0) * 10) / 10;

      if (i < 24) {
        if (uAqi > peakUsAqi) {
          peakUsAqi = uAqi;
          peakTime = hourlyTimes[i] || peakTime;
        }
        sumPm25_24h += pm25;
      }

      hourlyList.push({
        time: hourlyTimes[i],
        usAqi: uAqi,
        europeanAqi: eAqi,
        pm2_5: pm25,
        pm10: pm10,
        nitrogenDioxide: no2,
        ozone: o3,
        sulphurDioxide: so2,
        carbonMonoxide: co,
      });
    }

    const avgPm25 = count24h > 0 ? Math.round((sumPm25_24h / count24h) * 10) / 10 : 25;

    // Aggregate Daily forecasts from hourly data (7 days)
    const dailyMap = new Map<string, { maxUsAqi: number; maxEuAqi: number; pm25Sum: number; count: number }>();
    hourlyTimes.forEach((t, idx) => {
      const dateKey = t.split('T')[0];
      if (!dailyMap.has(dateKey)) {
        dailyMap.set(dateKey, {
          maxUsAqi: hourlyUsAqi[idx] ?? 0,
          maxEuAqi: hourlyEuAqi[idx] ?? 0,
          pm25Sum: hourlyPm25[idx] ?? 0,
          count: 1,
        });
      } else {
        const item = dailyMap.get(dateKey)!;
        item.maxUsAqi = Math.max(item.maxUsAqi, hourlyUsAqi[idx] ?? 0);
        item.maxEuAqi = Math.max(item.maxEuAqi, hourlyEuAqi[idx] ?? 0);
        item.pm25Sum += hourlyPm25[idx] ?? 0;
        item.count += 1;
      }
    });

    const dailyList: AirQualityResponse['daily'] = [];
    const entries = Array.from(dailyMap.entries()).slice(0, 7);
    entries.forEach(([date, val]) => {
      const maxUs = Math.round(val.maxUsAqi);
      dailyList.push({
        date,
        maxUsAqi: maxUs,
        maxEuropeanAqi: Math.round(val.maxEuAqi),
        avgPm2_5: Math.round((val.pm25Sum / (val.count || 1)) * 10) / 10,
        category: AirQualityService.getUsAqiCategory(maxUs),
      });
    });

    const result: AirQualityResponse = {
      location: {
        latitude: lat,
        longitude: lon,
        timezone: data.timezone || 'Asia/Dhaka',
      },
      current: {
        usAqi: curUsAqi,
        usAqiCategory: usAqiCat,
        europeanAqi: curEuAqi,
        europeanAqiCategory: euAqiCat,
        pm2_5: Math.round((cur.pm2_5 ?? 25) * 10) / 10,
        pm10: Math.round((cur.pm10 ?? 45) * 10) / 10,
        nitrogenDioxide: Math.round((cur.nitrogen_dioxide ?? 15) * 10) / 10,
        ozone: Math.round((cur.ozone ?? 30) * 10) / 10,
        sulphurDioxide: Math.round((cur.sulphur_dioxide ?? 8) * 10) / 10,
        carbonMonoxide: Math.round((cur.carbon_monoxide ?? 250) * 10) / 10,
      },
      units: {
        aqi: 'index',
        pm2_5: 'µg/m³',
        pm10: 'µg/m³',
        nitrogenDioxide: 'µg/m³',
        ozone: 'µg/m³',
        sulphurDioxide: 'µg/m³',
        carbonMonoxide: 'µg/m³',
      },
      summary24h: {
        peakUsAqi: peakUsAqi || curUsAqi,
        peakTime,
        peakCategory: AirQualityService.getUsAqiCategory(peakUsAqi || curUsAqi),
        averagePm2_5: avgPm25,
      },
      healthGuidance: this.getHealthGuidance(usAqiCat),
      hourly: hourlyList,
      daily: dailyList,
      isModeled: true,
      source: 'Open-Meteo Copernicus Atmosphere Monitoring Service (CAMS / SILAM)',
      updatedAt: new Date().toISOString(),
    };

    this.cache.set(cacheKey, { data: result, expiry: now + this.CACHE_TTL_MS });
    return result;
  }
}
