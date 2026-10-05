export interface BmdStation {
  stationId: string;
  stationType: string;
  stationCode: string;
  longitude: number;
  latitude: number;
  stationName: string;
  active: boolean;
}

export interface NearestStation {
  code: string;
  name: string;
  latitude: number;
  longitude: number;
  distanceKm: number;
}

export interface LocationInfo {
  latitude: number;
  longitude: number;
  upazila: string;
  district: string;
  division: string;
  displayName: string;
}

export interface CurrentWeather {
  temperature: number | null;
  feelsLike: number | null;
  tempMin?: number | null;
  tempMax?: number | null;
  condition: string | null;
  conditionBn?: string | null;
  description?: string | null;
  humidity: number | null;
  pressure: number | null;
  windSpeed: number | null;
  windDirection: number | null;
  visibility: number | null;
  rainfall: number | null;
  uvIndex: number | null;
  weatherCode?: number;
  icon?: string;
  recordedAt?: string | null;
}

export interface HourlyForecastItem {
  timestamp: number;
  timeString: string;
  dateString?: string;
  temperature: number | null;
  feelsLike: number | null;
  humidity: number | null;
  pressure: number | null;
  condition: string;
  conditionBn?: string;
  description: string;
  icon: string;
  weatherCode: number;
  rainProbability: number;
  precipitationMm: number;
  windSpeed: number;
  windDirection: number;
}

export interface DailyForecastItem {
  date: string;
  dayName: string;
  dayNameBn?: string;
  dateFormattedBn?: string;
  tempMin: number | null;
  tempMax: number | null;
  condition: string;
  conditionBn?: string;
  icon: string;
  weatherCode: number;
  rainProbability: number;
  precipitationMm: number;
}

export interface AirQualityInfo {
  aqi: number | null;
  category: string | null;
  categoryBn?: string | null;
  pm25: number | null;
  pm10: number | null;
}

export interface SunMoonInfo {
  sunrise: string | null;
  sunset: string | null;
  moonrise: string | null;
  moonset: string | null;
  moonPhase: string | null;
  moonPhaseBn?: string | null;
}

export interface WeatherWarning {
  id: string;
  title: string;
  titleBn?: string;
  description: string;
  descriptionBn?: string;
  severity: 'INFO' | 'WARNING' | 'DANGER' | 'GREAT_DANGER';
  startTime: string;
  endTime: string;
  source: string;
  signalNumber?: number | null;
  regions: string[];
}

export interface RainPredictionInfo {
  expectedNextHours?: string | null;
  rainProbability?: number | null;
  summaryBn?: string | null;
}

export interface UnifiedWeatherResponse {
  success: boolean;
  location: LocationInfo;
  station: NearestStation | null;
  current: CurrentWeather;
  hourly: HourlyForecastItem[];
  daily: DailyForecastItem[];
  airQuality: AirQualityInfo;
  sunMoon: SunMoonInfo;
  warnings: WeatherWarning[];
  rainPrediction: RainPredictionInfo;
  updatedAt: string | null;

  // Backwards compatibility fields for existing mobile components
  alerts?: WeatherWarning[];
  bmd?: {
    available: boolean;
    station: string | null;
    observation: any;
  } | null;
  radar?: any;
}
