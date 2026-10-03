# Weather Alert Bangladesh - Backend Proxy Server

A secure aggregation and proxy service for the Weather Alert Bangladesh native Android application.

## Architectural Objectives

1. **Security**: Private API credentials (`OPENWEATHER_API_KEY`, etc.) remain server-side and are never committed or decompiled from APK binaries.
2. **Data Normalization**: Integrates three distinct meteorological providers into a unified schema:
   - **OpenWeather**: Current observations, 3-hourly forecasts, feels-like, UV index, atmospheric parameters.
   - **Bangladesh Meteorological Department (BMD)**: National station network readings, 24h rainfall recordings, inland riverport signals, and maritime storm danger warnings.
   - **RainViewer**: Public Doppler radar maps, composite precipitation tile URLs, and time-lapse frames.
3. **Resilience & Caching**: Employs an in-memory TTL cache to reduce redundant API quota consumption and provides graceful degradation if any provider experiences outages.
4. **Vercel Serverless Compatible**: Fully prepared for serverless deployment on Vercel with zero cold-start bottlenecks.

---

## Local Development

### Prerequisites
- Node.js 18+ or 20+
- npm or yarn

### Installation & Run
```bash
cd backend
npm install
npm run dev
```

### Production Build
```bash
npm run build
npm start
```

---

## Vercel Deployment Instructions (ভার্সেল ডিপ্লয়মেন্ট নির্দেশিকা)

### ধাপ ১: গিটহাবে পুশ করুন (Push to GitHub)
নিশ্চিত করুন যে আপনার কোড গিটহাব রিপোজিটরিতে পুশ করা হয়েছে। (`.env` ফাইল কখনো গিটহাবে পুশ করবেন না, এটি `.gitignore`-এ অন্তর্ভুক্ত রয়েছে)।

### ধাপ ২: ভার্সেলে প্রজেক্ট ইম্পোর্ট করুন (Import to Vercel)
1. [vercel.com](https://vercel.com) এ লগইন করুন।
2. **"Add New..."** -> **"Project"** বাটনে ক্লিক করুন।
3. আপনার গিটহাব রিপোজিটরিটি নির্বাচন করুন (`Import`)।

### ধাপ ৩: প্রজেক্ট সেটিংস কনফিগার করুন (Configure Project Settings)
- **Framework Preset**: `Other`
- **Root Directory**: `backend` (খুব গুরুত্বপূর্ণ! "Edit" বাটনে ক্লিক করে `backend` ফোল্ডার নির্বাচন করুন)।
  *(যদি রুট থেকে ডিপ্লয় করতে চান, তাহলেও রুটের `vercel.json` দিয়ে ব্যাকএন্ড হ্যান্ডল হবে)।*
- **Build Command**: `npm run build`
- **Output Directory**: ডিফল্ট রাখুন।
- **Install Command**: `npm install`

### ধাপ ৪: Environment Variables যোগ করুন (Add Environment Variables)
Vercel ড্যাশবোর্ডে **Environment Variables** সেকশনে নিচের ভেরিয়েবলগুলো যোগ করুন:

| Variable Name | Value / Description |
|---|---|
| `OPENWEATHER_API_KEY` | আপনার OpenWeather API Key (https://openweathermap.org থেকে বিনামূল্যে নেওয়া) |
| `BMD_API_BASE_URL` | `https://live4.bmd.gov.bd` (ডিফল্ট BMD API লিংক) |
| `RAINVIEWER_API_BASE_URL` | `https://api.rainviewer.com/public/weather-maps.json` |

### ধাপ ৫: Deploy বাটনে ক্লিক করুন (Click Deploy)
- **"Deploy"** বাটনে ক্লিক করুন।
- ১-২ মিনিটের মধ্যে ভার্সেল সার্ভারলেস ফাংশন তৈরি করে লাইভ URL প্রদান করবে (যেমন: `https://weather-alert-bd.vercel.app`)।

### ধাপ ৬: ডিপ্লয়মেন্ট সফল হয়েছে কিনা যাচাই করুন (Verification)
ব্রাউজারে বা `curl` দিয়ে আপনার লাইভ URL পরীক্ষা করুন:
```bash
curl https://your-deployment-url.vercel.app/health
```
সফল হলে নিচের মতো JSON রেসপন্স দেখতে পাবেন:
```json
{
  "status": "ok",
  "service": "Weather Alert Bangladesh Backend Proxy",
  "environment": "vercel-serverless",
  "timestamp": "2026-10-03T..."
}
```

---

## API Endpoints

- `GET /health` : Service health status
- `GET /api/weather?lat=23.8103&lon=90.4125&name=Dhaka` : Full unified weather response
- `GET /api/weather/current?lat=23.8103&lon=90.4125` : Current conditions
- `GET /api/weather/hourly?lat=23.8103&lon=90.4125` : 24-48h hourly intervals
- `GET /api/weather/daily?lat=23.8103&lon=90.4125` : 7-day outlook
- `GET /api/weather/alerts?lat=23.8103&lon=90.4125` : Active meteorological warnings
- `GET /api/bmd/observations` : Official BMD ground station readings
- `GET /api/bmd/rainfall` : 24-hour recorded precipitation by station
- `GET /api/bmd/warnings` : Official cautionary & danger signals
- `GET /api/radar` : RainViewer radar map frames and tile URL templates
