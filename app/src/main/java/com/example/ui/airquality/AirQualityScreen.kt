package com.example.ui.airquality

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Masks
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.AirQualityResponse
import com.example.data.model.DailyAirQualityModel
import com.example.data.model.HourlyAirQualityModel
import com.example.ui.components.ErrorStateView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirQualityScreen(
    viewModel: AirQualityViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val selectedStandard by viewModel.selectedStandard.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.air_quality_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val subtitle = (uiState as? AirQualityUiState.Success)?.cityName ?: "Bangladesh"
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_aqi_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_close)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadAirQuality(forceRefresh = true) },
                        enabled = !isRefreshing,
                        modifier = Modifier.testTag("btn_aqi_refresh")
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.loadAirQuality(forceRefresh = true) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("aqi_pull_refresh")
        ) {
            when (val state = uiState) {
                is AirQualityUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.testTag("aqi_loading"))
                    }
                }
                is AirQualityUiState.Error -> {
                    if (state.cachedData != null) {
                        AirQualityContent(
                            data = state.cachedData,
                            standard = selectedStandard,
                            onStandardChanged = { viewModel.setAqiStandard(it) }
                        )
                    } else {
                        ErrorStateView(
                            message = state.message.ifBlank { stringResource(R.string.error_generic) },
                            onRetry = { viewModel.loadAirQuality(forceRefresh = true) }
                        )
                    }
                }
                is AirQualityUiState.Success -> {
                    AirQualityContent(
                        data = state.data,
                        standard = state.selectedStandard,
                        onStandardChanged = { viewModel.setAqiStandard(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun AirQualityContent(
    data: AirQualityResponse,
    standard: AqiStandard,
    onStandardChanged: (AqiStandard) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Standard Selector Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = standard == AqiStandard.US,
                    onClick = { onStandardChanged(AqiStandard.US) },
                    label = { Text("US EPA AQI Standard (0–500)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                FilterChip(
                    selected = standard == AqiStandard.EUROPEAN,
                    onClick = { onStandardChanged(AqiStandard.EUROPEAN) },
                    label = { Text("European EAQI (0–100+)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // AQI Visual Gauge Hero Card
        item {
            AqiGaugeHeroCard(data = data, standard = standard)
        }

        // 24-Hour Peak Forecast Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "24-Hour Peak AQI: ${data.summary24h.peakUsAqi} (${data.summary24h.peakCategory})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Average PM2.5: ${data.summary24h.averagePm2_5} µg/m³",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Pollutants Breakdown Grid
        item {
            Text(
                text = "Atmospheric Pollutants",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PollutantCard(
                        name = stringResource(R.string.pm25_label),
                        value = "${data.current.pm2_5}",
                        unit = data.units.pm2_5,
                        severityColor = getPm25Color(data.current.pm2_5),
                        modifier = Modifier.weight(1f)
                    )
                    PollutantCard(
                        name = stringResource(R.string.pm10_label),
                        value = "${data.current.pm10}",
                        unit = data.units.pm10,
                        severityColor = getPm10Color(data.current.pm10),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PollutantCard(
                        name = stringResource(R.string.no2_label),
                        value = "${data.current.nitrogenDioxide}",
                        unit = data.units.nitrogenDioxide,
                        modifier = Modifier.weight(1f)
                    )
                    PollutantCard(
                        name = stringResource(R.string.o3_label),
                        value = "${data.current.ozone}",
                        unit = data.units.ozone,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PollutantCard(
                        name = stringResource(R.string.so2_label),
                        value = "${data.current.sulphurDioxide}",
                        unit = data.units.sulphurDioxide,
                        modifier = Modifier.weight(1f)
                    )
                    PollutantCard(
                        name = stringResource(R.string.co_label),
                        value = "${data.current.carbonMonoxide}",
                        unit = data.units.carbonMonoxide,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Tailored Health Guidance Panel
        item {
            HealthGuidancePanel(guidance = data.healthGuidance)
        }

        // Hourly Air Quality Forecast
        if (data.hourly.isNotEmpty()) {
            item {
                Text(
                    text = "24-Hour Air Quality Forecast",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(data.hourly.take(24)) { hour ->
                            HourlyAqiItem(hour = hour, standard = standard)
                        }
                    }
                }
            }
        }

        // 7-Day Daily Forecast
        if (data.daily.isNotEmpty()) {
            item {
                Text(
                    text = "7-Day Air Quality Outlook",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            items(data.daily) { day ->
                DailyAqiRow(day = day, standard = standard)
            }
        }

        // Source and Metadata Disclaimer
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${data.source} • ${stringResource(R.string.modeled_data_disclaimer)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AqiGaugeHeroCard(
    data: AirQualityResponse,
    standard: AqiStandard,
    modifier: Modifier = Modifier
) {
    val aqiValue = if (standard == AqiStandard.US) data.current.usAqi else data.current.europeanAqi
    val categoryName = if (standard == AqiStandard.US) data.current.usAqiCategory else data.current.europeanAqiCategory
    val aqiColor = if (standard == AqiStandard.US) getUsAqiColor(aqiValue) else getEuropeanAqiColor(aqiValue)
    val maxScale = if (standard == AqiStandard.US) 350f else 100f
    val fraction = (aqiValue / maxScale).coerceIn(0.05f, 1f)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = aqiColor.copy(alpha = 0.12f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (standard == AqiStandard.US) "United States EPA AQI" else "European Union EAQI",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Circular Visual Gauge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(140.dp)
            ) {
                Canvas(modifier = Modifier.size(130.dp)) {
                    // Track background
                    drawArc(
                        color = Color.LightGray.copy(alpha = 0.4f),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Active gauge
                    drawArc(
                        color = aqiColor,
                        startAngle = 135f,
                        sweepAngle = 270f * fraction,
                        useCenter = false,
                        style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$aqiValue",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = aqiColor
                    )
                    Text(
                        text = "AQI",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(aqiColor)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun PollutantCard(
    name: String,
    value: String,
    unit: String,
    severityColor: Color? = null,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = severityColor ?: MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun HealthGuidancePanel(
    guidance: com.example.data.model.AirQualityHealthGuidanceModel
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.health_advisory_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val genText = if (guidance.generalBn.isNotBlank()) guidance.generalBn else guidance.generalEn
            if (genText.isNotBlank()) {
                Text(
                    text = genText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            val sensText = if (guidance.sensitiveGroupsBn.isNotBlank()) guidance.sensitiveGroupsBn else guidance.sensitiveGroupsEn
            if (sensText.isNotBlank()) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = sensText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            val maskText = if (guidance.outdoorActivitiesBn.isNotBlank()) guidance.outdoorActivitiesBn else guidance.outdoorActivitiesEn
            if (maskText.isNotBlank()) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Masks,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = maskText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun HourlyAqiItem(
    hour: HourlyAirQualityModel,
    standard: AqiStandard
) {
    val aqi = if (standard == AqiStandard.US) hour.usAqi else hour.europeanAqi
    val aqiColor = if (standard == AqiStandard.US) getUsAqiColor(aqi) else getEuropeanAqiColor(aqi)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(58.dp)
    ) {
        Text(
            text = formatHour(hour.time),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(aqiColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$aqi",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = aqiColor
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${hour.pm2_5}",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun DailyAqiRow(
    day: DailyAirQualityModel,
    standard: AqiStandard
) {
    val aqi = if (standard == AqiStandard.US) day.maxUsAqi else day.maxEuropeanAqi
    val aqiColor = if (standard == AqiStandard.US) getUsAqiColor(aqi) else getEuropeanAqiColor(aqi)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = day.date,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Avg PM2.5: ${day.avgPm2_5} µg/m³",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(aqiColor.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$aqi AQI",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = aqiColor
                )
            }
        }
    }
}

fun getUsAqiColor(aqi: Int): Color {
    return when {
        aqi <= 50 -> Color(0xFF2E7D32)   // Good (Green)
        aqi <= 100 -> Color(0xFFF9A825)  // Moderate (Yellow/Gold)
        aqi <= 150 -> Color(0xFFEF6C00)  // Sensitive Groups (Orange)
        aqi <= 200 -> Color(0xFFC62828)  // Unhealthy (Red)
        aqi <= 300 -> Color(0xFF6A1B9A)  // Very Unhealthy (Purple)
        else -> Color(0xFF4E342E)        // Hazardous (Maroon)
    }
}

fun getEuropeanAqiColor(aqi: Int): Color {
    return when {
        aqi <= 20 -> Color(0xFF2E7D32)
        aqi <= 40 -> Color(0xFF558B2F)
        aqi <= 60 -> Color(0xFFF9A825)
        aqi <= 80 -> Color(0xFFEF6C00)
        else -> Color(0xFFC62828)
    }
}

fun getPm25Color(pm25: Double): Color {
    return when {
        pm25 <= 12.0 -> Color(0xFF2E7D32)
        pm25 <= 35.4 -> Color(0xFFF9A825)
        pm25 <= 55.4 -> Color(0xFFEF6C00)
        pm25 <= 150.4 -> Color(0xFFC62828)
        else -> Color(0xFF6A1B9A)
    }
}

fun getPm10Color(pm10: Double): Color {
    return when {
        pm10 <= 54.0 -> Color(0xFF2E7D32)
        pm10 <= 154.0 -> Color(0xFFF9A825)
        pm10 <= 254.0 -> Color(0xFFEF6C00)
        else -> Color(0xFFC62828)
    }
}

private fun formatHour(isoTime: String): String {
    if (isoTime.isBlank()) return "--:--"
    return try {
        if (isoTime.contains("T")) {
            isoTime.substringAfter("T").take(5)
        } else {
            isoTime
        }
    } catch (_: Exception) {
        isoTime
    }
}
