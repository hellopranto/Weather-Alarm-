package com.example.ui.stations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BmdStationDto
import com.example.location.BangladeshCity
import com.example.ui.components.formatStationName
import com.example.util.BanglaUtils

@Composable
fun BmdStationsScreen(
    viewModel: BmdStationsViewModel,
    onSelectStationForHome: (BangladeshCity) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val divisions = listOf("সকল", "ঢাকা", "চট্টগ্রাম", "সিলেট", "রাজশাহী", "খুলনা", "বরিশাল", "রংপুর", "ময়মনসিংহ")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
            .testTag("bmd_stations_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = null,
                        tint = Color(0xFF81D4FA),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "BMD আবহাওয়া পর্যবেক্ষণ কেন্দ্র",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = "সারাদেশের ${BanglaUtils.toBanglaDigits(uiState.stations.size)}টি সরকারি আবহাওয়া স্টেশন পর্যবেক্ষণ",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }

            IconButton(
                onClick = { viewModel.loadStations() },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x2BFFFFFF))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("স্টেশন বা বিভাগ খুঁজুন...", color = Color.White.copy(alpha = 0.6f)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF81D4FA)) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF81D4FA),
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                cursorColor = Color(0xFF81D4FA)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("station_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Division Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            divisions.forEach { div ->
                val isSelected = uiState.selectedDivision == div
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectDivision(div) },
                    label = {
                        Text(
                            text = div,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF81D4FA),
                        selectedLabelColor = Color(0xFF0D3268),
                        containerColor = Color(0x24FFFFFF),
                        labelColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Content
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = Color.White)
                    Text("BMD স্টেশন পর্যবেক্ষণ তথ্য লোড হচ্ছে...", color = Color.White)
                }
            }
        } else if (uiState.error != null && uiState.stations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = uiState.error ?: "ত্রুটি হয়েছে", color = Color.White)
                    Button(
                        onClick = { viewModel.loadStations() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81D4FA))
                    ) {
                        Text("পুনরায় চেষ্টা করুন", color = Color(0xFF0D3268), fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Nationwide summary banner
                if (uiState.stations.isNotEmpty()) {
                    item {
                        NationwideSummaryCard(stations = uiState.stations)
                    }
                }

                if (uiState.filteredStations.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("কোনো স্টেশন পাওয়া যায়নি", color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                } else {
                    items(uiState.filteredStations, key = { it.stationId }) { station ->
                        BmdStationCard(
                            station = station,
                            onSelectForHome = {
                                val bnName = formatStationName(station.stationName)
                                onSelectStationForHome(
                                    BangladeshCity(
                                        nameEn = station.stationName,
                                        nameBn = bnName,
                                        districtEn = station.stationName,
                                        districtBn = BmdStationsViewModel.translateDivision(station.division),
                                        latitude = station.latitude,
                                        longitude = station.longitude
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NationwideSummaryCard(stations: List<BmdStationDto>) {
    val maxRainStation = stations.maxByOrNull { it.rainfall24hMm }
    val maxTempStation = stations.maxByOrNull { it.temperatureC }
    val minTempStation = stations.minByOrNull { it.temperatureC }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x33102A4E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "সারাদেশের আজকের আবহাওয়া একনজরে",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF81D4FA)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (maxRainStation != null) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "সর্বোচ্চ বৃষ্টিপাত",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        val rainBn = formatStationName(maxRainStation.stationName)
                        Text(
                            text = "$rainBn (${BanglaUtils.toBanglaDigits(maxRainStation.rainfall24hMm)} মিমি)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64B5F6)
                        )
                    }
                }

                if (maxTempStation != null) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text(
                            text = "সর্বোচ্চ তাপমাত্রা",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        val tempBn = formatStationName(maxTempStation.stationName)
                        Text(
                            text = "$tempBn (${BanglaUtils.toBanglaDigits(maxTempStation.temperatureC)}°C)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BmdStationCard(
    station: BmdStationDto,
    onSelectForHome: () -> Unit
) {
    val bnName = formatStationName(station.stationName)
    val divBn = BmdStationsViewModel.translateDivision(station.division)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x33102A4E)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectForHome() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top row: Name & Division badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = bnName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "(${station.stationName})",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = "স্টেশন আইডি: ${station.stationId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF81D4FA),
                        fontSize = 10.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x33FFFFFF))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$divBn বিভাগ",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metrics row: Temp | 24h Rain | Humidity | Pressure
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Temperature
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Thermostat, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(16.dp))
                        Text(
                            text = "${BanglaUtils.toBanglaDigits(station.temperatureC)}°C",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text("তাপমাত্রা", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                }

                // 24h Rainfall
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF64B5F6), modifier = Modifier.size(16.dp))
                        Text(
                            text = "${BanglaUtils.toBanglaDigits(station.rainfall24hMm)} মিমি",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF81D4FA)
                        )
                    }
                    Text("২৪ ঘণ্টার বৃষ্টি", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                }

                // Humidity
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.InvertColors, contentDescription = null, tint = Color(0xFF80DEEA), modifier = Modifier.size(16.dp))
                        Text(
                            text = "${BanglaUtils.toBanglaDigits(station.humidityPercent)}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text("আর্দ্রতা", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                }

                // Pressure
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Compress, contentDescription = null, tint = Color(0xFFA5D6A7), modifier = Modifier.size(16.dp))
                        Text(
                            text = "${BanglaUtils.toBanglaDigits(station.pressureHpa.toInt())}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text("চাপ (hPa)", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom action: View Weather
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "বিস্তারিত আবহাওয়া দেখুন",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF81D4FA),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF81D4FA),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
