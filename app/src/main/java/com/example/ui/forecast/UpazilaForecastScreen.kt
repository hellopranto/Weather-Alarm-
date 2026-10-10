package com.example.ui.forecast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UpazilaBbsRecord
import com.example.data.model.UpazilaDailyForecastItem
import com.example.data.model.UpazilaSubDailyForecastItem
import com.example.data.model.UpazilaUiForecast
import com.example.util.BanglaUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpazilaForecastScreen(
    viewModel: UpazilaForecastViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedSource by viewModel.selectedSource.collectAsState()
    val currentUpazila by viewModel.currentUpazila.collectAsState()
    val allUpazilas by viewModel.allUpazilas.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var showPicker by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D253F),
                        Color(0xFF0A1B36),
                        Color(0xFF040D1A)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header: Title + Upazila Selector + GPS button
            Spacer(modifier = Modifier.height(14.dp))
            UpazilaHeaderSection(
                upazila = currentUpazila,
                isRefreshing = isRefreshing,
                onOpenPicker = { showPicker = true },
                onUseGps = { viewModel.useCurrentGpsLocation() },
                onRefresh = { viewModel.refresh() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Forecast Source Selector Tabs (BMDWRF, ECMWF, RIMESWRF)
            ForecastSourceSelector(
                currentSource = selectedSource,
                onSelectSource = { viewModel.setSource(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Main Content Area
            when (val state = uiState) {
                is UpazilaForecastUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF81D4FA),
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = "উপজেলা ভিত্তিক আবহাওয়া পূর্বাভাস লোড হচ্ছে...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                is UpazilaForecastUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                                .background(Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { viewModel.refresh() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF81D4FA),
                                    contentColor = Color(0xFF0D3268)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("পুনরায় চেষ্টা করুন", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                is UpazilaForecastUiState.Success -> {
                    UpazilaForecastContent(
                        forecast = state.forecast,
                        isCached = state.isCached,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Upazila Picker Bottom Sheet
        if (showPicker) {
            UpazilaPickerBottomSheet(
                upazilas = allUpazilas,
                currentPcode = currentUpazila?.pcode ?: "",
                onSelect = {
                    viewModel.selectUpazila(it)
                    showPicker = false
                },
                onDismiss = { showPicker = false }
            )
        }
    }
}

@Composable
private fun UpazilaHeaderSection(
    upazila: UpazilaBbsRecord?,
    isRefreshing: Boolean,
    onOpenPicker: () -> Unit,
    onUseGps: () -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x331E3A5F)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenPicker() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = Color(0xFF81D4FA),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = upazila?.name ?: "উপজেলা নির্বাচন করুন",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                val subtitle = if (upazila != null) {
                    "${upazila.districtBn ?: upazila.district} জেলা, ${upazila.divisionBn ?: upazila.division} বিভাগ (PCODE: ${upazila.pcode})"
                } else {
                    "ট্যাপ করে তালিকা থেকে উপজেলা বেছে নিন"
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 24.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onUseGps,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Live GPS",
                        tint = Color(0xFF81D4FA),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = if (isRefreshing) Color(0xFFFFB300) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ForecastSourceSelector(
    currentSource: String,
    onSelectSource: (String) -> Unit
) {
    val sources = listOf("BMDWRF", "ECMWF", "RIMESWRF")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sources.forEach { src ->
            val isSelected = currentSource == src
            FilterChip(
                selected = isSelected,
                onClick = { onSelectSource(src) },
                label = {
                    Text(
                        text = src,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF0288D1),
                    selectedLabelColor = Color.White,
                    containerColor = Color(0x22FFFFFF),
                    labelColor = Color.White.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
private fun UpazilaForecastContent(
    forecast: UpazilaUiForecast,
    isCached: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Source info & Last Updated Chip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "মডেল: ${forecast.source}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF81D4FA),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = forecast.updatedAtFormattedBn,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        // Severe Alerts if any
        if (forecast.alerts.isNotEmpty()) {
            items(forecast.alerts) { alert ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (alert.isSevere) Color(0x44D32F2F) else Color(0x44F57C00)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (alert.isSevere) Color(0xFFFF5252) else Color(0xFFFFD54F)
                        )
                        Column {
                            Text(
                                text = alert.titleBn,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = alert.descriptionBn,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }

        // Sub-daily 3-Hour Steps (Horizontally scrollable)
        if (forecast.subDailySteps.isNotEmpty()) {
            item {
                Text(
                    text = "৪ দিনের ৩-ঘণ্টাভিত্তিক ধাপসমূহ (Steps)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(forecast.subDailySteps) { step ->
                        SubDailyStepCard(step = step)
                    }
                }
            }
        }

        // 10-Day Daily Forecast Section
        item {
            Text(
                text = "দৈনিক বিশদ পূর্বাভাস (${BanglaUtils.toBanglaDigits(forecast.dailySteps.size)} দিন)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        items(forecast.dailySteps) { day ->
            UpazilaDailyCard(day = day)
        }
    }
}

@Composable
private fun SubDailyStepCard(step: UpazilaSubDailyForecastItem) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x281B4B82)),
        modifier = Modifier.width(130.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = step.timeLabelBn,
                fontSize = 11.sp,
                color = Color(0xFF81D4FA),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${BanglaUtils.toBanglaDigits(step.temp?.toInt())}°C",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = Color(0xFF4FC3F7),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "${BanglaUtils.toBanglaDigits(step.rainfallMm ?: 0.0)} মিমি",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            if (step.humidityPercent != null) {
                Text(
                    text = "আর্দ্রতা: ${BanglaUtils.toBanglaDigits(step.humidityPercent.toInt())}%",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun UpazilaDailyCard(day: UpazilaDailyForecastItem) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x2514335A)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = day.dateLabelBn,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "সর্বোচ্চ ${BanglaUtils.toBanglaDigits(day.tempMax?.toInt())}°C",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF8A80)
                    )
                    Text(
                        text = "সর্বনিম্ন ${BanglaUtils.toBanglaDigits(day.tempMin?.toInt())}°C",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF80D8FF)
                    )
                }
            }

            // Key forecast metrics in row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Rainfall in mm
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = "Rainfall",
                        tint = Color(0xFF4FC3F7),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "বৃষ্টি: ${BanglaUtils.toBanglaDigits(day.rainfallMm ?: 0.0)} মিমি",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                // Humidity in %
                if (day.humidityPercent != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Air,
                            contentDescription = "Humidity",
                            tint = Color(0xFF81D4FA),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "আর্দ্রতা: ${BanglaUtils.toBanglaDigits(day.humidityPercent.toInt())}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                // Wind speed in km/h
                if (day.windSpeedKmh != null) {
                    Text(
                        text = "বাতাস: ${BanglaUtils.toBanglaDigits(day.windSpeedKmh.toInt())} কিমি/ঘ",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            // Secondary row: Cloud Cover and Gust
            if (day.cloudCoverPercent != null || day.windGustKmh != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (day.cloudCoverPercent != null) {
                        Text(
                            text = "মেঘের আচ্ছাদন: ${BanglaUtils.toBanglaDigits(day.cloudCoverPercent.toInt())}%",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                    if (day.windGustKmh != null && day.windGustKmh > 0.0) {
                        Text(
                            text = "দমকা: ${BanglaUtils.toBanglaDigits(day.windGustKmh.toInt())} কিমি/ঘ",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpazilaPickerBottomSheet(
    upazilas: List<UpazilaBbsRecord>,
    currentPcode: String,
    onSelect: (UpazilaBbsRecord) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }

    val filtered = remember(query, upazilas) {
        if (query.isBlank()) {
            upazilas
        } else {
            val q = query.trim().lowercase()
            upazilas.filter {
                it.name.lowercase().contains(q) ||
                it.district.lowercase().contains(q) ||
                it.division.lowercase().contains(q) ||
                it.pcode.contains(q) ||
                (it.districtBn != null && it.districtBn.contains(q)) ||
                (it.divisionBn != null && it.divisionBn.contains(q))
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0D253F)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "উপজেলা নির্বাচন করুন (${BanglaUtils.toBanglaDigits(upazilas.size)} টি)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("উপজেলা, জেলা বা PCODE দিয়ে খুঁজুন...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF81D4FA)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF81D4FA),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered) { item ->
                    val isSelected = item.pcode == currentPcode
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(item) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF0288D1) else Color(0x22FFFFFF)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = item.name,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "${item.districtBn ?: item.district} জেলা, ${item.divisionBn ?: item.division}",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                            Text(
                                text = "PCODE: ${item.pcode}",
                                fontSize = 11.sp,
                                color = Color(0xFF81D4FA)
                            )
                        }
                    }
                }
            }
        }
    }
}
