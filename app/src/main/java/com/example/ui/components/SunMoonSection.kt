package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SunMoonModel
import com.example.ui.theme.AnekBanglaFontFamily
import com.example.util.BanglaUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun SunMoonSection(
    sunMoon: SunMoonModel?,
    modifier: Modifier = Modifier
) {
    if (sunMoon == null) return
    if (sunMoon.sunrise == null && sunMoon.sunset == null && sunMoon.moonrise == null && sunMoon.moonset == null) return

    // 1. Local real-time timer (updates every 30-60 seconds without calling network)
    var currentMinuteOfDay by remember {
        val cal = Calendar.getInstance()
        mutableIntStateOf(cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE))
    }

    var currentFormattedTime by remember {
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("hh:mm a", Locale.US)
        mutableStateOf(BanglaUtils.formatHourBengali(sdf.format(cal.time)))
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(30_000L) // 30 seconds local recalculation loop
            val cal = Calendar.getInstance()
            currentMinuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val sdf = SimpleDateFormat("hh:mm a", Locale.US)
            currentFormattedTime = BanglaUtils.formatHourBengali(sdf.format(cal.time))
        }
    }

    // 2. Completely independent Sun & Moon progress calculation
    val sunProgress by remember(sunMoon.sunrise, sunMoon.sunset, currentMinuteOfDay) {
        derivedStateOf {
            calculateCycleProgress(
                riseTimeStr = sunMoon.sunrise,
                setTimeStr = sunMoon.sunset,
                currentMinuteOfDay = currentMinuteOfDay
            )
        }
    }

    val moonProgress by remember(sunMoon.moonrise, sunMoon.moonset, currentMinuteOfDay) {
        derivedStateOf {
            calculateCycleProgress(
                riseTimeStr = sunMoon.moonrise,
                setTimeStr = sunMoon.moonset,
                currentMinuteOfDay = currentMinuteOfDay
            )
        }
    }

    // 3. Smooth animated progress for Sun and Moon
    val animatedSunProgress by animateFloatAsState(
        targetValue = sunProgress,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "sun_slider_progress"
    )

    val animatedMoonProgress by animateFloatAsState(
        targetValue = moonProgress,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "moon_slider_progress"
    )

    // Outer card container matching screenshot style
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sun_moon_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x33102A4E)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x3381D4FA), Color(0x111E88E5))
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: Title with Yellow Sun Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
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
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = "Sun and Moon",
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "সূর্য ও চাঁদ",
                            fontFamily = AnekBanglaFontFamily,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "বাস্তব জ্যোতির্বিদ্যা ও সৌর-চন্দ্র ট্র্যাকার",
                            fontFamily = AnekBanglaFontFamily,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }

                // Current local time pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x2AFFFFFF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Text(
                        text = "বর্তমান: $currentFormattedTime",
                        fontFamily = AnekBanglaFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE0F7FA),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Responsive Layout: Stacked or Side-by-side depending on screen width
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWide = maxWidth >= 540.dp
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            SunSliderCard(
                                sunriseStr = sunMoon.sunrise,
                                sunsetStr = sunMoon.sunset,
                                progress = animatedSunProgress,
                                currentMinute = currentMinuteOfDay,
                                currentTimeStr = currentFormattedTime
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            MoonSliderCard(
                                moonriseStr = sunMoon.moonrise,
                                moonsetStr = sunMoon.moonset,
                                moonPhaseBn = sunMoon.moonPhaseBn,
                                progress = animatedMoonProgress,
                                currentMinute = currentMinuteOfDay,
                                currentTimeStr = currentFormattedTime
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SunSliderCard(
                            sunriseStr = sunMoon.sunrise,
                            sunsetStr = sunMoon.sunset,
                            progress = animatedSunProgress,
                            currentMinute = currentMinuteOfDay,
                            currentTimeStr = currentFormattedTime
                        )
                        MoonSliderCard(
                            moonriseStr = sunMoon.moonrise,
                            moonsetStr = sunMoon.moonset,
                            moonPhaseBn = sunMoon.moonPhaseBn,
                            progress = animatedMoonProgress,
                            currentMinute = currentMinuteOfDay,
                            currentTimeStr = currentFormattedTime
                        )
                    }
                }
            }
        }
    }
}

/**
 * ☀️ Sun Slider Card Component
 */
