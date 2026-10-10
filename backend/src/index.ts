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

app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', service: 'weather-alert-bangladesh-backend' });
});

if (process.env.NODE_ENV !== 'test') {
  app.listen(port, () => {
    console.log(`Backend server listening on port ${port}`);
  });
}

export default app;
