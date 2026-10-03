import { Request, Response } from 'express';
import { WeatherService } from '../services/weatherService';

const weatherService = new WeatherService();

export const getWeather = async (req: Request, res: Response) => {
  try {
    const lat = parseFloat(req.query.lat as string) || 23.8103; // Default Dhaka
    const lon = parseFloat(req.query.lon as string) || 90.4125;
    const name = (req.query.name as string) || undefined;

    const data = await weatherService.getUnifiedWeather(lat, lon, name);
    res.json(data);
  } catch (error: any) {
    res.status(500).json({
      error: 'Failed to retrieve weather data',
      message: error.message,
    });
  }
};

export const getCurrentWeather = async (req: Request, res: Response) => {
  try {
    const lat = parseFloat(req.query.lat as string) || 23.8103;
    const lon = parseFloat(req.query.lon as string) || 90.4125;
    const name = (req.query.name as string) || undefined;

    const full = await weatherService.getUnifiedWeather(lat, lon, name);
    res.json({
      location: full.location,
      current: full.current,
      updatedAt: full.updatedAt,
    });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
};

export const getHourlyForecast = async (req: Request, res: Response) => {
  try {
    const lat = parseFloat(req.query.lat as string) || 23.8103;
    const lon = parseFloat(req.query.lon as string) || 90.4125;
    const full = await weatherService.getUnifiedWeather(lat, lon);
    res.json({
      location: full.location,
      hourly: full.hourly,
      updatedAt: full.updatedAt,
    });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
};

export const getDailyForecast = async (req: Request, res: Response) => {
  try {
    const lat = parseFloat(req.query.lat as string) || 23.8103;
    const lon = parseFloat(req.query.lon as string) || 90.4125;
    const full = await weatherService.getUnifiedWeather(lat, lon);
    res.json({
      location: full.location,
      daily: full.daily,
      updatedAt: full.updatedAt,
    });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
};

export const getAlerts = async (req: Request, res: Response) => {
  try {
    const lat = parseFloat(req.query.lat as string) || 23.8103;
    const lon = parseFloat(req.query.lon as string) || 90.4125;
    const full = await weatherService.getUnifiedWeather(lat, lon);
    res.json({
      location: full.location,
      alerts: full.alerts,
      updatedAt: full.updatedAt,
    });
  } catch (error: any) {
    res.status(500).json({ error: error.message });
  }
};
