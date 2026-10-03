import axios from 'axios';

export interface RadarFrame {
  time: number;
  path: string;
}

export interface RainViewerRadarResponse {
  version: string;
  generated: number;
  host: string;
  radar: {
    past: RadarFrame[];
    nowcast: RadarFrame[];
  };
  satellite?: {
    infrared?: RadarFrame[];
  };
}

export class RainViewerProvider {
  private apiUrl: string;

  constructor(apiUrl?: string) {
    this.apiUrl = apiUrl || process.env.RAINVIEWER_API_BASE_URL || 'https://api.rainviewer.com/public/weather-maps.json';
  }

  async getRadarData(): Promise<RainViewerRadarResponse> {
    const response = await axios.get<RainViewerRadarResponse>(this.apiUrl, {
      timeout: 8000,
    });
    return response.data;
  }
}
