import axios from 'axios';

export interface OpenWeatherCurrent {
  temp: number;
  feels_like: number;
  humidity: number;
  pressure: number;
  wind_speed: number;
  wind_deg: number;
  visibility: number;
  weather: Array<{ id: number; main: string; description: string; icon: string }>;
  sunrise: number;
  sunset: number;
  rain?: { '1h'?: number };
}

export interface OpenWeatherForecastItem {
  dt: number;
  main: {
    temp: number;
    feels_like: number;
    temp_min: number;
    temp_max: number;
    humidity: number;
    pressure: number;
  };
  weather: Array<{ id: number; main: string; description: string; icon: string }>;
  wind: { speed: number; deg: number };
  pop: number; // Probability of precipitation (0 to 1)
  rain?: { '3h'?: number };
  dt_txt: string;
}

export class OpenWeatherProvider {
  private apiKey: string;
  private baseUrl = 'https://api.openweathermap.org/data/2.5';

  constructor(apiKey?: string) {
    this.apiKey = apiKey || process.env.OPENWEATHER_API_KEY || '';
  }

  async getCurrentWeather(lat: number, lon: number): Promise<any> {
    if (!this.apiKey) {
      throw new Error('OPENWEATHER_API_KEY is not configured on server');
    }
    const response = await axios.get(`${this.baseUrl}/weather`, {
      params: {
        lat,
        lon,
        units: 'metric',
        appid: this.apiKey,
      },
      timeout: 10000,
    });
    return response.data;
  }

  async getForecast(lat: number, lon: number): Promise<any> {
    if (!this.apiKey) {
      throw new Error('OPENWEATHER_API_KEY is not configured on server');
    }
    const response = await axios.get(`${this.baseUrl}/forecast`, {
      params: {
        lat,
        lon,
        units: 'metric',
        appid: this.apiKey,
      },
      timeout: 10000,
    });
    return response.data;
  }
}
