package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Modifier extension to apply a fluid shimmer animation to placeholders.
 */
fun Modifier.weatherShimmer(
    shape: Shape = RoundedCornerShape(12.dp),
    baseColor: Color = Color(0x1FFFFFFF),
    highlightColor: Color = Color(0x4DFFFFFF)
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "weatherShimmerTransition")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "weatherShimmerTranslate"
    )

    val shimmerColors = listOf(
        baseColor,
        highlightColor,
        baseColor
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(x = translateAnimation - 350f, y = translateAnimation - 350f),
        end = Offset(x = translateAnimation, y = translateAnimation)
    )

    this
        .clip(shape)
        .background(brush = brush, shape = shape)
}

/**
 * Basic shimmer placeholder block.
 */
@Composable
fun ShimmerBlock(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    val sizeModifier = if (width != null) {
        modifier
            .width(width)
            .height(height)
    } else {
        modifier
            .fillMaxWidth()
            .height(height)
    }

    Box(
        modifier = sizeModifier.weatherShimmer(shape = shape)
    )
}

/**
 * Full weather loading skeleton that mimics the exact dashboard layout:
 * - Header (Location, Date, Action buttons placeholder)
 * - Hero Card (Icon, Large Temperature, Condition, Min/Max pill)
 * - Hourly Forecast Horizontal Scroll skeleton
 * - Weather Metrics 2x Grid cards
 */
@Composable
fun WeatherLoadingSkeleton(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("weather_loading_skeleton"),
        contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = false
    ) {
        // 1. Header Skeleton
        item {
            HeaderSkeleton()
        }

        // 2. Weather Hero Card Skeleton (Big Temperature, weather icon, condition, feels like)
        item {
            WeatherHeroSkeletonCard()
        }

        // 3. Hourly Forecast Scroll Skeleton
        item {
            HourlyScrollSkeleton()
        }

        // 4. Metrics Grid 2-column Skeleton (Wind, Pressure, Rain, UV, Humidity, Visibility)
        item {
            MetricsGridSkeleton()
        }

        // 5. Additional Section Skeleton (e.g. Air Quality / Sun & Moon placeholder)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("skeleton_subcard"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x33102A4E))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShimmerBlock(width = 140.dp, height = 22.dp, shape = RoundedCornerShape(8.dp))
                        ShimmerBlock(width = 60.dp, height = 22.dp, shape = RoundedCornerShape(12.dp))
                    }
                    ShimmerBlock(height = 54.dp, shape = RoundedCornerShape(14.dp))
                }
            }
        }
    }
}

@Composable
private fun HeaderSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Location picker pill
            ShimmerBlock(
                width = 44.dp,
                height = 44.dp,
                shape = CircleShape
            )

            // Centered Location & Date
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ShimmerBlock(
                    width = 160.dp,
                    height = 24.dp,
                    shape = RoundedCornerShape(8.dp)
                )
                ShimmerBlock(
                    width = 110.dp,
                    height = 16.dp,
                    shape = RoundedCornerShape(6.dp)
                )
            }

            // Refresh button circle
            ShimmerBlock(
                width = 42.dp,
                height = 42.dp,
                shape = CircleShape
            )
        }

        // GPS Quick pill in center
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            ShimmerBlock(
                width = 170.dp,
                height = 32.dp,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
private fun WeatherHeroSkeletonCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("skeleton_hero_card"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x33102A4E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Weather Icon Circle
            ShimmerBlock(
                width = 72.dp,
                height = 72.dp,
                shape = CircleShape
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Temperature Text Placeholder (large)
            ShimmerBlock(
                width = 130.dp,
                height = 68.dp,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Feels like text placeholder
            ShimmerBlock(
                width = 110.dp,
                height = 20.dp,
                shape = RoundedCornerShape(6.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Condition subtitle placeholder
            ShimmerBlock(
                width = 150.dp,
                height = 26.dp,
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Min / Max pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBlock(
                    width = 80.dp,
                    height = 26.dp,
                    shape = RoundedCornerShape(13.dp)
                )
                ShimmerBlock(
                    width = 80.dp,
                    height = 26.dp,
                    shape = RoundedCornerShape(13.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Station badge placeholder
            ShimmerBlock(
                width = 200.dp,
                height = 28.dp,
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}

@Composable
private fun HourlyScrollSkeleton() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("skeleton_hourly_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x33102A4E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp)
        ) {
            // Header Row of Hourly Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ShimmerBlock(
                    width = 34.dp,
                    height = 34.dp,
                    shape = RoundedCornerShape(8.dp)
                )
                ShimmerBlock(
                    width = 150.dp,
                    height = 20.dp,
                    shape = RoundedCornerShape(6.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Horizontal Hourly Item Cards
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 18.dp),
                userScrollEnabled = false
            ) {
                items(6) {
                    HourlyItemSkeleton()
                }
            }
        }
    }
}

@Composable
private fun HourlyItemSkeleton() {
    Box(
        modifier = Modifier
            .width(68.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x2BFFFFFF))
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Time text
            ShimmerBlock(width = 38.dp, height = 12.dp, shape = RoundedCornerShape(4.dp))
            // Condition Icon
            ShimmerBlock(width = 30.dp, height = 30.dp, shape = CircleShape)
            // Temp text
            ShimmerBlock(width = 36.dp, height = 16.dp, shape = RoundedCornerShape(4.dp))
            // Rain pill
            ShimmerBlock(width = 44.dp, height = 14.dp, shape = RoundedCornerShape(6.dp))
        }
    }
}

@Composable
private fun MetricsGridSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("skeleton_metrics_grid"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShimmerBlock(width = 24.dp, height = 24.dp, shape = CircleShape)
            ShimmerBlock(width = 130.dp, height = 18.dp, shape = RoundedCornerShape(6.dp))
        }

        // Grid Rows
        repeat(3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCardSkeleton(modifier = Modifier.weight(1f))
                MetricCardSkeleton(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricCardSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x33102A4E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBlock(width = 65.dp, height = 14.dp, shape = RoundedCornerShape(4.dp))
                ShimmerBlock(width = 18.dp, height = 18.dp, shape = CircleShape)
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Main value
            ShimmerBlock(width = 90.dp, height = 26.dp, shape = RoundedCornerShape(6.dp))

            // Subtitle / indicator
            ShimmerBlock(width = 75.dp, height = 12.dp, shape = RoundedCornerShape(4.dp))

            // Progress bar
            ShimmerBlock(height = 4.dp, shape = RoundedCornerShape(2.dp))
        }
    }
}