@Composable
private fun SunSliderCard(
    sunriseStr: String?,
    sunsetStr: String?,
    progress: Float,
    currentMinute: Int,
    currentTimeStr: String,
    modifier: Modifier = Modifier
) {
    val riseMinute = parseTimeToMinuteOfDay(sunriseStr)
    val setMinute = parseTimeToMinuteOfDay(sunsetStr)

    val isDaylight = riseMinute != null && setMinute != null && currentMinute in riseMinute..setMinute
    val (statusLabel, statusBg, statusColor) = when {
        isDaylight -> Triple("দিন • দৃশ্যমান", Color(0x33FFB300), Color(0xFFFFD54F))
        riseMinute != null && currentMinute < riseMinute -> Triple("রাত • সূর্যোদয়ের অপেক্ষা", Color(0x3329B6F6), Color(0xFF81D4FA))
        else -> Triple("রাত • সূর্যাস্ত সম্পন্ন", Color(0x3378909C), Color(0xFFCFD8DC))
    }

    val daylightSummary = getDaylightSummary(riseMinute, setMinute, currentMinute)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sun_slider_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x33FFD54F), Color(0x11FF8F00))
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row: ☀️ সূর্য + status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFCA28)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = "Sun",
                            tint = Color(0xFFFFCA28),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "সূর্য",
                        fontFamily = AnekBanglaFontFamily,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = statusBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = statusLabel,
                        fontFamily = AnekBanglaFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Upper labels: সূর্যোদয় ─────────── ☀️ ─────────── সূর্যাস্ত
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "সূর্যোদয়",
                    fontFamily = AnekBanglaFontFamily,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f)
                )
                Text(
                    text = "বর্তমান (${BanglaUtils.toBanglaDigits((progress * 100).toInt())}%)",
                    fontFamily = AnekBanglaFontFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFD54F),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "সূর্যাস্ত",
                    fontFamily = AnekBanglaFontFamily,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Animated Sun Slider Track
            AstronomicalTrack(
                progress = progress,
                isSun = true
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Lower time labels: Backend exact sunrise & sunset times
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sunriseStr?.let { BanglaUtils.formatHourBengali(it) } ?: "--",
                    fontFamily = AnekBanglaFontFamily,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = currentTimeStr,
                    fontFamily = AnekBanglaFontFamily,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = sunsetStr?.let { BanglaUtils.formatHourBengali(it) } ?: "--",
                    fontFamily = AnekBanglaFontFamily,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            if (daylightSummary.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = daylightSummary,
                    fontFamily = AnekBanglaFontFamily,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = Color(0xFFFFECB3)
                )
            }
        }
    }
}

/**
 * 🌙 Moon Slider Card Component
 */
@Composable
private fun MoonSliderCard(
    moonriseStr: String?,
    moonsetStr: String?,
    moonPhaseBn: String?,
    progress: Float,
    currentMinute: Int,
    currentTimeStr: String,
    modifier: Modifier = Modifier
) {
    val riseMinute = parseTimeToMinuteOfDay(moonriseStr)
    val setMinute = parseTimeToMinuteOfDay(moonsetStr)

    val isMoonVisible = when {
        riseMinute == null || setMinute == null -> false
        setMinute > riseMinute -> currentMinute in riseMinute..setMinute
        else -> currentMinute >= riseMinute || currentMinute <= setMinute
    }

    val (statusLabel, statusBg, statusColor) = when {
        isMoonVisible -> Triple("চন্দ্র দৃশ্যমান", Color(0x337E57C2), Color(0xFFD1C4E9))
        else -> Triple("চন্দ্রাস্ত সম্পন্ন", Color(0x33455A64), Color(0xFFCFD8DC))
    }

    val moonSummary = getMoonSummary(riseMinute, setMinute, currentMinute, moonPhaseBn)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("moon_slider_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x33B0BEC5), Color(0x1178909C))
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row: 🌙 চাঁদ + Moon Phase pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0x33ECEFF1)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NightlightRound,
                            contentDescription = "Moon",
                            tint = Color(0xFFECEFF1),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "চাঁদ",
                        fontFamily = AnekBanglaFontFamily,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!moonPhaseBn.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0x33FFFFFF),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFFFFF))
                        ) {
                            Text(
                                text = moonPhaseBn,
                                fontFamily = AnekBanglaFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE0E0E0),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = statusBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = statusLabel,
                            fontFamily = AnekBanglaFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Upper labels: চন্দ্রোদয় ─────────── 🌙 ─────────── চন্দ্রাস্ত
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "চন্দ্রোদয়",
                    fontFamily = AnekBanglaFontFamily,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f)
                )
                Text(
                    text = "বর্তমান (${BanglaUtils.toBanglaDigits((progress * 100).toInt())}%)",
                    fontFamily = AnekBanglaFontFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFD1C4E9),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "চন্দ্রাস্ত",
                    fontFamily = AnekBanglaFontFamily,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Animated Moon Slider Track
            AstronomicalTrack(
                progress = progress,
                isSun = false
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Lower time labels: Backend exact moonrise & moonset times
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = moonriseStr?.let { BanglaUtils.formatHourBengali(it) } ?: "--",
                    fontFamily = AnekBanglaFontFamily,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = currentTimeStr,
                    fontFamily = AnekBanglaFontFamily,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = moonsetStr?.let { BanglaUtils.formatHourBengali(it) } ?: "--",
                    fontFamily = AnekBanglaFontFamily,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            if (moonSummary.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = moonSummary,
                    fontFamily = AnekBanglaFontFamily,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = Color(0xFFE1BEE7)
                )
            }
        }
    }
}

