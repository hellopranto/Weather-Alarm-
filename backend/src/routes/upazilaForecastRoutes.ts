import { Router, Request, Response } from 'express';
import { UpazilaForecastService } from '../services/upazilaForecastService';

const router = Router();
const forecastService = new UpazilaForecastService();

const VALID_SOURCES = ['BMDWRF', 'ECMWF', 'RIMESWRF'];
const VALID_PARAMS = [
  'rf', 'temp', 'rh', 'tempdew', 'smois',
  'windspd', 'winddir', 'cldcvr', 'windgust',
  'thi', 'tempbc', 'thi_broilers', 'thi_layers'
];

/**
 * GET /api/upazila-forecast/upazilas
 * Search or list upazilas
 */
router.get('/upazilas', (req: Request, res: Response) => {
  const q = (req.query.q as string || '').toLowerCase().trim();
  const all = forecastService.getAllUpazilas();
  if (!q) {
    return res.json({ success: true, count: all.length, upazilas: all.slice(0, 50) });
  }
  const filtered = all.filter(u =>
    u.name.toLowerCase().includes(q) ||
    u.district.toLowerCase().includes(q) ||
    u.division.toLowerCase().includes(q) ||
    u.pcode.includes(q) ||
    (u.district_bn && u.district_bn.includes(q)) ||
    (u.division_bn && u.division_bn.includes(q))
  );
  return res.json({ success: true, count: filtered.length, upazilas: filtered });
});

/**
 * GET /api/upazila-forecast/nearest
 * Find nearest upazila by lat & lon
 */
router.get('/nearest', (req: Request, res: Response) => {
  const lat = parseFloat(req.query.lat as string);
  const lon = parseFloat(req.query.lon as string);

  if (isNaN(lat) || isNaN(lon)) {
    return res.status(400).json({ success: false, error: 'Valid lat and lon query parameters are required' });
  }

  const nearest = forecastService.findNearestUpazila(lat, lon);
  if (!nearest) {
    return res.status(404).json({ success: false, error: 'No upazila found' });
  }

  return res.json({ success: true, upazila: nearest });
});

/**
 * GET /api/upazila-forecast/recent
 * Proxy to https://api.bdservers.site/upazila_forecast_recent
 */
router.get('/recent', async (req: Request, res: Response) => {
  try {
    let source = (req.query.SOURCE || req.query.source || 'BMDWRF') as string;
    source = source.toUpperCase();
    if (!VALID_SOURCES.includes(source)) {
      source = 'BMDWRF';
    }

    let pcode = (req.query.PCODE || req.query.pcode) as string;
    const lat = parseFloat(req.query.lat as string);
    const lon = parseFloat(req.query.lon as string);

    if (!pcode && !isNaN(lat) && !isNaN(lon)) {
      const nearest = forecastService.findNearestUpazila(lat, lon);
      if (nearest) {
        pcode = nearest.pcode;
      }
    }

    if (!pcode) {
      return res.status(400).json({ success: false, error: 'PCODE or lat/lon required' });
    }

    let params: string[] = [];
    if (req.query.PARAM) {
      if (Array.isArray(req.query.PARAM)) {
        params = (req.query.PARAM as string[]).map(p => p.trim());
      } else {
        params = (req.query.PARAM as string).split(',').map(p => p.trim());
      }
    }
    if (params.length === 0) {
      params = ['rf', 'temp', 'rh', 'windspd'];
    }

    // Filter valid params
    params = params.filter(p => VALID_PARAMS.includes(p));

    const result = await forecastService.getRecentForecast(source, pcode, params);
    return res.json(result);
  } catch (err: any) {
    console.error('Error fetching recent forecast:', err.message);
    return res.status(502).json({
      error: 'Upstream forecast API error',
      details: err.message
    });
  }
});

/**
 * GET /api/upazila-forecast/steps
 * Proxy to https://api.bdservers.site/upazila_forecast_steps_recent
 */
router.get('/steps', async (req: Request, res: Response) => {
  try {
    let source = (req.query.SOURCE || req.query.source || 'BMDWRF') as string;
    source = source.toUpperCase();
    if (!VALID_SOURCES.includes(source)) {
      source = 'BMDWRF';
    }

    let pcode = (req.query.PCODE || req.query.pcode) as string;
    const lat = parseFloat(req.query.lat as string);
    const lon = parseFloat(req.query.lon as string);

    if (!pcode && !isNaN(lat) && !isNaN(lon)) {
      const nearest = forecastService.findNearestUpazila(lat, lon);
      if (nearest) {
        pcode = nearest.pcode;
      }
    }

    if (!pcode) {
      return res.status(400).json({ success: false, error: 'PCODE or lat/lon required' });
    }

    let params: string[] = [];
    if (req.query.PARAM) {
      if (Array.isArray(req.query.PARAM)) {
        params = (req.query.PARAM as string[]).map(p => p.trim());
      } else {
        params = (req.query.PARAM as string).split(',').map(p => p.trim());
      }
    }
    if (params.length === 0) {
      params = ['rf', 'temp', 'rh', 'windspd'];
    }
    params = params.filter(p => VALID_PARAMS.includes(p));

    const result = await forecastService.getStepsForecast(source, pcode, params);
    return res.json(result);
  } catch (err: any) {
    console.error('Error fetching steps forecast:', err.message);
    return res.status(502).json({
      error: 'Upstream steps forecast API error',
      details: err.message
    });
  }
});

/**
 * GET /api/upazila-forecast/date
 * Proxy to https://api.bdservers.site/upazila_forecast_date
 */
router.get('/date', async (req: Request, res: Response) => {
  try {
    let source = (req.query.SOURCE || req.query.source || 'BMDWRF') as string;
    source = source.toUpperCase();
    if (!VALID_SOURCES.includes(source)) {
      source = 'BMDWRF';
    }

    const pcode = (req.query.PCODE || req.query.pcode) as string;
    const fdate = (req.query.FDATE || req.query.fdate || req.query.date) as string;

    if (!pcode || !fdate) {
      return res.status(400).json({ success: false, error: 'PCODE and FDATE (YYYYMMDD) required' });
    }

    let params: string[] = [];
    if (req.query.PARAM) {
      if (Array.isArray(req.query.PARAM)) {
        params = (req.query.PARAM as string[]).map(p => p.trim());
      } else {
        params = (req.query.PARAM as string).split(',').map(p => p.trim());
      }
    }
    if (params.length === 0) {
      params = ['rf', 'temp'];
    }
    params = params.filter(p => VALID_PARAMS.includes(p));

    const result = await forecastService.getForecastByDate(source, pcode, fdate, params);
    return res.json(result);
  } catch (err: any) {
    console.error('Error fetching date forecast:', err.message);
    return res.status(502).json({
      error: 'Upstream date forecast API error',
      details: err.message
    });
  }
});

export default router;
