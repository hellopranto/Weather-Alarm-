package com.example.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.WbCloudy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.WeatherRadarRainHeavy
import com.example.ui.theme.WeatherTertiaryLight

@Composable
fun WeatherConditionIcon(
    weatherCode: Int,
    isNight: Boolean = false,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val (icon, tint) = when {
        // Thunderstorm / Cyclone (200-232)
        weatherCode in 200..232 -> Pair(
            Icons.Filled.Thunderstorm,
            Color(0xFFFFB300)
        )
        // Drizzle / Rain (300-531)
        weatherCode in 300..531 -> Pair(
            Icons.Filled.WaterDrop,
            WeatherRadarRainHeavy
        )
        // Snow (600-622)
        weatherCode in 600..622 -> Pair(
            Icons.Filled.Grain,
            Color(0xFF81D4FA)
        )
        // Atmosphere: Fog, Mist, Haze (701-781)
        weatherCode in 701..781 -> Pair(
            Icons.Filled.Air,
            Color(0xFF90A4AE)
        )
        // Clear (800)
        weatherCode == 800 -> Pair(
            Icons.Filled.WbSunny,
            if (isNight) Color(0xFFE0E0E0) else WeatherTertiaryLight
        )
        // Few clouds / Scattered (801-802)
        weatherCode in 801..802 -> Pair(
            Icons.Outlined.WbCloudy,
            if (isNight) Color(0xFFB0BEC5) else Color(0xFFFBC02D)
        )
        // Broken / Overcast (803-804)
        else -> Pair(
            Icons.Filled.Cloud,
            Color(0xFF78909C)
        )
    }

    Icon(
        imageVector = icon,
        contentDescription = "Weather Condition",
        tint = tint,
        modifier = modifier.size(size)
    )
}
