package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.data.model.AirQualityModel
import com.example.util.BanglaUtils

@Composable
fun AirQualitySection(
    airQuality: AirQualityModel?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("air_quality_card"),
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
                        imageVector = Icons.Default.Air,
                        contentDescription = "Air Quality",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "বাতাসের গুণগত মান",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (airQuality?.aqi == null) {
                Text(
                    text = "বাতাসের গুণগত মান সংক্রান্ত তথ্য বর্তমানে অনুপলব্ধ।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f)
                )
            } else {
                val aqi = airQuality.aqi
                val categoryBn = airQuality.categoryBn ?: when {
                    aqi <= 50 -> "ভালো"
                    aqi <= 100 -> "মাঝারি"
                    aqi <= 150 -> "সংবেদনশীল গোষ্ঠীর জন্য অস্বাস্থ্যকর"
                    aqi <= 200 -> "অস্বাস্থ্যকর"
                    aqi <= 300 -> "খুব অস্বাস্থ্যকর"
                    else -> "বিপজ্জনক"
                }

                val aqiColor = when {
                    aqi <= 50 -> Color(0xFF4CAF50)
                    aqi <= 100 -> Color(0xFFFFEB3B)
                    aqi <= 150 -> Color(0xFFFF9800)
                    aqi <= 200 -> Color(0xFFF44336)
                    aqi <= 300 -> Color(0xFF9C27B0)
                    else -> Color(0xFF795548)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = BanglaUtils.toBanglaDigits(aqi),
                                fontSize = 42.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AQI",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        Text(
                            text = categoryBn,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = aqiColor
                        )
                    }

                    // PM2.5 and PM10 pills
                    Column(horizontalAlignment = Alignment.End) {
                        if (airQuality.pm25 != null) {
                            Text(
                                text = "PM2.5: ${BanglaUtils.toBanglaDigits(airQuality.pm25)} µg/m³",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        if (airQuality.pm10 != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "PM10: ${BanglaUtils.toBanglaDigits(airQuality.pm10)} µg/m³",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Horizontal AQI Scale
                val gradient = Brush.horizontalGradient(
                    listOf(
                        Color(0xFF4CAF50), // ভালো
                        Color(0xFFFFEB3B), // মাঝারি
                        Color(0xFFFF9800), // সংবেদনশীল গোষ্ঠীর জন্য অস্বাস্থ্যকর
                        Color(0xFFF44336), // অস্বাস্থ্যকর
                        Color(0xFF9C27B0), // খুব অস্বাস্থ্যকর
                        Color(0xFF795548)  // বিপজ্জনক
                    )
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(gradient)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("০", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                    Text("৫০", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                    Text("১০০", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                    Text("১৫০", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                    Text("২০০", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                    Text("৩০০+", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                }
            }
        }
    }
}
