import axios from 'axios';

export interface BmdStation {
  stationId: string;
  stationType: string;
  stationCode: string;
  longitude: number;
  latitude: number;
  stationName: string;
  active: boolean;
  distanceKm?: number;
}

export interface BmdObservation {
  temperature: number | null;
  humidity: number | null;
  pressure: number | null;
  windSpeed: number | null;
  windDirection: number | null;
  rainfall: number | null;
  weatherCondition: string | null;
  observationTime: string | null;
}

export interface BmdResponse {
  success: boolean;
  location: {
    latitude: number;
    longitude: number;
  };
  nearestStation: {
    stationId: string;
    stationCode: string;
    stationName: string;
    latitude: number;
    longitude: number;
    distanceKm: number;
  };
  observation: BmdObservation;
  source: string;
  updatedAt: string;
  isStale?: boolean;
  dataAgeMinutes?: number | null;
}

export class BmdService {
  private static cachedStations: BmdStation[] = [];
  private static stationsLastFetched: number = 0;
  private static readonly STATIONS_CACHE_TTL_MS = 60 * 60 * 1000; // 1 hour

  /**
   * Calculates distance in kilometers between two coordinates using the Haversine formula
   */
  public distanceKm(lat1: number, lon1: number, lat2: number, lon2: number): number {
    const R = 6371; // Earth radius in km
    const dLat = (lat2 - lat1) * (Math.PI / 180);
    const dLon = (lon2 - lon1) * (Math.PI / 180);

    const a =
      Math.sin(dLat / 2) ** 2 +
      Math.cos(lat1 * (Math.PI / 180)) *
        Math.cos(lat2 * (Math.PI / 180)) *
        Math.sin(dLon / 2) ** 2;

    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return Math.round(R * c * 10) / 10;
  }

