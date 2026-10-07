package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.RainPredictionResponse
import com.example.data.model.RainTimelinePointModel
import com.example.util.BanglaUtils

@Composable
fun RainPredictionSection(
    prediction: RainPredictionResponse,
    onViewFullPrediction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val rain = prediction.rain
    val metrics = prediction.prediction
    val radar = prediction.radar
    val heavyWarning = prediction.heavyRainWarning
    val timeline = prediction.timeline

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("rain_prediction_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x33102A4E)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x5581D4FA), Color(0x111E88E5))
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: Title + Confidence Badge
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
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x3300E5FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Umbrella,
                            contentDescription = "Rain Prediction",
                            tint = Color(0xFF80D8FF),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "বৃষ্টির পূর্বাভাস",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "রিয়েল-টাইম মাল্টি-সোর্স পূর্বাভাস ইঞ্জিন",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }

                // Confidence Chip
                prediction.confidence?.let { conf ->
                    val (badgeBg, badgeText) = when {
                        conf.contains("উচ্চ") -> Pair(Color(0x3300E676), Color(0xFF69F0AE))
                        conf.contains("মাঝারি") -> Pair(Color(0x33FFD600), Color(0xFFFFE57F))
                        else -> Pair(Color(0x33B0BEC5), Color(0xFFCFD8DC))
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = badgeBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeText.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = conf,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = badgeText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Heavy Rain Warning Banner if active
            AnimatedVisibility(visible = heavyWarning != null && heavyWarning.isWarningActive) {
                heavyWarning?.let { warning ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("heavy_rain_warning_banner"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0x44D32F2F)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = "Heavy Rain Warning",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "⚠️ ভারী বৃষ্টির সতর্কতা",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF8A80),
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${warning.expectedStart ?: "নিকটবর্তী সময়ে"} ভারী বর্ষণের সম্ভাবনা। সম্ভাব্য পরিমাণ: ${warning.expectedRainfallAmount ?: "অধিক"}।",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // Rain ETA & Intensity Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (rain?.expected == true) Color(0x440D47A1) else Color(0x22FFFFFF)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rain ETA
                        Column {
                            Text(
                                text = "বৃষ্টি শুরু হতে পারে",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            val etaText = when {
                                rain?.startInMinutes == 0 -> "এখনই বৃষ্টি হচ্ছে"
                                rain?.startInMinutes != null -> "প্রায় ${BanglaUtils.toBanglaDigits(rain.startInMinutes)} মিনিটের মধ্যে"
                                else -> "আগামী কয়েক ঘণ্টায় নেই"
                            }
                            Text(
                                text = etaText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (rain?.expected == true) Color(0xFF80D8FF) else Color.White
                            )
                        }

                        // Intensity Badge
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "তীব্রতা",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            val intensityBn = rain?.intensityBn ?: "বৃষ্টি নেই"
                            val intensityColor = when (intensityBn) {
                                "ভারী", "অতি ভারী" -> Color(0xFFFF5252)
                                "মাঝারি" -> Color(0xFFFFD54F)
                                "হালকা" -> Color(0xFF81D4FA)
                                else -> Color(0xFFB0BEC5)
                            }
                            Text(
                                text = intensityBn,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = intensityColor
                            )
                        }
                    }

                    // Duration if available
                    if (rain?.durationMinutes != null && rain.durationMinutes > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Color(0xFF81D4FA),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "সম্ভাব্য স্থায়িত্ব: প্রায় ${BanglaUtils.toBanglaDigits(rain.durationMinutes)} মিনিট",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Immediate Prediction Grid (15m, 30m, 1h, 3h)
            Text(
                text = "🌧️ বৃষ্টির সম্ভাবনা (তাৎক্ষণিক)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PredictionProbabilityChip(
                    timeLabel = "১৫ মিনিট",
                    probability = metrics?.next15Minutes ?: 0,
                    modifier = Modifier.weight(1f)
                )
                PredictionProbabilityChip(
                    timeLabel = "৩০ মিনিট",
                    probability = metrics?.next30Minutes ?: 0,
                    modifier = Modifier.weight(1f)
                )
                PredictionProbabilityChip(
                    timeLabel = "১ ঘণ্টা",
                    probability = metrics?.next1Hour ?: 0,
                    modifier = Modifier.weight(1f)
                )
                PredictionProbabilityChip(
                    timeLabel = "৩ ঘণ্টা",
                    probability = metrics?.next3Hours ?: 0,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Radar Status Pill
            radar?.let { r ->
                val radarStatusColor = when {
                    r.approaching -> Color(0xFFFFAB40)
                    r.statusTextBn == "বৃষ্টি দূরে সরে যাচ্ছে" -> Color(0xFF81D4FA)
                    else -> Color(0xFF90A4AE)
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0x22FFFFFF)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = "Radar",
                            tint = radarStatusColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "রাডার পর্যবেক্ষণ: ${r.statusTextBn}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = radarStatusColor
                            )
                            if (!r.radarMessageBn.isNullOrBlank()) {
                                Text(
                                    text = r.radarMessageBn,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // Timeline UI (Now -> 15m -> 30m -> 1h -> 2h -> 3h)
            if (timeline.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "সময়রেখা (Nowcast Timeline)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(timeline) { point ->
                        TimelinePointCard(point = point)
                    }
                }
            }

            // Footer: BMD station / Source & View full details button
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    prediction.bmdStation?.let { st ->
                        Text(
                            text = "নিকটতম BMD স্টেশন: $st" + (prediction.bmdDistanceKm?.let { " (প্রায় ${BanglaUtils.toBanglaDigits(it.toInt())} কিমি)" } ?: ""),
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    if (prediction.updatedAt.isNotBlank()) {
                        Text(
                            text = "সর্বশেষ আপডেট: ${BanglaUtils.formatBengaliDateTime(prediction.updatedAt)}",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                if (onViewFullPrediction != null) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onViewFullPrediction)
                            .testTag("btn_view_full_rain_prediction"),
                        color = Color(0x3381D4FA)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "বিস্তারিত",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF81D4FA)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFF81D4FA),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PredictionProbabilityChip(
    timeLabel: String,
    probability: Int,
    modifier: Modifier = Modifier
) {
    val probColor = when {
        probability >= 70 -> Color(0xFFFF5252)
        probability >= 45 -> Color(0xFFFFD54F)
        probability >= 25 -> Color(0xFF81D4FA)
        else -> Color(0xFFB0BEC5)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0x22FFFFFF),
        border = androidx.compose.foundation.BorderStroke(1.dp, probColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = timeLabel,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${BanglaUtils.toBanglaDigits(probability)}%",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = probColor
            )
        }
    }
}

@Composable
private fun TimelinePointCard(
    point: RainTimelinePointModel
) {
    val probColor = when {
        point.probability >= 70 -> Color(0xFFFF5252)
        point.probability >= 45 -> Color(0xFFFFD54F)
        point.probability >= 25 -> Color(0xFF81D4FA)
        else -> Color(0xFFB0BEC5)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x2A102A4E)),
        border = androidx.compose.foundation.BorderStroke(1.dp, probColor.copy(alpha = 0.35f)),
        modifier = Modifier.width(76.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = point.timeLabel,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Icon(
                imageVector = if (point.probability >= 50) Icons.Default.WaterDrop else Icons.Default.Umbrella,
                contentDescription = null,
                tint = probColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${BanglaUtils.toBanglaDigits(point.probability)}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = probColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = point.intensityBn,
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
    }
}
