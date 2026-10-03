import { Router } from 'express';
import { getRadarMetadata } from '../controllers/radarController';

const router = Router();

router.get('/', getRadarMetadata);

export default router;