  /**
   * Loads active BMD stations dynamically from the official BMD CSV
   */
  public async loadStations(): Promise<BmdStation[]> {
    const now = Date.now();
    if (BmdService.cachedStations.length > 0 && now - BmdService.stationsLastFetched < BmdService.STATIONS_CACHE_TTL_MS) {
      return BmdService.cachedStations;
    }

    try {
      const response = await axios.get<string>('https://mobile.bmd.gov.bd/bmdmobile/station_synop.csv', {
        timeout: 10000,
        headers: { 'User-Agent': 'WeatherAlertBangladesh/2.0' }
      });

      const lines = response.data.split('\n');
      const stations: BmdStation[] = [];

      for (let i = 1; i < lines.length; i++) {
        const line = lines[i].trim();
        if (!line) continue;

        // Parse CSV handling quoted fields
        const regex = /(".*?"|[^",\s]+)(?=\s*,|\s*$)/g;
        const matches = line.match(regex) || line.split(',');
        if (matches.length < 7) continue;

        const stationId = matches[0].replace(/"/g, '').trim();
        const stationType = matches[1].replace(/"/g, '').trim();
        const stationCode = matches[2].replace(/"/g, '').trim();
        const lon = parseFloat(matches[3].replace(/"/g, '').trim());
        const lat = parseFloat(matches[4].replace(/"/g, '').trim());
        const stationName = matches[5].replace(/"/g, '').trim();
        const activeStr = matches[6].replace(/"/g, '').toLowerCase().trim();

        if (isNaN(lat) || isNaN(lon)) continue;

        const active = activeStr === 'yes' || activeStr === 'true' || activeStr === '1';

        stations.push({
          stationId,
          stationType,
          stationCode,
          longitude: lon,
          latitude: lat,
          stationName,
          active
        });
      }

      if (stations.length > 0) {
        BmdService.cachedStations = stations;
        BmdService.stationsLastFetched = now;
        return stations;
      }
    } catch (error) {
      console.warn('Failed to fetch stations from BMD mobile CSV, attempting cached or fallback:', error);
    }

    return BmdService.cachedStations;
  }

  /**
   * Fetches latest BMD observations (from AWS data or synop source)
   */
  public async fetchAwsObservations(): Promise<Map<string, Partial<BmdObservation>>> {
    const obsMap = new Map<string, Partial<BmdObservation>>();
    try {
      const response = await axios.get<string>('https://mobile.bmd.gov.bd/bmdmobile/aws_data.php', {
        timeout: 8000,
        headers: { 'User-Agent': 'WeatherAlertBangladesh/2.0' }
      });

      const lines = response.data.split('\n');
      for (let i = 1; i < lines.length; i++) {
        const line = lines[i].trim();
        if (!line) continue;
        const parts = line.split(',');
        if (parts.length >= 11) {
          const stationId = parts[1]?.trim();
          const param = parts[2]?.trim().toLowerCase();
          const val = parseFloat(parts[4]?.trim());
          const dateStr = parts[10]?.trim();

          if (!stationId) continue;
          const current = obsMap.get(stationId) || { observationTime: dateStr };

          if (!isNaN(val)) {
            if (param.includes('temp')) current.temperature = val;
            else if (param.includes('hum')) current.humidity = Math.round(val);
            else if (param.includes('press')) current.pressure = Math.round(val * 10) / 10;
            else if (param.includes('wind_spd') || param.includes('wind speed')) current.windSpeed = Math.round(val * 3.6 * 10) / 10; // convert m/s to km/h if needed
            else if (param.includes('wind_dir') || param.includes('wind direction')) current.windDirection = Math.round(val);
            else if (param.includes('rain')) current.rainfall = val;
          }

          obsMap.set(stationId, current);
        }
      }
    } catch (e) {
      // AWS data is optional fallback
    }
    return obsMap;
  }

  /**
   * Finds the nearest active station and returns clean normalized BMD data
   */
  public async getNearestStationData(lat: number, lon: number): Promise<BmdResponse> {
    const allStations = await this.loadStations();
    const activeStations = allStations.filter(s => s.active);

    if (activeStations.length === 0) {
      throw new Error('No active BMD stations available');
    }

    // Sort stations by distance to user coordinates
    const sortedStations = activeStations.map(station => ({
      ...station,
      distanceKm: this.distanceKm(lat, lon, station.latitude, station.longitude)
    })).sort((a, b) => (a.distanceKm || 0) - (b.distanceKm || 0));

    const awsObservations = await this.fetchAwsObservations();

    // Iterate sorted stations for station fallback if nearest has invalid/empty data
    let selectedStation = sortedStations[0];
    let selectedObs: BmdObservation = {
      temperature: null,
      humidity: null,
      pressure: null,
      windSpeed: null,
      windDirection: null,
      rainfall: null,
      weatherCondition: null,
      observationTime: null
    };

    for (const station of sortedStations) {
      const awsObs = awsObservations.get(station.stationId) || awsObservations.get(station.stationCode);
      if (awsObs && (awsObs.temperature !== undefined || awsObs.humidity !== undefined)) {
        selectedStation = station;
        selectedObs = {
          temperature: awsObs.temperature ?? null,
          humidity: awsObs.humidity ?? null,
          pressure: awsObs.pressure ?? null,
          windSpeed: awsObs.windSpeed ?? null,
          windDirection: awsObs.windDirection ?? null,
          rainfall: awsObs.rainfall ?? null,
          weatherCondition: null,
          observationTime: awsObs.observationTime ?? new Date().toISOString()
        };
        break;
      }
    }

    const updatedAt = new Date().toISOString();
    let isStale = false;
    let dataAgeMinutes: number | null = null;

    if (selectedObs.observationTime) {
      const obsDate = new Date(selectedObs.observationTime);
      if (!isNaN(obsDate.getTime())) {
        dataAgeMinutes = Math.round((Date.now() - obsDate.getTime()) / (60 * 1000));
        if (dataAgeMinutes > 180) { // older than 3 hours
          isStale = true;
        }
      }
    }

    return {
      success: true,
      location: {
        latitude: lat,
        longitude: lon
      },
      nearestStation: {
        stationId: selectedStation.stationId,
        stationCode: selectedStation.stationCode,
        stationName: selectedStation.stationName,
        latitude: selectedStation.latitude,
        longitude: selectedStation.longitude,
        distanceKm: selectedStation.distanceKm ?? 0
      },
      observation: selectedObs,
      source: 'BMD',
      updatedAt,
      isStale,
      dataAgeMinutes
    };
  }
}
