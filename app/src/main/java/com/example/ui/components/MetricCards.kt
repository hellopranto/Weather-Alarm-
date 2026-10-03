package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.WindUnit

@Composable
fun WeatherMetricCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    testTag: String = "metric_card"
) {
    Card(
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun WeatherMetricsGrid(
    humidity: Int,
    windSpeedKmh: Double,
    windDirectionDegrees: Int,
    pressureHpa: Int,
    visibilityMeters: Int,
    sunrise: String,
    sunset: String,
    rainProbability: Int,
    precipitationMm: Double,
    windUnit: WindUnit,
    modifier: Modifier = Modifier
) {
    val formattedWind = when (windUnit) {
        WindUnit.KMH -> "%.1f km/h".format(windSpeedKmh)
        WindUnit.MS -> "%.1f m/s".format(windSpeedKmh / 3.6)
        WindUnit.MPH -> "%.1f mph".format(windSpeedKmh * 0.621371)
    }

    val windDirectionCompass = getWindDirectionText(windDirectionDegrees)
    val visibilityKm = "%.1f km".format(visibilityMeters / 1000.0)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WeatherMetricCard(
                title = stringResource(R.string.humidity),
                value = "$humidity%",
                subtitle = if (humidity > 80) "High humidity" else "Normal",
                icon = Icons.Default.Opacity,
                modifier = Modifier.weight(1f),
                testTag = "card_humidity"
            )
            WeatherMetricCard(
                title = stringResource(R.string.wind),
                value = formattedWind,
                subtitle = "$windDirectionCompass ($windDirectionDegrees°)",
                icon = Icons.Default.Air,
                modifier = Modifier.weight(1f),
                testTag = "card_wind"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WeatherMetricCard(
                title = stringResource(R.string.pressure),
                value = "$pressureHpa hPa",
                subtitle = if (pressureHpa < 1000) "Low (Storm alert)" else "Stable",
                icon = Icons.Default.Compress,
                modifier = Modifier.weight(1f),
                testTag = "card_pressure"
            )
            WeatherMetricCard(
                title = stringResource(R.string.visibility),
                value = visibilityKm,
                subtitle = if (visibilityMeters < 3000) "Hazy / Foggy" else "Good visibility",
                icon = Icons.Default.Visibility,
                modifier = Modifier.weight(1f),
                testTag = "card_visibility"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WeatherMetricCard(
                title = stringResource(R.string.rain_probability),
                value = "$rainProbability%",
                subtitle = if (precipitationMm > 0) "%.1f mm expected".format(precipitationMm) else "No heavy rain",
                icon = Icons.Default.WaterDrop,
                modifier = Modifier.weight(1f),
                testTag = "card_rain_prob"
            )
            WeatherMetricCard(
                title = stringResource(R.string.sunrise),
                value = sunrise,
                subtitle = "Sunset $sunset",
                icon = Icons.Default.WbSunny,
                modifier = Modifier.weight(1f),
                testTag = "card_sun_times"
            )
        }
    }
}

private fun getWindDirectionText(degrees: Int): String {
    val directions = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
    val index = ((degrees + 11.25) / 22.5).toInt() % 16
    return directions[index]
}
