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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TemperatureUnit
import com.example.data.model.DailyForecastModel
import com.example.util.BanglaUtils

@Composable
fun TenDayForecastSection(
    daily: List<DailyForecastModel>,
    tempUnit: TemperatureUnit,
    modifier: Modifier = Modifier
) {
    if (daily.isEmpty()) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ten_day_forecast_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x33102A4E)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x33FFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "১০ দিনের পূর্বাভাস",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            daily.forEachIndexed { index, item ->
                DailyForecastRowBengali(item = item, tempUnit = tempUnit)
                if (index < daily.size - 1) {
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.1f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DailyForecastRowBengali(
    item: DailyForecastModel,
    tempUnit: TemperatureUnit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Icon
        WeatherConditionIcon(
            weatherCode = item.weatherCode,
            size = 36.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        // বার, তারিখ, অবস্থা
        Column(modifier = Modifier.weight(1f)) {
            val dayName = item.dayNameBn ?: item.dayName
            val dateText = item.dateFormattedBn ?: BanglaUtils.toBanglaDigits(item.date)
            val conditionBn = item.conditionBn ?: BanglaUtils.mapCondition(item.condition, item.weatherCode)

            Text(
                text = dayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = dateText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 11.sp
            )

            Text(
                text = conditionBn,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF81D4FA),
                fontWeight = FontWeight.Medium
            )
        }

        // High / Low temp
        val highTemp = BanglaUtils.formatTemp(item.tempMax, tempUnit)
        val lowTemp = BanglaUtils.formatTemp(item.tempMin, tempUnit)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$highTemp / $lowTemp",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
