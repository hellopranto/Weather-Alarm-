import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import weatherRoutes from './routes/weatherRoutes';
import radarRoutes from './routes/radarRoutes';
import bmdRoutes from './routes/bmdRoutes';
import { errorHandler } from './middleware/errorHandler';

dotenv.config();

const app = express();
const port = process.env.PORT || 4000;

// CORS configuration to allow requests from Android clients, emulators, and web preview
app.use(
  cors({
    origin: '*',
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization', 'X-Requested-With', 'Accept'],
  })
);

app.use(express.json());

// Health check endpoint
app.get('/health', (_req, res) => {
  res.json({
    status: 'ok',
    service: 'Weather Alert Bangladesh Backend Proxy',
    environment: process.env.VERCEL ? 'vercel-serverless' : 'local-node',
    timestamp: new Date().toISOString(),
  });
});

// API Routes
app.use('/weather', weatherRoutes);
app.use('/api/weather', weatherRoutes);
app.use('/api/radar', radarRoutes);
app.use('/api/bmd', bmdRoutes);

// Error Handling Middleware
app.use(errorHandler);

// Start HTTP server only when run directly as main module and not in Vercel serverless environment
if (require.main === module && !process.env.VERCEL) {
  app.listen(port, () => {
    console.log(`Weather Alert Bangladesh Proxy Server listening on port ${port}`);
  });
}

export default app;
module.exports = app;