/**
 * Custom smooth animated astronomical slider track and thumb
 */
@Composable
private fun AstronomicalTrack(
    progress: Float,
    isSun: Boolean,
    modifier: Modifier = Modifier
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val indicatorSize = 28.dp

    val activeGradient = if (isSun) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFFFB300),
                Color(0xFFFFD54F),
                Color(0xFFFF7043)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF7E57C2),
                Color(0xFF9FA8DA),
                Color(0xFFECEFF1)
            )
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val availableWidth = maxWidth

        // 1. Background full track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0x28FFFFFF))
        )

        // 2. Active filled track (from start to thumb center)
        val fillWidth = availableWidth * clampedProgress
        if (fillWidth > 0.dp) {
            Box(
                modifier = Modifier
                    .width(fillWidth)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(activeGradient)
            )
        }

        // 3. Smooth animated Thumb indicator (☀️ or 🌙)
        val thumbOffset = (availableWidth - indicatorSize) * clampedProgress
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(indicatorSize)
                .shadow(elevation = 6.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(
                    if (isSun) Brush.radialGradient(
                        colors = listOf(Color(0xFFFFF176), Color(0xFFFFB300))
                    ) else Brush.radialGradient(
                        colors = listOf(Color(0xFFFFFFFF), Color(0xFFCFD8DC))
                    )
                )
                .border(
                    width = 1.5.dp,
                    color = if (isSun) Color(0xFFFFE082) else Color(0xFFECEFF1),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSun) Icons.Default.WbSunny else Icons.Default.NightlightRound,
                contentDescription = if (isSun) "Sun indicator" else "Moon indicator",
                tint = if (isSun) Color(0xFFE65100) else Color(0xFF37474F),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Calculates cycle progress from rise time to set time for the given current minute of day.
 * Strictly guarantees a return value between 0.0f and 1.0f (never NaN, negative, or > 1.0f).
 * Gracefully handles midnight crossover (e.g., Moonrise at night, Moonset next morning).
 */
fun calculateCycleProgress(
    riseTimeStr: String?,
    setTimeStr: String?,
    currentMinuteOfDay: Int
): Float {
    val riseMinute = parseTimeToMinuteOfDay(riseTimeStr)
    val setMinute = parseTimeToMinuteOfDay(setTimeStr)

    if (riseMinute == null || setMinute == null) {
        return 0.5f
    }

    if (riseMinute == setMinute) {
        return 0.5f
    }

    val progress = if (setMinute > riseMinute) {
        // Normal cycle within same day (e.g. Sunrise 05:52 AM -> Sunset 05:39 PM)
        when {
            currentMinuteOfDay <= riseMinute -> 0.0f
            currentMinuteOfDay >= setMinute -> 1.0f
            else -> (currentMinuteOfDay - riseMinute).toFloat() / (setMinute - riseMinute).toFloat()
        }
    } else {
        // Midnight crossover cycle (e.g. Moonrise 08:30 PM (1230m) -> Moonset 06:15 AM (375m))
        val totalDuration = (1440 - riseMinute) + setMinute
        if (totalDuration <= 0) {
            0.5f
        } else if (currentMinuteOfDay >= riseMinute) {
            // Evening part before midnight
            (currentMinuteOfDay - riseMinute).toFloat() / totalDuration.toFloat()
        } else if (currentMinuteOfDay <= setMinute) {
            // Morning part after midnight
            ((1440 - riseMinute) + currentMinuteOfDay).toFloat() / totalDuration.toFloat()
        } else {
            // Between set and rise (object is below horizon)
            val midpoint = (setMinute + riseMinute) / 2
            if (currentMinuteOfDay < midpoint) 1.0f else 0.0f
        }
    }

    return if (progress.isNaN() || progress.isInfinite()) {
        0.5f
    } else {
        progress.coerceIn(0.0f, 1.0f)
    }
}

/**
 * Parses diverse time formats (12-hour AM/PM, 24-hour, ISO) into minute of day (0..1439).
 */
fun parseTimeToMinuteOfDay(timeStr: String?): Int? {
    if (timeStr.isNullOrBlank()) return null
    val clean = timeStr.trim()

    // 1. 12-hour AM/PM format: e.g. "05:52 AM", "5:52 PM", "05:52am"
    val regex12 = Regex("""^(\d{1,2}):(\d{2})\s*([AaPp][Mm])$""")
    val match12 = regex12.find(clean)
    if (match12 != null) {
        var hour = match12.groupValues[1].toIntOrNull() ?: return null
        val minute = match12.groupValues[2].toIntOrNull() ?: return null
        val ampm = match12.groupValues[3].uppercase()
        if (ampm == "PM" && hour < 12) hour += 12
        if (ampm == "AM" && hour == 12) hour = 0
        return (hour * 60 + minute).coerceIn(0, 1439)
    }

    // 2. 24-hour format: e.g. "17:39", "05:52"
    val regex24 = Regex("""^(\d{1,2}):(\d{2})(?::\d{2})?$""")
    val match24 = regex24.find(clean)
    if (match24 != null) {
        val hour = match24.groupValues[1].toIntOrNull() ?: return null
        val minute = match24.groupValues[2].toIntOrNull() ?: return null
        return (hour * 60 + minute).coerceIn(0, 1439)
    }

    // 3. ISO format: e.g. "2026-10-07T05:52:00"
    if (clean.contains("T")) {
        val timePart = clean.substringAfter("T").substringBefore("Z").substringBefore("+")
        val matchIso = regex24.find(timePart)
        if (matchIso != null) {
            val hour = matchIso.groupValues[1].toIntOrNull() ?: return null
            val minute = matchIso.groupValues[2].toIntOrNull() ?: return null
            return (hour * 60 + minute).coerceIn(0, 1439)
        }
    }

    return null
}

private fun getDaylightSummary(riseMinute: Int?, setMinute: Int?, currentMinute: Int): String {
    if (riseMinute == null || setMinute == null) return ""
    return if (currentMinute in riseMinute..setMinute) {
        val remaining = setMinute - currentMinute
        val hours = remaining / 60
        val mins = remaining % 60
        val timeStr = if (hours > 0) {
            "${BanglaUtils.toBanglaDigits(hours)} ঘণ্টা ${BanglaUtils.toBanglaDigits(mins)} মিনিট"
        } else {
            "${BanglaUtils.toBanglaDigits(mins)} মিনিট"
        }
        "সূর্যাস্তের বাকি $timeStr"
    } else if (currentMinute < riseMinute) {
        val remaining = riseMinute - currentMinute
        val hours = remaining / 60
        val mins = remaining % 60
        val timeStr = if (hours > 0) {
            "${BanglaUtils.toBanglaDigits(hours)} ঘণ্টা ${BanglaUtils.toBanglaDigits(mins)} মিনিট"
        } else {
            "${BanglaUtils.toBanglaDigits(mins)} মিনিট"
        }
        "সূর্যোদয়ের বাকি $timeStr"
    } else {
        "আজকের সূর্যাস্ত সম্পন্ন হয়েছে"
    }
}

private fun getMoonSummary(
    riseMinute: Int?,
    setMinute: Int?,
    currentMinute: Int,
    moonPhaseBn: String?
): String {
    if (riseMinute == null || setMinute == null) return moonPhaseBn.orEmpty()
    val isVisible = if (setMinute > riseMinute) {
        currentMinute in riseMinute..setMinute
    } else {
        currentMinute >= riseMinute || currentMinute <= setMinute
    }
    return if (isVisible) {
        "চাঁদ বর্তমানে আকাশে বিদ্যমান"
    } else {
        "চাঁদ দিগন্তের নিচে রয়েছে"
    }
}
