import { Router, Request, Response } from 'express';
import { RainService } from '../services/rainService';

const router = Router();
const rainService = new RainService();

router.get('/', async (req: Request, res: Response) => {
  try {
    const latStr = req.query.lat as string;
    const lonStr = req.query.lon as string;

    const lat = parseFloat(latStr || '23.8103');
    const lon = parseFloat(lonStr || '90.4125');

    if (isNaN(lat) || isNaN(lon) || lat < -90 || lat > 90 || lon < -180 || lon > 180) {
      return res.status(400).json({
        error: 'Invalid coordinates',
        message: 'Latitude must be between -90 and 90, longitude between -180 and 180.',
      });
    }

    const data = await rainService.getRainPrediction(lat, lon);
    return res.json(data);
  } catch (error: any) {
    return res.status(500).json({
      error: 'Failed to retrieve rain prediction',
      message: error.message || 'Internal upstream service error',
    });
  }
});

export default router;
