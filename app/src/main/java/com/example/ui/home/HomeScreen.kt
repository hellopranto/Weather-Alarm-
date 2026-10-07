package com.example.ui.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.TemperatureUnit
import com.example.data.model.UnifiedWeatherResponse
import com.example.ui.components.AirQualitySection
import com.example.ui.components.BangladeshDistrictPickerSheet
import com.example.ui.components.HourlyForecastSection
import com.example.ui.components.LocationHierarchyCard
import com.example.ui.components.SunMoonSection
import com.example.ui.components.TenDayForecastSection
import com.example.ui.components.WeatherAlertsSection
import com.example.ui.components.WeatherHeroCard
import com.example.ui.components.WeatherLoadingSkeleton
import com.example.ui.components.WeatherMetricsGridBengali
import com.example.ui.components.WeatherTopHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToAlerts: () -> Unit,
    onNavigateToRainPrediction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isGpsLocating by viewModel.isGpsLocating.collectAsStateWithLifecycle()
    val isLiveLocation by viewModel.isLiveLocation.collectAsStateWithLifecycle()
    val locationMessage by viewModel.locationMessage.collectAsStateWithLifecycle()
    var isPickerOpen by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            viewModel.useGps()
        }
    }

    LaunchedEffect(locationMessage) {
        locationMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissLocationMessage()
        }
    }

    BangladeshDistrictPickerSheet(
        isOpen = isPickerOpen,
        onDismiss = { isPickerOpen = false },
        onLocationSelected = { city -> viewModel.selectCity(city) },
        onLocationItemSelected = { item -> viewModel.selectLocationItem(item) },
        onUseLiveLocation = {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        },
        isDetectingLocation = isGpsLocating
    )

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0D3268),
            Color(0xFF1B5AA6),
            Color(0xFF13427E),
            Color(0xFF0A1B36)
        )
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.loadWeather(forceRefresh = true) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("home_pull_refresh")
        ) {
            when (val state = uiState) {
                is WeatherUiState.Loading -> {
                    WeatherLoadingSkeleton()
                }
                is WeatherUiState.Error -> {
                    if (state.cachedData != null) {
                        HomeContent(
                            weather = state.cachedData,
                            isOffline = true,
                            tempUnit = TemperatureUnit.CELSIUS,
                            isRefreshing = isRefreshing,
                            isGpsLocating = isGpsLocating,
                            isLiveLocationActive = isLiveLocation,
                            onOpenSearch = { isPickerOpen = true },
                            onUseGps = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            onRefresh = { viewModel.loadWeather(forceRefresh = true) },
                            onNavigateToAlerts = onNavigateToAlerts
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = state.message.ifBlank { "আবহাওয়ার তথ্য লোড করা যায়নি।" },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = { viewModel.loadWeather(forceRefresh = true) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF81D4FA),
                                        contentColor = Color(0xFF0D3268)
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.testTag("btn_retry_home")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "পুনরায় চেষ্টা করুন",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                is WeatherUiState.Success -> {
                    HomeContent(
                        weather = state.data,
                        isOffline = state.isOfflineCached,
                        tempUnit = state.userPreferences.temperatureUnit,
                        isRefreshing = isRefreshing,
                        isGpsLocating = isGpsLocating,
                        isLiveLocationActive = isLiveLocation,
                        onOpenSearch = { isPickerOpen = true },
                        onUseGps = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        onRefresh = { viewModel.loadWeather(forceRefresh = true) },
                        onNavigateToAlerts = onNavigateToAlerts,
                        onNavigateToRainPrediction = onNavigateToRainPrediction
                    )
                }
            }
        }
    }
}

@Composable
fun HomeContent(
    weather: UnifiedWeatherResponse,
    isOffline: Boolean,
    tempUnit: TemperatureUnit,
    isRefreshing: Boolean = false,
    isGpsLocating: Boolean = false,
    isLiveLocationActive: Boolean = false,
    onOpenSearch: () -> Unit,
    onUseGps: () -> Unit,
    onRefresh: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToRainPrediction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Header: Location, Date, Search trigger & GPS trigger
        item {
            WeatherTopHeader(
                location = weather.location,
                isRefreshing = isRefreshing,
                isGpsLocating = isGpsLocating,
                isLiveLocationActive = isLiveLocationActive,
                updatedAt = weather.updatedAt,
                onOpenSearch = onOpenSearch,
                onRefresh = onRefresh,
                onUseGps = onUseGps
            )
        }

        // Administrative location details hierarchy chip if present
        item {
            LocationHierarchyCard(location = weather.location)
        }

        // Offline Cached Banner if offline
        if (isOffline) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x33000000), shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFFFCC80),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "সর্বশেষ সংরক্ষিত তথ্য প্রদর্শিত হচ্ছে",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // 2. Weather Hero Card (Large temp, condition, feels like, min/max, BMD badge)
        item {
            WeatherHeroCard(
                current = weather.current,
                station = weather.station,
                tempUnit = tempUnit,
                bmd = weather.bmd
            )
        }

        // Nearest BMD Observation Data Card
        if (weather.bmd != null && !weather.bmd.station.isNullOrBlank()) {
            item {
                com.example.ui.components.BmdStationObservationCard(bmd = weather.bmd)
            }
        }

        // 3. Hourly Forecast (24h)
        if (weather.hourly.isNotEmpty()) {
            item {
                HourlyForecastSection(
                    hourly = weather.hourly,
                    tempUnit = tempUnit
                )
            }
        }

        // 4. Daily Forecast (10-Day)
        if (weather.daily.isNotEmpty()) {
            item {
                TenDayForecastSection(
                    daily = weather.daily,
                    tempUnit = tempUnit
                )
            }
        }

        // 5. Weather Metrics Grid (Wind, Pressure, Rain, UV, Humidity, Visibility, Dew point, Cloud coverage, Wind gust)
        item {
            WeatherMetricsGridBengali(
                windSpeedKmh = weather.current.windSpeed,
                windDirectionDegrees = weather.current.windDirection,
                windGustKmh = weather.current.windGust,
                pressureHpa = weather.current.pressure,
                rainfallMm = weather.current.rainfall ?: weather.current.precipitationMm,
                rainProbability = weather.current.rainProbability,
                uvIndex = weather.current.uvIndex,
                humidity = weather.current.humidity,
                visibilityMeters = weather.current.visibility,
                dewPointC = weather.current.dewPoint,
                cloudCoverage = weather.current.cloudCoverage,
                sunshineDuration = weather.current.sunshineDuration
            )
        }

        // 5.1 Real-Time Multi-Source Rain Prediction Bulletin
        item {
            val predictionData = com.example.domain.RainPredictionEngine.generatePrediction(
                weather = weather,
                radar = null,
                lat = weather.location.latitude,
                lon = weather.location.longitude,
                cityName = weather.location.displayName ?: weather.location.name
            )
            com.example.ui.components.RainPredictionSection(
                prediction = predictionData,
                onViewFullPrediction = onNavigateToRainPrediction
            )
        }

        // 6. Air Quality (AQI, PM2.5, PM10, etc.)
        if (weather.airQuality != null && weather.airQuality.aqi != null) {
            item {
                AirQualitySection(airQuality = weather.airQuality)
            }
        }

        // 7. Sun & Moon (Sunrise, Sunset, Moonrise, Moonset)
        if (weather.sunMoon != null) {
            item {
                SunMoonSection(sunMoon = weather.sunMoon)
            }
        }

        // 8. Weather Alerts & Bulletins
        item {
            val allWarnings = if (weather.warnings.isNotEmpty()) weather.warnings else weather.alerts
            WeatherAlertsSection(
                warnings = allWarnings,
                onNavigateToAlerts = onNavigateToAlerts
            )
        }
    }
}
