import { Request, Response } from 'express';
import { BmdProvider } from '../providers/bmd/bmdProvider';

const bmdProvider = new BmdProvider();

export const getBmdObservations = async (req: Request, res: Response) => {
  try {
    const lat = req.query.lat ? parseFloat(req.query.lat as string) : undefined;
    const lon = req.query.lon ? parseFloat(req.query.lon as string) : undefined;
    const stationQuery = (req.query.station as string) || undefined;
    const result = await bmdProvider.getStationObservations(lat, lon, stationQuery);
    res.json(result);
  } catch (error: any) {
    res.status(500).json({
      error: 'BMD observations unavailable',
      message: error.message,
    });
  }
};

export const getBmdRainfall = async (req: Request, res: Response) => {
  try {
    const result = await bmdProvider.getStationObservations();
    const rainfallData = result.stations.map(s => ({
      stationId: s.stationId,
      stationName: s.stationName,
      division: s.division,
      rainfall24hMm: s.rainfall24hMm,
      recordedAt: s.recordedAt,
    }));
    res.json({
      available: true,
      unit: 'mm',
      data: rainfallData,
      source: result.source,
    });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
};

export const getBmdWarnings = async (_req: Request, res: Response) => {
  try {
    const warnings = await bmdProvider.getWarnings();
    res.json({
      available: true,
      warnings,
      updatedAt: new Date().toISOString(),
    });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
};
