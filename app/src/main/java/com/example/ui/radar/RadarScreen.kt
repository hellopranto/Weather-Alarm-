package com.example.ui.radar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.ui.theme.WeatherRadarRainHeavy
import com.example.ui.theme.WeatherRadarRainLight
import com.example.ui.theme.WeatherRadarRainMod
import com.example.ui.theme.WeatherRadarRainSevere
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

@Composable
fun RadarScreen(
    viewModel: RadarViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F1A24))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.75f, 3.0f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
            .testTag("radar_screen")
    ) {
        // Base Bangladesh Map & Rivers Canvas
        BangladeshMapBackground(
            userLat = uiState.userLat,
            userLon = uiState.userLon,
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY
        )

        // RainViewer Doppler Radar Tile Overlay
        if (uiState.currentFrame != null) {
            val zoom = uiState.zoomLevel
            // Calculate web mercator tile (x, y) for Bangladesh center
            val tileX = lonToTileX(uiState.mapCenterLon, zoom)
            val tileY = latToTileY(uiState.mapCenterLat, zoom)
            val tileUrl = viewModel.getRadarTileUrl(zoom, tileX, tileY)

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(tileUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "RainViewer Radar Overlay",
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("radar_tile_image"),
                contentScale = ContentScale.Fit,
                alpha = 0.85f
            )
        }

        // Top Status & Frame Timestamp Bar
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
                .testTag("radar_top_bar"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.radar_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.radar_source_rainviewer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = uiState.formattedTime,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Floating Zoom Controls on Right Side
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    viewModel.zoomIn()
                    scale = (scale * 1.2f).coerceAtMost(3.0f)
                },
                modifier = Modifier
                    .size(44.dp)
                    .testTag("btn_radar_zoom_in"),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In")
            }
            FloatingActionButton(
                onClick = {
                    viewModel.zoomOut()
                    scale = (scale / 1.2f).coerceAtLeast(0.75f)
                },
                modifier = Modifier
                    .size(44.dp)
                    .testTag("btn_radar_zoom_out"),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
            }
            FloatingActionButton(
                onClick = {
                    offsetX = 0f
                    offsetY = 0f
                    scale = 1f
                    viewModel.centerOnUserLocation()
                },
                modifier = Modifier
                    .size(44.dp)
                    .testTag("btn_radar_recenter"),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = "Center on location",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        // Bottom Controls Container (Playback, Timeline, Legend)
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth()
                .testTag("radar_bottom_controls"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Precipitation Intensity Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.radar_legend_title),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendPill(color = WeatherRadarRainLight, label = stringResource(R.string.radar_light))
                        LegendPill(color = WeatherRadarRainMod, label = stringResource(R.string.radar_moderate))
                        LegendPill(color = WeatherRadarRainHeavy, label = stringResource(R.string.radar_heavy))
                        LegendPill(color = WeatherRadarRainSevere, label = stringResource(R.string.radar_extreme))
                    }
                }

                // Timeline Scrubbing Slider
                val framesCount = uiState.frames.size
                if (framesCount > 1) {
                    Slider(
                        value = uiState.currentFrameIndex.toFloat(),
                        onValueChange = { viewModel.selectFrameIndex(it.toInt()) },
                        valueRange = 0f..(framesCount - 1).toFloat(),
                        steps = if (framesCount > 2) framesCount - 2 else 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("radar_timeline_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Playback Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.stepBack() },
                        modifier = Modifier.testTag("btn_radar_step_back")
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = stringResource(R.string.radar_step_back))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    FloatingActionButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("btn_radar_play_pause"),
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (uiState.isPlaying) stringResource(R.string.radar_pause) else stringResource(R.string.radar_play),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(
                        onClick = { viewModel.stepForward() },
                        modifier = Modifier.testTag("btn_radar_step_forward")
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = stringResource(R.string.radar_step_forward))
                    }
                }
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun LegendPill(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun BangladeshMapBackground(
    userLat: Double,
    userLon: Double,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2 + offsetX
        val centerY = size.height / 2 + offsetY

        // Draw Bay of Bengal in the south
        drawRect(
            color = Color(0xFF0C243B),
            topLeft = Offset(0f, centerY + 180f * scale),
            size = androidx.compose.ui.geometry.Size(size.width, size.height)
        )

        // Draw Delta boundary contour simulation
        val deltaColor = Color(0xFF1B2F40)
        drawCircle(
            color = deltaColor,
            radius = 280f * scale,
            center = Offset(centerX, centerY)
        )

        // Draw Padma, Jamuna, Meghna major river flow approximations
        val riverColor = Color(0xFF2E5B82)
        drawLine(
            color = riverColor,
            start = Offset(centerX - 100f * scale, centerY - 250f * scale),
            end = Offset(centerX, centerY - 30f * scale),
            strokeWidth = 5f * scale
        )
        drawLine(
            color = riverColor,
            start = Offset(centerX + 120f * scale, centerY - 200f * scale),
            end = Offset(centerX, centerY - 30f * scale),
            strokeWidth = 4f * scale
        )
        drawLine(
            color = riverColor,
            start = Offset(centerX, centerY - 30f * scale),
            end = Offset(centerX + 40f * scale, centerY + 220f * scale),
            strokeWidth = 7f * scale
        )

        // Draw user location pulsing marker
        drawCircle(
            color = Color(0x5500E5FF),
            radius = 22f * scale,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = Color(0xFF00E5FF),
            radius = 8f * scale,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = Color.White,
            radius = 3.5f * scale,
            center = Offset(centerX, centerY)
        )
    }
}

// Convert geographic longitude to Web Mercator tile X coordinate
fun lonToTileX(lon: Double, zoom: Int): Int {
    return floor((lon + 180.0) / 360.0 * (1 shl zoom)).toInt()
}

// Convert geographic latitude to Web Mercator tile Y coordinate
fun latToTileY(lat: Double, zoom: Int): Int {
    val latRad = Math.toRadians(lat)
    return floor((1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * (1 shl zoom)).toInt()
}
