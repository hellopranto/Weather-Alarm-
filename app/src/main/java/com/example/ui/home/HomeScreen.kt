package com.example.ui.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.TemperatureUnit
import com.example.data.model.UnifiedWeatherResponse
import com.example.ui.components.AirQualitySection
import com.example.ui.components.BangladeshDistrictPickerSheet
import com.example.ui.components.HourlyForecastSection
import com.example.ui.components.SunMoonSection
import com.example.ui.components.TenDayForecastSection
import com.example.ui.components.WeatherAlertsSection
import com.example.ui.components.WeatherHeroCard
import com.example.ui.components.WeatherMetricsGridBengali
import com.example.ui.components.WeatherTopHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToAlerts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isGpsLocating by viewModel.isGpsLocating.collectAsStateWithLifecycle()
    val locationMessage by viewModel.locationMessage.collectAsStateWithLifecycle()
    var isPickerOpen by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(locationMessage) {
        locationMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissLocationMessage()
        }
    }

    BangladeshDistrictPickerSheet(
        isOpen = isPickerOpen,
        onDismiss = { isPickerOpen = false },
        onLocationSelected = { city ->
            viewModel.selectCity(city)
        }
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
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.testTag("home_loading")
                            )
                            Text(
                                text = "আবহাওয়ার তথ্য লোড হচ্ছে...",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
                is WeatherUiState.Error -> {
                    if (state.cachedData != null) {
                        HomeContent(
                            weather = state.cachedData,
                            isOffline = true,
                            tempUnit = TemperatureUnit.CELSIUS,
                            isRefreshing = isRefreshing,
                            isGpsLocating = isGpsLocating,
                            onOpenMenu = { isPickerOpen = true },
                            onUseGps = { viewModel.useGps() },
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
                                    text = "BMD থেকে সর্বশেষ তথ্য পাওয়া যাচ্ছে না।",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.75f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.loadWeather(forceRefresh = true) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF0F3870)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("পুনরায় চেষ্টা করুন", fontWeight = FontWeight.Bold)
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
                        onOpenMenu = { isPickerOpen = true },
                        onUseGps = { viewModel.useGps() },
                        onRefresh = { viewModel.loadWeather(forceRefresh = true) },
                        onNavigateToAlerts = onNavigateToAlerts
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
    onOpenMenu: () -> Unit,
    onUseGps: () -> Unit,
    onRefresh: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Header: ☰ | কুড়িগ্রাম সদর, কুড়িগ্রাম | সোমবার, ০৫ অক্টোবর, ২০২৬ | Action icons
        item {
            WeatherTopHeader(
                location = weather.location,
                isRefreshing = isRefreshing,
                isGpsLocating = isGpsLocating,
                onOpenMenu = onOpenMenu,
                onRefresh = onRefresh,
                onUseGps = onUseGps
            )
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

        // 2. Main Weather Hero Card (Large temp, condition, feels like, high/low, BMD station badge)
        item {
            WeatherHeroCard(
                current = weather.current,
                station = weather.station,
                tempUnit = tempUnit
            )
        }

        // 3. Hourly Forecast ("পরবর্তী ২৪ ঘণ্টার পূর্বাভাস")
        if (weather.hourly.isNotEmpty()) {
            item {
                HourlyForecastSection(
                    hourly = weather.hourly,
                    tempUnit = tempUnit
                )
            }
        }

        // 4. 10-Day Forecast ("১০ দিনের পূর্বাভাস")
        if (weather.daily.isNotEmpty()) {
            item {
                TenDayForecastSection(
                    daily = weather.daily,
                    tempUnit = tempUnit
                )
            }
        }

        // 5. Weather Metric Cards (2-column grid in Bengali: বাতাস, বায়ু চাপ, সম্ভাব্য বৃষ্টিপাত, অতিবেগুনী রশ্মি, আর্দ্রতা, দৃশ্যমানতা)
        item {
            WeatherMetricsGridBengali(
                windSpeedKmh = weather.current.windSpeed,
                windDirectionDegrees = weather.current.windDirection,
                pressureHpa = weather.current.pressure,
                rainfallMm = weather.current.rainfall ?: weather.current.precipitationMm,
                uvIndex = weather.current.uvIndex,
                humidity = weather.current.humidity,
                visibilityMeters = weather.current.visibility
            )
        }

        // 6. Sun & Moon ("সূর্য ও চাঁদ")
        if (weather.sunMoon != null) {
            item {
                SunMoonSection(sunMoon = weather.sunMoon)
            }
        }

        // 7. Air Quality ("বাতাসের গুণগত মান")
        item {
            AirQualitySection(airQuality = weather.airQuality)
        }

        // 8. Weather Alerts ("এইরূপ আবহাওয়ায় করণীয়")
        item {
            val allWarnings = if (weather.warnings.isNotEmpty()) weather.warnings else weather.alerts
            WeatherAlertsSection(
                warnings = allWarnings,
                onNavigateToAlerts = onNavigateToAlerts
            )
        }
    }
}

fun formatTemp(celsius: Double?, unit: TemperatureUnit): String {
    if (celsius == null) return "--"
    return when (unit) {
        TemperatureUnit.CELSIUS -> "%.0f°C".format(celsius)
        TemperatureUnit.FAHRENHEIT -> "%.0f°F".format(celsius * 9 / 5 + 32)
    }
}

