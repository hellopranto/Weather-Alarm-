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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
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
import com.example.data.model.WeatherAlertModel

@Composable
fun WeatherAlertsSection(
    warnings: List<WeatherAlertModel>,
    onNavigateToAlerts: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onNavigateToAlerts() }
            .testTag("weather_alerts_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (warnings.isNotEmpty()) Color(0x40E65100) else Color(0x33102A4E)
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
                        imageVector = if (warnings.isNotEmpty()) Icons.Default.Warning else Icons.Default.Security,
                        contentDescription = "Alerts",
                        tint = if (warnings.isNotEmpty()) Color(0xFFFFB74D) else Color(0xFF81C784),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "আবহাওয়া সতর্কতা ও বুলেটিন",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (warnings.isEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF81C784),
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "বর্তমানে কোনো সক্রিয় আবহাওয়া সতর্কতা নেই।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            } else {
                warnings.forEach { warning ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = warning.titleBn ?: warning.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFCC80)
                        )
                        if (!warning.area.isNullOrBlank() || warning.regions.isNotEmpty()) {
                            val regionsText = warning.area ?: warning.regions.joinToString(", ")
                            Text(
                                text = "প্রভাবিত এলাকা: $regionsText",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF81D4FA),
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = warning.descriptionBn ?: warning.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 3
                        )
                        if (!warning.instructions.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "পরামর্শ: ${warning.instructions}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFD54F),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
