import { Router, Request, Response } from 'express';
import { WeatherService } from '../services/weather.service';

const router = Router();
const weatherService = new WeatherService();

/**
 * GET /api/weather?lat={latitude}&lon={longitude}
 * Primary unified endpoint integrating BMD observations, nearest station resolution,
 * reverse geocoding, 10-day forecast, air quality, sun/moon, and warnings.
 */
router.get('/', async (req: Request, res: Response) => {
  try {
    const lat = req.query.lat !== undefined ? parseFloat(req.query.lat as string) : 23.8103;
    const lon = req.query.lon !== undefined ? parseFloat(req.query.lon as string) : 90.4125;

    if (isNaN(lat) || isNaN(lon)) {
      return res.status(400).json({
        success: false,
        error: 'Invalid coordinates provided. lat and lon must be numeric values.',
      });
    }

    const data = await weatherService.getUnifiedWeather(lat, lon);
    res.json(data);
  } catch (error: any) {
    console.error('Weather retrieval error:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to retrieve weather data',
      message: error?.message || 'Internal error',
    });
  }
});

router.get('/current', async (req: Request, res: Response) => {
  try {
    const lat = req.query.lat !== undefined ? parseFloat(req.query.lat as string) : 23.8103;
    const lon = req.query.lon !== undefined ? parseFloat(req.query.lon as string) : 90.4125;
    const data = await weatherService.getUnifiedWeather(lat, lon);
    res.json({
      success: true,
      location: data.location,
      station: data.station,
      current: data.current,
      updatedAt: data.updatedAt,
    });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error?.message });
  }
});

router.get('/hourly', async (req: Request, res: Response) => {
  try {
    const lat = req.query.lat !== undefined ? parseFloat(req.query.lat as string) : 23.8103;
    const lon = req.query.lon !== undefined ? parseFloat(req.query.lon as string) : 90.4125;
    const data = await weatherService.getUnifiedWeather(lat, lon);
    res.json({
      success: true,
      location: data.location,
      hourly: data.hourly,
      updatedAt: data.updatedAt,
    });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error?.message });
  }
});

router.get('/daily', async (req: Request, res: Response) => {
  try {
    const lat = req.query.lat !== undefined ? parseFloat(req.query.lat as string) : 23.8103;
    const lon = req.query.lon !== undefined ? parseFloat(req.query.lon as string) : 90.4125;
    const data = await weatherService.getUnifiedWeather(lat, lon);
    res.json({
      success: true,
      location: data.location,
      daily: data.daily,
      updatedAt: data.updatedAt,
    });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error?.message });
  }
});

router.get('/alerts', async (req: Request, res: Response) => {
  try {
    const lat = req.query.lat !== undefined ? parseFloat(req.query.lat as string) : 23.8103;
    const lon = req.query.lon !== undefined ? parseFloat(req.query.lon as string) : 90.4125;
    const data = await weatherService.getUnifiedWeather(lat, lon);
    res.json({
      success: true,
      location: data.location,
      warnings: data.warnings,
      updatedAt: data.updatedAt,
    });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error?.message });
  }
});

export default router;
