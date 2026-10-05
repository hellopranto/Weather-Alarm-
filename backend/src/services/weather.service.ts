import { BmdStationService } from './bmdStation.service';
import { BmdService } from './bmd.service';
import { LocationService } from './location.service';
import { ForecastService } from './forecast.service';
import { RainViewerProvider } from '../providers/rainviewer/rainViewerProvider';
import {
  UnifiedWeatherResponse,
  CurrentWeather,
  RainPredictionInfo,
} from '../types/weather';
import { mapConditionToBengali } from '../utils/bengali';

export class WeatherService {
  private bmdStationService: BmdStationService;
  private bmdService: BmdService;
  private locationService: LocationService;
  private forecastService: ForecastService;
  private rainViewerProvider: RainViewerProvider;

  // In-memory cache for unified weather response
  private unifiedCache = new Map<string, { data: UnifiedWeatherResponse; timestamp: number }>();
  private readonly CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

  constructor() {
    this.bmdStationService = new BmdStationService();
    this.bmdService = new BmdService();
    this.locationService = new LocationService();
    this.forecastService = new ForecastService();
    this.rainViewerProvider = new RainViewerProvider();
  }

  async getUnifiedWeather(lat: number, lon: number): Promise<UnifiedWeatherResponse> {
    const cacheKey = `${lat.toFixed(2)}_${lon.toFixed(2)}`;
    const now = Date.now();
    const cached = this.unifiedCache.get(cacheKey);

    if (cached && now - cached.timestamp < this.CACHE_TTL_MS) {
      return cached.data;
    }

    // 1. Resolve user's actual administrative location and nearest BMD station in parallel
    const [locationInfo, nearestStation] = await Promise.all([
      this.locationService.getLocationInfo(lat, lon),
      this.bmdStationService.findNearestStation(lat, lon),
    ]);

    // 2. Concurrently fetch:
    //    - BMD latest observation for the nearest station
    //    - BMD active warnings
    //    - Real forecast & air quality data
    //    - RainViewer radar metadata
    const [bmdObsResult, warnings, forecastData, radarData] = await Promise.all([
      this.bmdService.getLatestObservation(nearestStation.code),
      this.bmdService.getActiveWarnings(),
      this.forecastService.getForecast(lat, lon),
      (async () => {
        try {
          const r = await this.rainViewerProvider.getRadarData();
          if (r?.radar?.past?.length > 0) {
            const latest = r.radar.past[r.radar.past.length - 1];
            return {
              available: true,
              host: r.host,
              latestPath: latest.path,
              latestTime: latest.time,
              pastFramesCount: r.radar.past.length,
            };
          }
        } catch {
          // Non-critical
        }
        return { available: false };
      })(),
    ]);

    // 3. Formulate current weather strictly from real measurements
    // BMD is our primary official observation authority in Bangladesh
    const bmdParsed = bmdObsResult.available && bmdObsResult.parsed ? bmdObsResult.parsed : null;
    const fallbackCurrent = forecastData.currentFallback;

    const temperature =
      bmdParsed?.temperatureC !== null && bmdParsed?.temperatureC !== undefined
        ? bmdParsed.temperatureC
        : (fallbackCurrent?.temperature ?? null);

    const feelsLike =
      bmdParsed?.feelsLikeC !== null && bmdParsed?.feelsLikeC !== undefined
        ? bmdParsed.feelsLikeC
        : (fallbackCurrent?.feelsLike ?? null);

    const humidity =
      bmdParsed?.humidityPercent !== null && bmdParsed?.humidityPercent !== undefined
        ? bmdParsed.humidityPercent
        : (fallbackCurrent?.humidity ?? null);

    const pressure =
      bmdParsed?.pressureHpa !== null && bmdParsed?.pressureHpa !== undefined
        ? bmdParsed.pressureHpa
        : (fallbackCurrent?.pressure ?? null);

    const windSpeed =
      bmdParsed?.windSpeedKmh !== null && bmdParsed?.windSpeedKmh !== undefined
        ? bmdParsed.windSpeedKmh
        : (fallbackCurrent?.windSpeed ?? null);

    const windDirection =
      bmdParsed?.windDirectionDegrees !== null && bmdParsed?.windDirectionDegrees !== undefined
        ? bmdParsed.windDirectionDegrees
        : (fallbackCurrent?.windDirection ?? null);

    const visibility =
      bmdParsed?.visibilityKm !== null && bmdParsed?.visibilityKm !== undefined
        ? bmdParsed.visibilityKm * 1000
        : (fallbackCurrent?.visibility ?? null);

    const rainfall =
      bmdParsed?.rainfallMm !== null && bmdParsed?.rainfallMm !== undefined
        ? bmdParsed.rainfallMm
        : (fallbackCurrent?.rainfall ?? null);

    const rawCondition = bmdParsed?.weatherCondition || fallbackCurrent?.condition || null;
    const weatherCode = bmdParsed?.weatherCode || fallbackCurrent?.weatherCode || 800;
    const conditionBn = mapConditionToBengali(rawCondition, weatherCode);

    const current: CurrentWeather = {
      temperature,
      feelsLike,
      tempMin: bmdParsed?.tempMinC ?? (forecastData.daily[0]?.tempMin ?? null),
      tempMax: bmdParsed?.tempMaxC ?? (forecastData.daily[0]?.tempMax ?? null),
      condition: rawCondition,
      conditionBn,
      description: rawCondition || conditionBn,
      humidity,
      pressure,
      windSpeed,
      windDirection,
      visibility,
      rainfall,
      uvIndex: fallbackCurrent?.uvIndex ?? null,
      weatherCode,
      icon: fallbackCurrent?.icon || '02d',
      recordedAt: bmdObsResult.dateTime || new Date().toISOString(),
    };

    // Rain prediction summary
    const nextRainHour = forecastData.hourly.find((h) => h.rainProbability >= 40);
    const rainPrediction: RainPredictionInfo = {
      rainProbability: forecastData.hourly[0]?.rainProbability ?? null,
      expectedNextHours: nextRainHour ? `সম্ভাব্য বৃষ্টিপাত ${nextRainHour.timeString} নাগাদ` : 'পরবর্তী কয়েক ঘণ্টায় বৃষ্টির সম্ভাবনা কম',
      summaryBn: rainfall && rainfall > 0 ? `আজ রেকর্ডকৃত বৃষ্টিপাত: ${rainfall} মিমি` : 'আজ কোনো উল্লেখযোগ্য বৃষ্টিপাত রেকর্ড করা হয়নি',
    };

    // Construct response matching Section 5
    const response: UnifiedWeatherResponse = {
      success: true,
      location: locationInfo,
      station: nearestStation,
      current,
      hourly: forecastData.hourly,
      daily: forecastData.daily,
      airQuality: forecastData.airQuality,
      sunMoon: forecastData.sunMoon,
      warnings,
      rainPrediction,
      updatedAt: new Date().toISOString(),

      // Backwards compatibility aliases
      alerts: warnings,
      bmd: {
        available: bmdObsResult.available,
        station: nearestStation.name,
        observation: bmdParsed
          ? {
              stationId: nearestStation.code,
              stationName: nearestStation.name,
              division: locationInfo.division,
              temperatureC: bmdParsed.temperatureC || 0,
              humidityPercent: bmdParsed.humidityPercent || 0,
              windSpeedKmh: bmdParsed.windSpeedKmh || 0,
              windDirectionDegrees: bmdParsed.windDirectionDegrees || 0,
              pressureHpa: bmdParsed.pressureHpa || 1010,
              rainfall24hMm: bmdParsed.rainfallMm || 0,
              recordedAt: bmdObsResult.dateTime || new Date().toISOString(),
            }
          : null,
      },
      radar: radarData,
    };

    this.unifiedCache.set(cacheKey, { data: response, timestamp: now });
    return response;
  }
}
