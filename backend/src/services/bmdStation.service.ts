import axios from 'axios';
import { BmdStation, NearestStation } from '../types/weather';
import { haversineDistanceKm } from '../utils/haversine';

export class BmdStationService {
  private static readonly CSV_URL = 'https://mobile.bmd.gov.bd/bmdmobile/station_synop.csv';
  private stationsCache: BmdStation[] = [];
  private lastFetchTime = 0;
  private readonly CACHE_TTL_MS = 24 * 60 * 60 * 1000; // 24 hours

  // Comprehensive fallback list of BMD SYNOP stations across Bangladesh in case remote CSV is unreachable
  private static readonly FALLBACK_SYNOP_STATIONS: BmdStation[] = [
    { stationId: '41858', stationType: 'SYNOP', stationCode: '41858', longitude: 88.91, latitude: 25.75, stationName: 'Saiddpur', active: true },
    { stationId: '41863', stationType: 'SYNOP', stationCode: '41863', longitude: 88.65, latitude: 25.65, stationName: 'Dinajpur', active: true },
    { stationId: '41859', stationType: 'SYNOP', stationCode: '41859', longitude: 89.23, latitude: 25.73, stationName: 'Rangpur', active: true },
    { stationId: '41851', stationType: 'SYNOP', stationCode: '41851', longitude: 88.35, latitude: 26.50, stationName: 'Tetulia', active: true },
    { stationId: '41860', stationType: 'SYNOP', stationCode: '41860', longitude: 89.65, latitude: 25.80, stationName: 'Rajarhat', active: true },
    { stationId: '41883', stationType: 'SYNOP', stationCode: '41883', longitude: 89.37, latitude: 24.85, stationName: 'Bogura', active: true },
    { stationId: '41895', stationType: 'SYNOP', stationCode: '41895', longitude: 88.60, latitude: 24.37, stationName: 'Rajshahi', active: true },
    { stationId: '41907', stationType: 'SYNOP', stationCode: '41907', longitude: 89.07, latitude: 24.15, stationName: 'Ishwardi', active: true },
    { stationId: '41886', stationType: 'SYNOP', stationCode: '41886', longitude: 90.43, latitude: 24.72, stationName: 'Mymensingh', active: true },
    { stationId: '41909', stationType: 'SYNOP', stationCode: '41909', longitude: 89.92, latitude: 24.25, stationName: 'Tangail', active: true },
    { stationId: '41923', stationType: 'SYNOP', stationCode: '41923', longitude: 90.38, latitude: 23.78, stationName: 'Dhaka', active: true },
    { stationId: '41929', stationType: 'SYNOP', stationCode: '41929', longitude: 89.84, latitude: 23.60, stationName: 'Faridpur', active: true },
    { stationId: '41939', stationType: 'SYNOP', stationCode: '41939', longitude: 90.18, latitude: 23.17, stationName: 'Madaripur', active: true },
    { stationId: '41915', stationType: 'SYNOP', stationCode: '41915', longitude: 91.73, latitude: 24.31, stationName: 'Srimangal', active: true },
    { stationId: '41891', stationType: 'SYNOP', stationCode: '41891', longitude: 91.87, latitude: 24.90, stationName: 'Sylhet', active: true },
    { stationId: '41926', stationType: 'SYNOP', stationCode: '41926', longitude: 88.85, latitude: 23.64, stationName: 'Chuadanga', active: true },
    { stationId: '41936', stationType: 'SYNOP', stationCode: '41936', longitude: 89.17, latitude: 23.18, stationName: 'Jashore', active: true },
    { stationId: '41947', stationType: 'SYNOP', stationCode: '41947', longitude: 89.53, latitude: 22.82, stationName: 'Khulna', active: true },
    { stationId: '41958', stationType: 'SYNOP', stationCode: '41958', longitude: 89.60, latitude: 22.48, stationName: 'Mongla', active: true },
    { stationId: '41946', stationType: 'SYNOP', stationCode: '41946', longitude: 89.08, latitude: 22.72, stationName: 'Satkhira', active: true },
    { stationId: '41950', stationType: 'SYNOP', stationCode: '41950', longitude: 90.37, latitude: 22.72, stationName: 'Barishal', active: true },
    { stationId: '41960', stationType: 'SYNOP', stationCode: '41960', longitude: 90.33, latitude: 22.33, stationName: 'Patuakhali', active: true },
    { stationId: '41984', stationType: 'SYNOP', stationCode: '41984', longitude: 90.23, latitude: 21.98, stationName: 'Khepupara', active: true },
    { stationId: '41951', stationType: 'SYNOP', stationCode: '41951', longitude: 90.65, latitude: 22.69, stationName: 'Bhola', active: true },
    { stationId: '41941', stationType: 'SYNOP', stationCode: '41941', longitude: 90.65, latitude: 23.23, stationName: 'Chandpur', active: true },
    { stationId: '41933', stationType: 'SYNOP', stationCode: '41933', longitude: 91.18, latitude: 23.43, stationName: 'Cumilla', active: true },
    { stationId: '41943', stationType: 'SYNOP', stationCode: '41943', longitude: 91.40, latitude: 23.03, stationName: 'Feni', active: true },
    { stationId: '41953', stationType: 'SYNOP', stationCode: '41953', longitude: 91.10, latitude: 22.87, stationName: 'Maijdee Court', active: true },
    { stationId: '41963', stationType: 'SYNOP', stationCode: '41963', longitude: 91.07, latitude: 22.45, stationName: 'Hatiya', active: true },
    { stationId: '41964', stationType: 'SYNOP', stationCode: '41964', longitude: 91.43, latitude: 22.48, stationName: 'Swandeep', active: true },
    { stationId: '41965', stationType: 'SYNOP', stationCode: '41965', longitude: 91.68, latitude: 22.63, stationName: 'SitaKundo', active: true },
    { stationId: '41977', stationType: 'SYNOP', stationCode: '41977', longitude: 91.82, latitude: 22.35, stationName: 'Chattogram Ambagan', active: true },
    { stationId: '41978', stationType: 'SYNOP', stationCode: '41978', longitude: 91.81, latitude: 22.25, stationName: 'Chattogram Patenga', active: true },
    { stationId: '41966', stationType: 'SYNOP', stationCode: '41966', longitude: 92.20, latitude: 22.63, stationName: 'Rangamati', active: true },
    { stationId: '41992', stationType: 'SYNOP', stationCode: '41992', longitude: 91.97, latitude: 21.43, stationName: 'Cox\'s Bazar', active: true },
    { stationId: '41989', stationType: 'SYNOP', stationCode: '41989', longitude: 91.85, latitude: 21.82, stationName: 'Kutubdia', active: true },
    { stationId: '41998', stationType: 'SYNOP', stationCode: '41998', longitude: 92.30, latitude: 20.87, stationName: 'Teknaf', active: true }
  ];

