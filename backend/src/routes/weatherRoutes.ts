import { Router } from 'express';
import {
  getWeather,
  getCurrentWeather,
  getHourlyForecast,
  getDailyForecast,
  getAlerts,
} from '../controllers/weatherController';

const router = Router();

router.get('/', getWeather);
router.get('/current', getCurrentWeather);
router.get('/hourly', getHourlyForecast);
router.get('/daily', getDailyForecast);
router.get('/alerts', getAlerts);

export default router;
