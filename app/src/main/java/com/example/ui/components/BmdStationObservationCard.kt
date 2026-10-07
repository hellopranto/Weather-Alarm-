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
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BmdStatusModel
import com.example.util.BanglaUtils

@Composable
fun BmdStationObservationCard(
    bmd: BmdStatusModel?,
    modifier: Modifier = Modifier
) {
    if (bmd == null || bmd.station.isNullOrBlank()) return

    val stationName = formatStationName(bmd.station)
    val distance = bmd.distanceKm?.let { "${BanglaUtils.toBanglaDigits(it.toInt())} কিমি" } ?: "নিকটবর্তী"
    val obs = bmd.observation
    val isStale = bmd.isStale

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bmd_station_observation_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x2A153A6B)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x444FC3F7), Color(0x110288D1))
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: BMD Icon + Station Info + Stale/Live Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x3300E5FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "BMD Station",
                            tint = Color(0xFF80D8FF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "নিকটতম BMD পর্যবেক্ষণ স্টেশন",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF80D8FF),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$stationName (দূরত্ব: $distance)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isStale) Color(0x33FFB300) else Color(0x3300E676),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isStale) Color(0xFFFFB300).copy(alpha = 0.5f) else Color(0xFF00E676).copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = if (isStale) "পূর্বের তথ্য" else "সক্রিয় স্টেশন",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isStale) Color(0xFFFFD54F) else Color(0xFF69F0AE),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Observations Grid if station has data
            if (obs != null && (obs.temperatureC != 0.0 || obs.humidityPercent != 0)) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BmdObsMetricItem(
                        icon = Icons.Default.Thermostat,
                        label = "তাপমাত্রা",
                        value = "${BanglaUtils.toBanglaDigits(obs.temperatureC.toInt())}° সে."
                    )
                    BmdObsMetricItem(
                        icon = Icons.Default.WaterDrop,
                        label = "আর্দ্রতা",
                        value = "${BanglaUtils.toBanglaDigits(obs.humidityPercent)}%"
                    )
                    BmdObsMetricItem(
                        icon = Icons.Default.Air,
                        label = "বায়ুর গতি",
                        value = "${BanglaUtils.toBanglaDigits(obs.windSpeedKmh.toInt())} কিমি/ঘ"
                    )
                    BmdObsMetricItem(
                        icon = Icons.Default.Speed,
                        label = "বায়ুচাপ",
                        value = "${BanglaUtils.toBanglaDigits(obs.pressureHpa.toInt())} hPa"
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "উৎস: বাংলাদেশ আবহাওয়া অধিদপ্তর (BMD Synop ও AWS নেটওয়ার্ক)",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
private fun BmdObsMetricItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFF81D4FA),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}