  /**
   * Fetches the dynamic BMD station CSV or returns cached/fallback stations.
   * Parses all active SYNOP stations.
   */
  async getActiveSynopStations(): Promise<BmdStation[]> {
    const now = Date.now();
    if (this.stationsCache.length > 0 && now - this.lastFetchTime < this.CACHE_TTL_MS) {
      return this.stationsCache;
    }

    try {
      const response = await axios.get(BmdStationService.CSV_URL, {
        timeout: 5000,
        headers: {
          'User-Agent': 'WeatherAlertBD/2.0 (Mobile Client)',
        },
      });

      if (response.data && typeof response.data === 'string') {
        const parsed = this.parseCsv(response.data);
        if (parsed.length > 0) {
          this.stationsCache = parsed;
          this.lastFetchTime = now;
          return this.stationsCache;
        }
      }
    } catch (err: any) {
      // Remote CSV failed or timed out; fall back to verified synop stations
    }

    // Use fallback list
    if (this.stationsCache.length === 0) {
      this.stationsCache = BmdStationService.FALLBACK_SYNOP_STATIONS;
    }
    return this.stationsCache;
  }

  /**
   * Finds the nearest active BMD SYNOP station to the user's coordinate using Haversine distance.
   */
  async findNearestStation(lat: number, lon: number): Promise<NearestStation> {
    const stations = await this.getActiveSynopStations();

    let closest: BmdStation = stations[0] || BmdStationService.FALLBACK_SYNOP_STATIONS[0];
    let minDistance = Number.MAX_VALUE;

    for (const station of stations) {
      const dist = haversineDistanceKm(lat, lon, station.latitude, station.longitude);
      if (dist < minDistance) {
        minDistance = dist;
        closest = station;
      }
    }

    return {
      code: closest.stationCode,
      name: closest.stationName,
      latitude: closest.latitude,
      longitude: closest.longitude,
      distanceKm: Math.round(minDistance * 10) / 10,
    };
  }

