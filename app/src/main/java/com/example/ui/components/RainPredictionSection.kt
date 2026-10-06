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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.data.model.RainPredictionModel
import com.example.util.BanglaUtils

@Composable
fun RainPredictionSection(
    rainPrediction: RainPredictionModel?,
    modifier: Modifier = Modifier
) {
    if (rainPrediction == null) return
    if (rainPrediction.summaryBn.isNullOrBlank() && rainPrediction.expectedNextHours.isNullOrBlank() && rainPrediction.rainProbability == null) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("rain_prediction_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x33102A4E))
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
                        imageVector = Icons.Default.Umbrella,
                        contentDescription = "Rain Prediction",
                        tint = Color(0xFF81D4FA),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "বৃষ্টিপাতের পূর্বাভাস বুলেটিন",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!rainPrediction.summaryBn.isNullOrBlank()) {
                Text(
                    text = rainPrediction.summaryBn,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.95f),
                    lineHeight = 22.sp
                )
            }

            if (!rainPrediction.expectedNextHours.isNullOrBlank() || rainPrediction.rainProbability != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!rainPrediction.expectedNextHours.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = Color(0xFF64B5F6),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = BanglaUtils.toBanglaDigits(rainPrediction.expectedNextHours),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF81D4FA)
                            )
                        }
                    }

                    if (rainPrediction.rainProbability != null) {
                        Text(
                            text = "সম্ভাবনা: ${BanglaUtils.toBanglaDigits(rainPrediction.rainProbability)}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFCC80)
                        )
                    }
                }
            }
        }
    }
}
