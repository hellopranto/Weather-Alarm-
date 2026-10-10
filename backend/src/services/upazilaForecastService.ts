import axios from 'axios';
import fs from 'fs';
import path from 'path';

export interface UpazilaRecord {
  name: string;
  pcode: string;
  pcode_raw: string;
  district: string;
  division: string;
  lat: number;
  lon: number;
  division_bn?: string;
  district_bn?: string;
}

export interface UpazilaStepPoint {
  step_start: string;
  step_end: string;
  val_min?: number;
  val_avg?: number;
  val_max?: number;
  val_avg_day?: number;
  val_avg_night?: number;
}

export interface UpazilaForecastPayload {
  upazila_name: string;
  district_name: string;
  division_name: string;
  ADM3_PCODE: number | string;
  forecast_data: Record<string, UpazilaStepPoint[]>;
}

export interface UpazilaForecastApiResponse {
  error: string | null;
  source: string;
  updated_at?: string;
  data: Record<string, UpazilaForecastPayload>;
}

export class UpazilaForecastService {
  private upazilas: UpazilaRecord[] = [];
  private cache = new Map<string, { timestamp: number; data: any }>();
  private readonly CACHE_TTL_MS = 10 * 60 * 1000; // 10 minutes cache
  private readonly BASE_URL = 'https://api.bdservers.site';

  constructor() {
    this.loadUpazilas();
  }

  private loadUpazilas() {
    try {
      const filePath = path.join(__dirname, '../data/upazilas_bbs.json');
      if (fs.existsSync(filePath)) {
        const raw = fs.readFileSync(filePath, 'utf-8');
        this.upazilas = JSON.parse(raw);
      }
    } catch (e) {
      console.error('Failed to load upazilas_bbs.json', e);
    }
  }

  public getAllUpazilas(): UpazilaRecord[] {
    return this.upazilas;
  }

  /**
   * Find nearest upazila by coordinates using Haversine formula
   */
  public findNearestUpazila(lat: number, lon: number): UpazilaRecord | null {
    if (this.upazilas.length === 0) return null;

    let minDistance = Infinity;
    let closest: UpazilaRecord | null = null;

    for (const u of this.upazilas) {
      const dist = this.haversineDistanceKm(lat, lon, u.lat, u.lon);
      if (dist < minDistance) {
        minDistance = dist;
        closest = u;
      }
    }

    return closest;
  }

  public findUpazilaByPcode(pcode: string): UpazilaRecord | null {
    const clean = pcode.replace('BD', '').trim();
    return this.upazilas.find(u => u.pcode === clean || u.pcode_raw === pcode) || null;
  }

  private haversineDistanceKm(lat1: number, lon1: number, lat2: number, lon2: number): number {
    const R = 6371;
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
      Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }

  /**
   * Fetch recent daily forecast
   */
  public async getRecentForecast(source: string, pcode: string, params: string[] = ['rf', 'temp', 'rh', 'windspd']) {
    const cleanPcode = pcode.replace('BD', '').trim();
    const cacheKey = `recent_${source}_${cleanPcode}_${params.sort().join(',')}`;
    const cached = this.getFromCache(cacheKey);
    if (cached) return cached;

    const queryParams = new URLSearchParams();
    queryParams.append('SOURCE', source);
    for (const p of params) {
      queryParams.append('PARAM', p);
    }
    queryParams.append('PCODE', cleanPcode);

    const url = `${this.BASE_URL}/upazila_forecast_recent?${queryParams.toString()}`;
    const response = await axios.get<UpazilaForecastApiResponse>(url, { timeout: 12000 });
    
    this.setCache(cacheKey, response.data);
    return response.data;
  }

  /**
   * Fetch 4-day time-step forecast (3-hour intervals)
   */
  public async getStepsForecast(source: string, pcode: string, params: string[] = ['rf', 'temp', 'rh', 'windspd']) {
    const cleanPcode = pcode.replace('BD', '').trim();
    const cacheKey = `steps_${source}_${cleanPcode}_${params.sort().join(',')}`;
    const cached = this.getFromCache(cacheKey);
    if (cached) return cached;

    const queryParams = new URLSearchParams();
    queryParams.append('SOURCE', source);
    for (const p of params) {
      queryParams.append('PARAM', p);
    }
    queryParams.append('PCODE', cleanPcode);

    const url = `${this.BASE_URL}/upazila_forecast_steps_recent?${queryParams.toString()}`;
    const response = await axios.get<UpazilaForecastApiResponse>(url, { timeout: 12000 });

    this.setCache(cacheKey, response.data);
    return response.data;
  }

  /**
   * Fetch forecast by generation date
   */
  public async getForecastByDate(source: string, pcode: string, fdate: string, params: string[] = ['rf', 'temp']) {
    const cleanPcode = pcode.replace('BD', '').trim();
    const cacheKey = `date_${source}_${cleanPcode}_${fdate}_${params.sort().join(',')}`;
    const cached = this.getFromCache(cacheKey);
    if (cached) return cached;

    const queryParams = new URLSearchParams();
    queryParams.append('SOURCE', source);
    queryParams.append('FDATE', fdate);
    for (const p of params) {
      queryParams.append('PARAM', p);
    }
    queryParams.append('PCODE', cleanPcode);

    const url = `${this.BASE_URL}/upazila_forecast_date?${queryParams.toString()}`;
    const response = await axios.get<UpazilaForecastApiResponse>(url, { timeout: 12000 });

    this.setCache(cacheKey, response.data);
    return response.data;
  }

  private getFromCache(key: string): any | null {
    const item = this.cache.get(key);
    if (!item) return null;
    if (Date.now() - item.timestamp > this.CACHE_TTL_MS) {
      this.cache.delete(key);
      return null;
    }
    return item.data;
  }

  private setCache(key: string, data: any) {
    this.cache.set(key, { timestamp: Date.now(), data });
  }
}