  /**
   * Dynamic CSV parser handling headers, quotation marks, and commas.
   */
  private parseCsv(csvText: string): BmdStation[] {
    const lines = csvText.split(/\r?\n/).map((l) => l.trim()).filter((l) => l.length > 0);
    if (lines.length < 2) return [];

    const header = lines[0].split(',').map((h) => h.replace(/^["']|["']$/g, '').trim().toLowerCase());
    const idIdx = header.findIndex((h) => h.includes('id'));
    const typeIdx = header.findIndex((h) => h.includes('type'));
    const codeIdx = header.findIndex((h) => h.includes('code'));
    const lonIdx = header.findIndex((h) => h.includes('lon'));
    const latIdx = header.findIndex((h) => h.includes('lat'));
    const nameIdx = header.findIndex((h) => h.includes('name'));
    const activeIdx = header.findIndex((h) => h.includes('active'));

    const stations: BmdStation[] = [];

    for (let i = 1; i < lines.length; i++) {
      const row = this.parseCsvLine(lines[i]);
      if (row.length < 5) continue;

      const stationType = typeIdx >= 0 && row[typeIdx] ? row[typeIdx].trim().toUpperCase() : 'SYNOP';
      const activeRaw = activeIdx >= 0 && row[activeIdx] ? row[activeIdx].trim() : '1';
      const isActive = activeRaw === '1' || activeRaw.toLowerCase() === 'true' || activeRaw.toLowerCase() === 'active';

      // Keep only active SYNOP stations
      if (!isActive) continue;
      if (stationType && !stationType.includes('SYNOP')) continue;

      const stationCode = codeIdx >= 0 && row[codeIdx] ? row[codeIdx].trim() : '';
      const stationId = idIdx >= 0 && row[idIdx] ? row[idIdx].trim() : stationCode;
      const stationName = nameIdx >= 0 && row[nameIdx] ? row[nameIdx].trim() : '';
      const latitude = latIdx >= 0 ? parseFloat(row[latIdx]) : NaN;
      const longitude = lonIdx >= 0 ? parseFloat(row[lonIdx]) : NaN;

      if (!isNaN(latitude) && !isNaN(longitude) && stationCode.length > 0) {
        stations.push({
          stationId: stationId || stationCode,
          stationType: 'SYNOP',
          stationCode,
          longitude,
          latitude,
          stationName: stationName || `Station ${stationCode}`,
          active: true,
        });
      }
    }

    return stations;
  }

  private parseCsvLine(line: string): string[] {
    const values: string[] = [];
    let current = '';
    let inQuotes = false;

    for (let i = 0; i < line.length; i++) {
      const char = line[i];
      if (char === '"' || char === "'") {
        inQuotes = !inQuotes;
      } else if (char === ',' && !inQuotes) {
        values.push(current.trim().replace(/^["']|["']$/g, ''));
        current = '';
      } else {
        current += char;
      }
    }
    values.push(current.trim().replace(/^["']|["']$/g, ''));
    return values;
  }
}
