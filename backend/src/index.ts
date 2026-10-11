import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import rainRoutes from './routes/rainRoutes';
import airQualityRoutes from './routes/airQualityRoutes';
import bmdRoutes from './routes/bmdRoutes';
import upazilaForecastRoutes from './routes/upazilaForecastRoutes';

dotenv.config();

const app = express();
const port = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

// Routes
app.use('/api/rain-prediction', rainRoutes);
app.use('/api/air-quality', airQualityRoutes);
app.use('/api/bmd', bmdRoutes);
app.use('/api/upazila-forecast', upazilaForecastRoutes);

// Health check endpoints (both /health and /api/health)
app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    service: 'weather-alert-bangladesh-backend',
    timestamp: new Date().toISOString()
  });
});

app.get('/api/health', (req, res) => {
  res.json({
    status: 'ok',
    service: 'weather-alert-bangladesh-backend',
    timestamp: new Date().toISOString()
  });
});

// Root welcome / discovery endpoint
app.get('/', (req, res) => {
  res.json({
    status: 'ok',
    name: 'Weather Alert Bangladesh Backend API',
    endpoints: {
      health: '/health',
      apiHealth: '/api/health',
      bmd: '/api/bmd?lat=23.8103&lon=90.4125',
      rainPrediction: '/api/rain-prediction?lat=23.8103&lon=90.4125',
      airQuality: '/api/air-quality?lat=23.8103&lon=90.4125',
      upazilaForecastRecent: '/api/upazila-forecast/recent?pcode=302602&source=BMDWRF',
      upazilaForecastSteps: '/api/upazila-forecast/steps?pcode=302602&source=BMDWRF',
      upazilasList: '/api/upazila-forecast/upazilas',
      nearestUpazila: '/api/upazila-forecast/nearest?lat=23.8103&lon=90.4125'
    }
  });
});

// Only listen locally or in container, NEVER on Vercel Serverless runtime
if (process.env.NODE_ENV !== 'test' && !process.env.VERCEL) {
  app.listen(port, () => {
    console.log(`Backend server listening on port ${port}`);
  });
}

export default app;
module.exports = app;
