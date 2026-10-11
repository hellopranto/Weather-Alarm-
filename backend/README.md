# Weather Alert Bangladesh Backend - Vercel Deployment Guide

This backend is built with Express + TypeScript and designed for seamless serverless deployment on Vercel.

## 1. Quick Deploy to Vercel

### Method A: Deploy from Vercel Web Dashboard (Recommended)
1. Push your repository to GitHub.
2. In [Vercel Dashboard](https://vercel.com/new), click **Add New...** -> **Project**.
3. Select your GitHub repository.
4. **CRITICAL SETTING**: In **Project Settings**:
   - Set **Root Directory** to: `backend`
   - Keep Framework Preset as: `Other`
   - Build Command: `npm run build` (or leave default)
   - Output Directory: leave default
5. Click **Deploy**.

### Method B: Deploy using Vercel CLI
```bash
cd backend
npm install
vercel
```
When prompted for settings, accept the defaults.

---

## 2. Common Vercel Deployment Errors & Solutions

### Error 1: `No package.json found in the root directory`
* **Cause**: Vercel tried to build the repository from the project root instead of the `backend` folder.
* **Solution**: In Vercel Project Settings -> **General** -> **Root Directory**, click **Edit** and set it to `backend`. Click Save and redeploy.

### Error 2: `Cannot find module '../data/upazilas_bbs.json'` or `ENOENT: no such file or directory`
* **Cause**: Vercel Serverless Functions do not include loose JSON files unless explicitly imported or bundled.
* **Solution**: Fixed! `upazilaForecastService.ts` now uses compile-time static bundling with multiple runtime fallback paths (`process.cwd()` and `__dirname`).

### Error 3: Function Timed Out or Server Hangs
* **Cause**: Calling `app.listen()` inside a Serverless Lambda environment prevents the Lambda container from completing or responding.
* **Solution**: Fixed! `src/index.ts` checks `!process.env.VERCEL` and only calls `app.listen()` in local development. For Vercel, `api/index.ts` exports the Express app directly as a Serverless handler.

### Error 4: `404 Not Found` on `/health` or `/api/...`
* **Cause**: Missing rewrites in `vercel.json`.
* **Solution**: Fixed! `backend/vercel.json` rewrites all traffic `/(.*)` to `/api/index`.

---

## 3. Verified Endpoints

- `GET /health` -> `{ status: "ok", service: "weather-alert-bangladesh-backend" }`
- `GET /api/health` -> `{ status: "ok", service: "weather-alert-bangladesh-backend" }`
- `GET /api/bmd?lat=23.8103&lon=90.4125` -> Nearest BMD observation & station telemetry
- `GET /api/rain-prediction?lat=23.8103&lon=90.4125` -> Multi-source rain prediction
- `GET /api/air-quality?lat=23.8103&lon=90.4125` -> Real-time AQI & health guidance
- `GET /api/upazila-forecast/recent?pcode=302602&source=BMDWRF` -> 10-day overview forecast
- `GET /api/upazila-forecast/steps?pcode=302602&source=BMDWRF` -> 4-day 3-hourly time-step forecast
- `GET /api/upazila-forecast/upazilas?q=dhaka` -> BBS upazila search with PCODEs
- `GET /api/upazila-forecast/nearest?lat=23.8103&lon=90.4125` -> Nearest upazila via Haversine
