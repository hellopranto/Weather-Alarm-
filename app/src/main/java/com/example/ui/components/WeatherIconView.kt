package com.example.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun WeatherConditionIcon(
    weatherCode: Int,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val (icon, tint) = when (weatherCode) {
        0, 800 -> Icons.Default.WbSunny to Color(0xFFFFD54F)
        1, 801 -> Icons.Default.WbCloudy to Color(0xFFFFE082)
        2, 802 -> Icons.Default.Cloud to Color(0xFFB0BEC5)
        3, 803, 804 -> Icons.Default.Cloud to Color(0xFF90A4AE)
        45, 48, 701, 741 -> Icons.Default.Air to Color(0xFFCFD8DC)
        51, 53, 55, 300 -> Icons.Default.WaterDrop to Color(0xFF81D4FA)
        61, 63, 65, 500, 501, 502 -> Icons.Default.WaterDrop to Color(0xFF4FC3F7)
        80, 81, 82 -> Icons.Default.Thunderstorm to Color(0xFF64B5F6)
        95, 96, 99, 200, 211 -> Icons.Default.Thunderstorm to Color(0xFFFFB74D)
        else -> Icons.Default.WbCloudy to Color(0xFFECEFF1)
    }

    Icon(
        imageVector = icon,
        contentDescription = "Weather condition",
        tint = tint,
        modifier = modifier.size(size)
    )
}
