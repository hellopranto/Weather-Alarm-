package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.TemperatureUnit
import com.example.data.model.HourlyForecastModel
import com.example.data.model.UnifiedWeatherResponse
import com.example.ui.components.BangladeshDistrictPickerSheet
import com.example.ui.components.BmdStationObservationCard
import com.example.ui.components.ErrorStateView
import com.example.ui.components.OfflineStatusBanner
import com.example.ui.components.WeatherConditionIcon
import com.example.ui.components.WeatherMetricsGrid
import com.example.ui.theme.WeatherAlertWarning
import com.example.util.TimeUtils

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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
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
                        CircularProgressIndicator(modifier = Modifier.testTag("home_loading"))
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
                            onOpenPicker = { isPickerOpen = true },
                            onUseGps = { viewModel.useGps() },
                            onRefresh = { viewModel.loadWeather(forceRefresh = true) },
                            onNavigateToAlerts = onNavigateToAlerts
                        )
                    } else {
                        ErrorStateView(
                            message = state.message.ifBlank { stringResource(R.string.error_generic) },
                            onRetry = { viewModel.loadWeather(forceRefresh = true) }
                        )
                    }
                }
                is WeatherUiState.Success -> {
                    HomeContent(
                        weather = state.data,
                        isOffline = state.isOfflineCached,
                        tempUnit = state.userPreferences.temperatureUnit,
                        windUnit = state.userPreferences.windUnit,
                        isRefreshing = isRefreshing,
                        isGpsLocating = isGpsLocating,
                        onOpenPicker = { isPickerOpen = true },
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
    windUnit: com.example.data.local.WindUnit = com.example.data.local.WindUnit.KMH,
    isRefreshing: Boolean = false,
    isGpsLocating: Boolean = false,
    onOpenPicker: () -> Unit,
    onUseGps: () -> Unit,
    onRefresh: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val areaTitle = remember(weather.location) {
        val village = weather.location.village?.trim().orEmpty()
        val name = weather.location.name.trim()
        if (village.isNotBlank()) village else if (name.isNotBlank()) name else "Bangladesh"
    }

    val adminHierarchy = remember(weather.location) {
        val parts = mutableListOf<String>()
        val upazila = weather.location.upazila?.trim().orEmpty()
        val district = weather.location.district?.trim().orEmpty()
        val division = weather.location.division?.trim().orEmpty()

        if (upazila.isNotBlank()) parts.add(upazila)
        if (district.isNotBlank() && !district.equals(upazila, ignoreCase = true) && !district.contains(upazila, ignoreCase = true)) {
            parts.add(district)
        }
        if (division.isNotBlank() && !division.equals(district, ignoreCase = true)) {
            parts.add(division)
        }
        if (parts.isNotEmpty()) parts.joinToString(" • ") else weather.location.country
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Location & Header Action Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_header"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenPicker() }
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = areaTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Change location",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        if (adminHierarchy.isNotBlank()) {
                            Text(
                                text = adminHierarchy,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(12.dp)
                            )
                            val displayTime = formatLastUpdateTime(weather.updatedAt)
                            Text(
                                text = stringResource(R.string.last_updated, displayTime),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onRefresh,
                        enabled = !isRefreshing,
                        modifier = Modifier.testTag("btn_refresh_top")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.pull_to_refresh),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = onUseGps,
                        enabled = !isGpsLocating,
                        modifier = Modifier.testTag("btn_gps_location")
                    ) {
                        if (isGpsLocating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = stringResource(R.string.use_gps),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = onOpenPicker,
                        modifier = Modifier.testTag("btn_search_location")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.search_title),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Offline Banner
        if (isOffline) {
            item {
                OfflineStatusBanner(lastUpdated = weather.updatedAt)
            }
        }

        // Active Alert Warning Banner if present
        if (weather.alerts.isNotEmpty()) {
            item {
                val topAlert = weather.alerts[0]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAlerts() }
                        .testTag("home_alert_banner"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = WeatherAlertWarning.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = WeatherAlertWarning,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = topAlert.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.alert_source_prefix, topAlert.source),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Main Weather Hero Card
        item {
            val currentTemp = formatTemp(weather.current.temperature, tempUnit)
            val feelsLike = formatTemp(weather.current.feelsLike, tempUnit)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("main_weather_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.current_weather),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = weather.current.condition,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        WeatherConditionIcon(
                            weatherCode = weather.current.weatherCode,
                            size = 56.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = currentTemp,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.testTag("hero_temperature_text")
                    )

                    Text(
                        text = stringResource(R.string.feels_like, feelsLike),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )

                    if (weather.current.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = weather.current.description.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }

        // Hourly Forecast Horizontal Preview
        if (weather.hourly.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.hourly_forecast),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(weather.hourly, key = { it.timestamp }) { item ->
                            HourlyItemCard(item = item, tempUnit = tempUnit)
                        }
                    }
                }
            }
        }

        // BMD Station Observation Details
        if (weather.bmd != null && weather.bmd.observation != null) {
            item {
                BmdStationObservationCard(observation = weather.bmd.observation)
            }
        }

        // Weather Metrics Grid (Humidity, Wind, Pressure, Visibility, Sunrise, Sunset)
        item {
            Text(
                text = stringResource(R.string.current_weather) + " Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            WeatherMetricsGrid(
                humidity = weather.current.humidity,
                windSpeedKmh = weather.current.windSpeed,
                windDirectionDegrees = weather.current.windDirection,
                pressureHpa = weather.current.pressure,
                visibilityMeters = weather.current.visibility,
                sunrise = TimeUtils.formatBangladeshSunTime(weather.current.sunrise),
                sunset = TimeUtils.formatBangladeshSunTime(weather.current.sunset),
                rainProbability = weather.current.rainProbability,
                precipitationMm = weather.current.precipitationMm,
                windUnit = windUnit
            )
        }
    }
}

@Composable
fun HourlyItemCard(
    item: HourlyForecastModel,
    tempUnit: TemperatureUnit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(80.dp)
            .testTag("hourly_item_${item.timeString}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val displayHour = remember(item.timestamp, item.timeString) {
                TimeUtils.formatBangladeshHour(item.timestamp, item.timeString)
            }
            Text(
                text = displayHour,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            WeatherConditionIcon(
                weatherCode = item.weatherCode,
                size = 28.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatTemp(item.temperature, tempUnit),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${item.rainProbability}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

fun formatTemp(celsius: Double, unit: TemperatureUnit): String {
    return when (unit) {
        TemperatureUnit.CELSIUS -> "%.0f°C".format(celsius)
        TemperatureUnit.FAHRENHEIT -> "%.0f°F".format(celsius * 9 / 5 + 32)
    }
}

fun formatLastUpdateTime(isoOrDate: String): String {
    return TimeUtils.formatLastUpdateTime(isoOrDate)
}
