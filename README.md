# Weather Alert Bangladesh (ওয়েদার অ্যালার্ট বাংলাদেশ)

A native Android application built with Kotlin, Jetpack Compose, Material Design 3, and modern Android Jetpack architecture (MVVM, Room local caching, StateFlow, Coroutines).

Engineered specifically for Bangladesh's unique meteorological patterns—monsoon deltas, coastal tropical cyclones, inland riverport storm warning signals, and Kalbaishakhi (কালবৈশাখী / Nor'westers).

---

## Key Highlights

- **100% Native Android**: Built purely in Kotlin and Jetpack Compose without WebViews or cross-platform wrappers.
- **Tri-Provider Aggregation Architecture**:
  1. **OpenWeather**: High-accuracy current observations, hourly forecasts, 7-day outlooks, atmospheric pressure, and humidity.
  2. **Bangladesh Meteorological Department (BMD)**: Ground synoptic station observations (temperature, humidity, 24h rainfall recordings) and official cautionary/danger storm warning signals (Signal No. 1 to 10).
  3. **RainViewer**: Real-time Doppler precipitation radar overlays with past frame timeline animation, zoom, pan, and rain intensity legend.
- **Secure Backend / Proxy Layer**: All private API credentials remain protected in the Node.js/TypeScript backend proxy (`backend/`) or injected via build environment variables (`BuildConfig`), preventing credential leakage in public binaries.
- **Offline First**: Room database cache (`WeatherCacheEntity`) ensures full dashboard accessibility when roaming in remote delta areas without network connectivity.
- **Bilingual**: First-class support for English and Bengali (বাংলা) with dynamic runtime switching.
- **Customizable Units**: Metric (°C, km/h, hPa) and Imperial (°F, mph), with Theme Appearance controls (System, Light, Dark).

---

## App Screens & Navigation

1. **Home**: Modern weather dashboard featuring current condition, large temperature display, feels-like indicator, 24h hourly forecast row, official BMD station observation readings, full metrics grid, and active alert notifications.
2. **Hourly**: Comprehensive forecast view with interactive toggle between 24-hour timeline and 7-day daily projection with precipitation probability progress bars.
3. **Radar**: Interactive live Doppler radar map centered on Bangladesh, with playback controls (Play, Pause, Step Forward/Back), timeline scrubbing slider, zoom controls, and precipitation intensity legend.
4. **Alerts**: Official inland riverport warnings and maritime cyclone danger signals issued by the BMD and meteorological advisories.
5. **Settings**: Language switch (English / বাংলা), temperature unit (°C / °F), wind speed unit (km/h, m/s, mph), theme mode (System / Light / Dark), and GPS location toggle.

---

## Project Structure

```
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/example/
│   │   │   ├── MainActivity.kt
│   │   │   ├── data/
│   │   │   │   ├── local/              # Room DB (WeatherDao, AppDatabase) & DataStore
│   │   │   │   ├── model/              # Unified weather, RainViewer & BMD models
│   │   │   │   ├── remote/             # Retrofit APIs (WeatherApi, BmdApi, RadarApi)
│   │   │   │   └── repository/         # Repository implementations
│   │   │   ├── domain/repository/      # Clean architecture repository interfaces
│   │   │   ├── location/               # FusedLocationTracker & Bangladesh 64 Districts
│   │   │   ├── navigation/             # Type-safe bottom navigation & routes
│   │   │   └── ui/                     # Jetpack Compose Screens, ViewModels & Theme
│   │   └── res/
│   │       ├── values/strings.xml      # English strings
│   │       └── values-bn/strings.xml   # Bengali strings
├── backend/                            # Node.js/Express/TypeScript API Proxy
│   ├── src/providers/                  # OpenWeather, BMD & RainViewer providers
│   ├── src/server.ts                   # Express server entry point
│   └── package.json
├── gradle/
│   └── libs.versions.toml              # Version Catalog
├── .env.example                        # Template for environment configuration
└── build.gradle.kts
```

---

## Build & Run Instructions

### 1. Android Application Build

To build the debug APK using Gradle:
```bash
./gradlew assembleDebug
```
The output APK is generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

To build a signed release APK:
```bash
./gradlew assembleRelease
```
The release APK is generated at:
```
app/build/outputs/apk/release/app-release-unsigned.apk
```

To run unit tests:
```bash
./gradlew test
```

### 2. Backend Proxy Server (Optional)

```bash
cd backend
npm install
cp .env.example .env
# Edit .env with your OPENWEATHER_API_KEY
npm run dev
```

---

## License

Built for Bangladesh Meteorological Monitoring and Disaster Preparedness.
