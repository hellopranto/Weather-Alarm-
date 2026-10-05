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
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Opacity
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
    subtitle: String? = null,
    progressFraction: Float? = null,
    progressColor: Color = Color(0xFF64B5F6),
    icon: ImageVector,
    modifier: Modifier = Modifier,
    testTag: String = "metric_card"
) {
    Card(
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x33102A4E)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x33FFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (progressFraction != null) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progressFraction.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = progressColor,
                    trackColor = Color(0x33FFFFFF)
                )
            }

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * 2-column grid of Weather Metric Cards in Bengali:
 * বাতাস, বায়ু চাপ, সম্ভাব্য বৃষ্টিপাত, অতিবেগুনী রশ্মি, আর্দ্রতা, দৃশ্যমানতা
 * Does not display values when backend returns null.
 */
@Composable
fun WeatherMetricsGridBengali(
    windSpeedKmh: Double?,
    windDirectionDegrees: Int?,
    pressureHpa: Int?,
    rainfallMm: Double?,
    uvIndex: Double?,
    humidity: Int?,
    visibilityMeters: Int?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: বাতাস (Wind) & বায়ু চাপ (Pressure)
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

        // Row 2: সম্ভাব্য বৃষ্টিপাত (Rain) & অতিবেগুনী রশ্মি (UV Index)
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

        // Row 3: আর্দ্রতা (Humidity) & দৃশ্যমানতা (Visibility)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (humidity != null) {
                val humSub = if (humidity > 75) "উচ্চ আর্দ্রতা" else "স্বস্তিদায়ক"
                WeatherMetricCard(
                    title = "আর্দ্রতা",
                    value = BanglaUtils.formatHumidity(humidity),
                    subtitle = humSub,
                    progressFraction = (humidity / 100.0).toFloat(),
                    progressColor = Color(0xFF80DEEA),
                    icon = Icons.Default.Opacity,
                    modifier = Modifier.weight(1f),
                    testTag = "card_humidity"
                )
            }

            if (visibilityMeters != null) {
                val visSub = if (visibilityMeters < 3000) "কুয়াশাচ্ছন্ন" else "পরিষ্কার দৃশ্যমানতা"
                WeatherMetricCard(
                    title = "দৃশ্যমানতা",
                    value = BanglaUtils.formatVisibility(visibilityMeters),
                    subtitle = visSub,
                    progressFraction = (visibilityMeters / 10000.0).toFloat(),
                    progressColor = Color(0xFFCE93D8),
                    icon = Icons.Default.Visibility,
                    modifier = Modifier.weight(1f),
                    testTag = "card_visibility"
                )
            }
        }
    }
}

private fun getWindDirectionBengali(degrees: Int): String {
    val directions = arrayOf(
        "উত্তর", "উত্তর-উত্তরপূর্ব", "উত্তর-পূর্ব", "পূর্ব-উত্তরপূর্ব",
        "পূর্ব", "পূর্ব-দক্ষিণপূর্ব", "দক্ষিণ-পূর্ব", "দক্ষিণ-দক্ষিণপূর্ব",
        "দক্ষিণ", "দক্ষিণ-দক্ষিণপশ্চিম", "দক্ষিণ-পশ্চিম", "পশ্চিম-দক্ষিণপশ্চিম",
        "পশ্চিম", "পশ্চিম-উত্তরপশ্চিম", "উত্তর-পশ্চিম", "উত্তর-উত্তরপশ্চিম"
    )
    val index = ((degrees + 11.25) / 22.5).toInt() % 16
    return directions[index]
}
