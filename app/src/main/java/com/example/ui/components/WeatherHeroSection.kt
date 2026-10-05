package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TemperatureUnit
import com.example.data.model.CurrentWeatherModel
import com.example.data.model.LocationModel
import com.example.data.model.StationModel
import com.example.util.BanglaUtils

@Composable
fun WeatherTopHeader(
    location: LocationModel,
    isRefreshing: Boolean,
    isGpsLocating: Boolean,
    onOpenMenu: () -> Unit,
    onRefresh: () -> Unit,
    onUseGps: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locationTitle = when {
        !location.displayName.isNullOrBlank() -> location.displayName
        !location.upazila.isNullOrBlank() && !location.district.isNullOrBlank() -> "${location.upazila}, ${location.district}"
        location.name.isNotBlank() -> location.name
        else -> "কুড়িগ্রাম সদর, কুড়িগ্রাম"
    }

    val dateSubtitle = BanglaUtils.getTodayBengaliDateString()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag("home_top_header"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Menu ☰ Button
        IconButton(
            onClick = onOpenMenu,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0x2BFFFFFF))
                .testTag("btn_menu_drawer")
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "District Selector",
                tint = Color.White
            )
        }

        // Center Location + Bengali Date
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { onOpenMenu() }
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = locationTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = dateSubtitle,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
        }

        // Action Buttons: GPS + Refresh
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onUseGps,
                enabled = !isGpsLocating,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x2BFFFFFF))
                    .testTag("btn_gps_location")
            ) {
                if (isGpsLocating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "GPS",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            IconButton(
                onClick = onRefresh,
                enabled = !isRefreshing,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x2BFFFFFF))
                    .testTag("btn_refresh_weather")
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WeatherHeroCard(
    current: CurrentWeatherModel,
    station: StationModel?,
    tempUnit: TemperatureUnit,
    modifier: Modifier = Modifier
) {
    val tempText = BanglaUtils.formatTemp(current.temperature, tempUnit)
    val feelsLikeText = BanglaUtils.formatFeelsLike(current.feelsLike, tempUnit)
    val conditionText = current.conditionBn ?: BanglaUtils.mapCondition(current.condition, current.weatherCode)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("main_hero_weather_card"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x33102A4E)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Weather Icon + Condition
            WeatherConditionIcon(
                weatherCode = current.weatherCode,
                size = 72.dp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Large Temperature Display
            Text(
                text = tempText,
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 76.sp,
                modifier = Modifier.testTag("hero_temperature_text")
            )

            // Feels Like Temperature
            if (feelsLikeText.isNotBlank()) {
                Text(
                    text = feelsLikeText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Weather Condition Text
            Text(
                text = conditionText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE1F5FE)
            )

            // High / Low Indicators
            if (current.tempMax != null || current.tempMin != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val high = BanglaUtils.formatTemp(current.tempMax, tempUnit)
                val low = BanglaUtils.formatTemp(current.tempMin, tempUnit)
                Text(
                    text = "সর্বোচ্চ $high  |  সর্বনিম্ন $low",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // BMD Data Source / "আমার পর্যবেক্ষণ" Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x33FFFFFF))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = "BMD",
                        tint = Color(0xFF80DEEA),
                        modifier = Modifier.size(16.dp)
                    )

                    val sourceText = if (station != null && station.name.isNotBlank()) {
                        val distStr = BanglaUtils.toBanglaDigits(station.distanceKm)
                        "BMD ${station.name} (${station.code}) • $distStr কিমি দূরে"
                    } else {
                        "আমার পর্যবেক্ষণ • BMD স্টেশন"
                    }

                    Text(
                        text = sourceText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
