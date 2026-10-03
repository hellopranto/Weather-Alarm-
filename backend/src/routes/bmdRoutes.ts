import { Router } from 'express';
import { getBmdObservations, getBmdRainfall, getBmdWarnings } from '../controllers/bmdController';

const router = Router();

router.get('/observations', getBmdObservations);
router.get('/rainfall', getBmdRainfall);
router.get('/warnings', getBmdWarnings);

export default router;
