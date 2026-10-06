package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.BanglaUtils

@Composable
fun WeatherMetricCard(
    title: String,
    value: String,
    subtitle: String?,
    progressFraction: Float? = null,
    progressColor: Color = Color(0xFF81D4FA),
    icon: ImageVector,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x33102A4E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                fontFamily = com.example.ui.theme.AnekBanglaFontFamily,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (progressFraction != null) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progressFraction.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = progressColor,
                    trackColor = Color(0x33FFFFFF)
                )
            }

            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun WeatherMetricsGridBengali(
    windSpeedKmh: Double?,
    windDirectionDegrees: Int?,
    windGustKmh: Double? = null,
    pressureHpa: Int?,
    rainfallMm: Double?,
    rainProbability: Int? = null,
    uvIndex: Double?,
    humidity: Int?,
    visibilityMeters: Int?,
    dewPointC: Double? = null,
    cloudCoverage: Int? = null,
    sunshineDuration: Double? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: বাতাস (Wind) & বায়ু চাপ (Pressure)
        if (windSpeedKmh != null || pressureHpa != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (windSpeedKmh != null) {
                    val windSubtitle = windDirectionDegrees?.let {
                        "${getWindDirectionBengali(it)} (${BanglaUtils.toBanglaDigits(it)}°)"
                    }
                    WeatherMetricCard(
                        title = "বাতাস",
                        value = BanglaUtils.formatWind(windSpeedKmh),
                        subtitle = windSubtitle,
                        progressFraction = (windSpeedKmh / 60.0).toFloat(),
                        progressColor = Color(0xFF81D4FA),
                        icon = Icons.Default.Air,
                        modifier = Modifier.weight(1f),
                        testTag = "card_wind"
                    )
                }
                if (pressureHpa != null) {
                    val presSubtitle = if (pressureHpa < 1000) "নিম্নচাপ সতর্কবার্তা" else "স্বাভাবিক চাপ"
                    WeatherMetricCard(
                        title = "বায়ু চাপ",
                        value = BanglaUtils.formatPressure(pressureHpa),
                        subtitle = presSubtitle,
                        progressFraction = ((pressureHpa - 980) / 40.0).toFloat(),
                        progressColor = Color(0xFFA5D6A7),
                        icon = Icons.Default.Compress,
                        modifier = Modifier.weight(1f),
                        testTag = "card_pressure"
                    )
                }
            }
        }

        // Row 2: সম্ভাব্য বৃষ্টিপাত (Rainfall) & অতিবেগুনী রশ্মি (UV Index)
        if (rainfallMm != null || uvIndex != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (rainfallMm != null) {
                    val rainSubtitle = if (rainfallMm > 0) "রেকর্ডকৃত বৃষ্টি" else "বৃষ্টিপাত নেই"
                    WeatherMetricCard(
                        title = "সম্ভাব্য বৃষ্টিপাত",
                        value = BanglaUtils.formatRainfall(rainfallMm),
                        subtitle = rainSubtitle,
                        progressFraction = (rainfallMm / 50.0).toFloat(),
                        progressColor = Color(0xFF4FC3F7),
                        icon = Icons.Default.WaterDrop,
                        modifier = Modifier.weight(1f),
                        testTag = "card_rain"
                    )
                }
                if (uvIndex != null) {
                    val uvSub = when {
                        uvIndex <= 2 -> "কম ঝুঁকিপূর্ণ"
                        uvIndex <= 5 -> "মাঝারি ঝুঁকি"
                        uvIndex <= 7 -> "উচ্চ ঝুঁকি"
                        uvIndex <= 10 -> "খুব উচ্চ ঝুঁকি"
                        else -> "চরম ঝুঁকি"
                    }
                    WeatherMetricCard(
                        title = "অতিবেগুনী রশ্মি",
                        value = BanglaUtils.formatUvIndex(uvIndex),
                        subtitle = uvSub,
                        progressFraction = (uvIndex / 12.0).toFloat(),
                        progressColor = Color(0xFFFFB74D),
                        icon = Icons.Default.WbSunny,
                        modifier = Modifier.weight(1f),
                        testTag = "card_uv"
                    )
                }
            }
        }

        // Row 3: আর্দ্রতা (Humidity) & দৃশ্যমানতা (Visibility)
        if (humidity != null || visibilityMeters != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (humidity != null) {
                    val humSub = when {
                        humidity > 80 -> "অতিরিক্ত আর্দ্র"
                        humidity > 60 -> "মাঝারি আর্দ্র"
                        else -> "শুষ্ক আবহাওয়া"
                    }
                    WeatherMetricCard(
                        title = "আর্দ্রতা",
                        value = BanglaUtils.formatHumidity(humidity),
                        subtitle = humSub,
                        progressFraction = (humidity / 100.0).toFloat(),
                        progressColor = Color(0xFF80DEEA),
                        icon = Icons.Default.InvertColors,
                        modifier = Modifier.weight(1f),
                        testTag = "card_humidity"
                    )
                }
                if (visibilityMeters != null) {
                    val visSub = if (visibilityMeters >= 10000) "পরিষ্কার দৃশ্যমানতা" else "সীমিত দৃশ্যমানতা"
                    WeatherMetricCard(
                        title = "দৃশ্যমানতা",
                        value = BanglaUtils.formatVisibility(visibilityMeters),
                        subtitle = visSub,
                        progressFraction = (visibilityMeters / 10000.0).toFloat(),
                        progressColor = Color(0xFFB0BEC5),
                        icon = Icons.Default.Visibility,
                        modifier = Modifier.weight(1f),
                        testTag = "card_visibility"
                    )
                }
            }
        }

        // Row 4 (Optional live backend extras): শিশিরাঙ্ক (Dew Point) & মেঘের ঘনত্ব (Cloud Coverage)
        if (dewPointC != null || cloudCoverage != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (dewPointC != null) {
                    WeatherMetricCard(
                        title = "শিশিরাঙ্ক",
                        value = "${BanglaUtils.toBanglaDigits(dewPointC)}°C",
                        subtitle = "ঘনীভবন তাপমাত্রা",
                        progressFraction = (dewPointC / 40.0).toFloat(),
                        progressColor = Color(0xFF81D4FA),
                        icon = Icons.Default.Thermostat,
                        modifier = Modifier.weight(1f),
                        testTag = "card_dew_point"
                    )
                }
                if (cloudCoverage != null) {
                    val cloudSub = when {
                        cloudCoverage > 75 -> "সম্পূর্ণ মেঘলা"
                        cloudCoverage > 25 -> "আংশিক মেঘলা"
                        else -> "পরিষ্কার আকাশ"
                    }
                    WeatherMetricCard(
                        title = "মেঘের আচ্ছাদন",
                        value = "${BanglaUtils.toBanglaDigits(cloudCoverage)}%",
                        subtitle = cloudSub,
                        progressFraction = (cloudCoverage / 100.0).toFloat(),
                        progressColor = Color(0xFF90A4AE),
                        icon = Icons.Default.Cloud,
                        modifier = Modifier.weight(1f),
                        testTag = "card_cloud_coverage"
                    )
                }
            }
        }

        // Row 5: দমকা হাওয়া (Wind Gust) & বৃষ্টির সম্ভাবনা (Rain Probability)
        if (windGustKmh != null || rainProbability != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (windGustKmh != null) {
                    WeatherMetricCard(
                        title = "দমকা হাওয়া",
                        value = BanglaUtils.formatWind(windGustKmh),
                        subtitle = "সর্বোচ্চ গতিবেগ",
                        progressFraction = (windGustKmh / 80.0).toFloat(),
                        progressColor = Color(0xFFFFB74D),
                        icon = Icons.Default.Air,
                        modifier = Modifier.weight(1f),
                        testTag = "card_wind_gust"
                    )
                }
                if (rainProbability != null) {
                    WeatherMetricCard(
                        title = "বৃষ্টির সম্ভাবনা",
                        value = "${BanglaUtils.toBanglaDigits(rainProbability)}%",
                        subtitle = if (rainProbability > 50) "উচ্চ সম্ভাবনা" else "কম সম্ভাবনা",
                        progressFraction = (rainProbability / 100.0).toFloat(),
                        progressColor = Color(0xFF64B5F6),
                        icon = Icons.Default.WaterDrop,
                        modifier = Modifier.weight(1f),
                        testTag = "card_rain_prob"
                    )
                }
            }
        }
    }
}

fun getWindDirectionBengali(degrees: Int): String {
    return when (((degrees % 360) + 360) % 360) {
        in 338..360, in 0..22 -> "উত্তর"
        in 23..67 -> "উত্তর-পূর্ব"
        in 68..112 -> "পূর্ব"
        in 113..157 -> "দক্ষিণ-পূর্ব"
        in 158..202 -> "দক্ষিণ"
        in 203..247 -> "দক্ষিণ-পশ্চিম"
        in 248..292 -> "পশ্চিম"
        in 293..337 -> "উত্তর-পশ্চিম"
        else -> "উত্তর"
    }
}
