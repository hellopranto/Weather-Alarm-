package com.example.ui.radar

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.util.BanglaUtils

@Composable
fun RadarScreen(
    viewModel: RadarViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("radar_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ডপলার বৃষ্টিপাত রাডার",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "RainViewer লাইভ রাডার পর্যবেক্ষণ",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }

            IconButton(
                onClick = { viewModel.loadRadar() },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x2BFFFFFF))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else if (uiState.error != null && uiState.frames.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = uiState.error ?: "রাডার লোড করা যায়নি", color = Color.White)
                    Button(
                        onClick = { viewModel.loadRadar() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81D4FA))
                    ) {
                        Text("পুনরায় চেষ্টা করুন", color = Color(0xFF0D3268), fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x33102A4E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val frame = uiState.currentFrame
                    val tileUrl = frame?.let {
                        val host = uiState.host.removeSuffix("/")
                        val path = if (it.path.startsWith("/")) it.path else "/${it.path}"
                        // Level 6 tile for Bangladesh
                        "$host$path/256/6/48/27/2/1_1.png"
                    }

                    if (tileUrl != null) {
                        AsyncImage(
                            model = tileUrl,
                            contentDescription = "Radar Frame",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Top timestamp overlay badge
                    Box(
                        modifier = Modifier
                            .padding(14.dp)
                            .align(Alignment.TopEnd)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xAA000000))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = BanglaUtils.formatHourBengali(uiState.formattedTime),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Playback controls
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x33102A4E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { viewModel.togglePlayPause() },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF81D4FA))
                        ) {
                            Icon(
                                imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                                tint = Color(0xFF0D3268)
                            )
                        }

                        Text(
                            text = "ফ্রেম: ${BanglaUtils.toBanglaDigits(uiState.currentFrameIndex + 1)} / ${BanglaUtils.toBanglaDigits(uiState.frames.size)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }

                    if (uiState.frames.size > 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = uiState.currentFrameIndex.toFloat(),
                            onValueChange = { viewModel.selectFrame(it.toInt()) },
                            valueRange = 0f..(uiState.frames.size - 1).toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF81D4FA),
                                activeTrackColor = Color(0xFF81D4FA),
                                inactiveTrackColor = Color(0x33FFFFFF)
                            )
                        )
                    }
                }
            }
        }
    }
}
