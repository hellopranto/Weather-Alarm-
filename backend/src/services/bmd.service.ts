import axios from 'axios';
import { SynopService, ParsedSynop } from './synop.service';
import { WeatherWarning } from '../types/weather';

export interface BmdRawRecord {
  id?: string;
  stCode: string;
  date_time?: string;
  rbody?: string;
  humidity?: string;
  timeUpdate?: string;
  status?: string;
}

export interface BmdObservationResult {
  available: boolean;
  stationCode: string;
  dateTime: string | null;
  parsed: ParsedSynop | null;
  rawRbody: string | null;
  fromCache?: boolean;
}

export class BmdService {
  private synopService: SynopService;
  private baseUrl: string;
  private apiKey: string;

  // In-memory cache for recent REAL observation data
  private observationCache = new Map<string, { data: BmdObservationResult; timestamp: number }>();
  private readonly CACHE_TTL_MS = 10 * 60 * 1000; // 10 minutes short cache

  constructor() {
    this.synopService = new SynopService();
    this.baseUrl = process.env.BMD_API_BASE_URL || 'https://server6.bmd.gov.bd';
    // Key must only exist in backend environment variables
    this.apiKey = process.env.BMD_API_KEY || process.env.BMD_KEY || '';
  }

  /**
   * Fetches the latest observation for a given BMD station code.
   * If BMD temporarily fails:
   * 1. Returns recent REAL cached BMD data if available.
   * 2. Otherwise returns null/unavailable state.
   * NEVER invents or generates fake weather values.
   */
  async getLatestObservation(stationCode: string): Promise<BmdObservationResult> {
    const cached = this.observationCache.get(stationCode);
    const now = Date.now();

    // If we have a fresh cached observation, return it
    if (cached && now - cached.timestamp < this.CACHE_TTL_MS) {
      return { ...cached.data, fromCache: true };
    }

    try {
      const url = `${this.baseUrl}/weather-condition/public.php`;
      const response = await axios.get(url, {
        params: {
          public: 'lastdata',
          key: this.apiKey,
          stCode: stationCode,
        },
        timeout: 5000,
        headers: {
          'User-Agent': 'WeatherAlertBD-Proxy/2.0',
        },
      });

      if (response.data) {
        let record: BmdRawRecord | null = null;
        if (Array.isArray(response.data) && response.data.length > 0) {
          record = response.data[0];
        } else if (response.data.stCode || response.data.rbody) {
          record = response.data;
        }

        if (record && (record.rbody || record.humidity || record.stCode)) {
          const parsed = this.synopService.parse(record.rbody || '', record.humidity);
          const result: BmdObservationResult = {
            available: true,
            stationCode,
            dateTime: record.date_time || record.timeUpdate || null,
            parsed,
            rawRbody: record.rbody || null,
            fromCache: false,
          };

          // Cache real observation
          this.observationCache.set(stationCode, { data: result, timestamp: now });
          return result;
        }
      }
    } catch (err: any) {
      // Remote call failed
    }

    // Fallback: If recent REAL cache exists (even slightly expired), serve it with fromCache = true
    if (cached) {
      return { ...cached.data, fromCache: true };
    }

    // No fake data: return unavailable state
    return {
      available: false,
      stationCode,
      dateTime: null,
      parsed: null,
      rawRbody: null,
      fromCache: false,
    };
  }

  /**
   * Fetches weather warnings from BMD or national advisory.
   * If none active or unavailable, returns an empty array.
   */
  async getActiveWarnings(): Promise<WeatherWarning[]> {
    try {
      // Check if BMD public warning endpoint exists
      const url = `${this.baseUrl}/weather-condition/public.php?public=warnings`;
      const response = await axios.get(url, { timeout: 3500 });
      if (response.data && Array.isArray(response.data.warnings)) {
        return response.data.warnings.map((w: any) => ({
          id: w.id || `WARN-${Date.now()}`,
          title: w.title || 'আবহাওয়া সতর্কবার্তা',
          titleBn: w.titleBn || w.title,
          description: w.description || '',
          descriptionBn: w.descriptionBn || w.description,
          severity: w.severity || 'WARNING',
          startTime: w.startTime || new Date().toISOString(),
          endTime: w.endTime || new Date(Date.now() + 86400000).toISOString(),
          source: 'Bangladesh Meteorological Department (BMD)',
          signalNumber: w.signalNumber || null,
          regions: Array.isArray(w.regions) ? w.regions : [],
        }));
      }
    } catch {
      // Warning API not reachable or returns 404
    }

    // Data-driven: No hardcoded fake warnings.
    return [];
  }
}
