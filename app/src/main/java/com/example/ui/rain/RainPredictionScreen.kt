package com.example.ui.rain

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.DailyRainModel
import com.example.data.model.HourlyRainModel
import com.example.data.model.RainPredictionResponse
import com.example.data.model.RainTimelinePointModel
import com.example.ui.components.ErrorStateView
import com.example.util.BanglaUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RainPredictionScreen(
    viewModel: RainPredictionViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.rain_prediction_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        val locSubtitle = (uiState as? RainUiState.Success)?.cityName?.ifBlank { "বাংলাদেশ" } ?: "বাংলাদেশ"
                        Text(
                            text = locSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_rain_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_close),
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadRainPrediction(forceRefresh = true) },
                        modifier = Modifier.testTag("btn_refresh_rain")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A192F)
                )
            )
        },
        containerColor = Color(0xFF0A192F),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.loadRainPrediction(forceRefresh = true) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is RainUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF81D4FA))
                    }
                }
                is RainUiState.Error -> {
                    ErrorStateView(
                        message = state.message,
                        onRetry = { viewModel.loadRainPrediction(forceRefresh = true) }
                    )
                }
                is RainUiState.Success -> {
                    RainPredictionContent(data = state.data)
                }
            }
        }
    }
}

@Composable
private fun RainPredictionContent(data: RainPredictionResponse) {
    val rain = data.rain
    val metrics = data.prediction
    val radar = data.radar
    val heavyWarning = data.heavyRainWarning
    val timeline = data.timeline

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("rain_prediction_content_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Location & Confidence Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = Color(0xFF81D4FA),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = data.location.name.ifBlank { "বর্তমান অবস্থান" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                data.confidence?.let { conf ->
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
        }

        // 2. Heavy Rain Warning Card (if active)
        if (heavyWarning != null && heavyWarning.isWarningActive) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("heavy_rain_warning_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x44D32F2F)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "⚠️ ভারী বৃষ্টির সতর্কতা",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF8A80)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "আপনার এলাকায় ${heavyWarning.expectedStart ?: "নিকটবর্তী সময়ে"} ভারী থেকে অতি ভারী বৃষ্টিপাতের পূর্বাভাস রয়েছে।",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.95f),
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = "সম্ভাব্য পরিমাণ: ${heavyWarning.expectedRainfallAmount ?: "১০+ মিমি"}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFFD54F)
                            )
                            Text(
                                text = "স্থায়িত্ব: ${heavyWarning.expectedDuration ?: "১–৩ ঘণ্টা"}",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // 3. Rain ETA, Intensity & Duration Hero Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rain_hero_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (rain?.expected == true) Color(0x330091EA) else Color(0x22102A4E)
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x4481D4FA), Color(0x111E88E5))
                    )
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "বৃষ্টি শুরু হতে পারে",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            val etaText = when {
                                rain?.startInMinutes == 0 -> "এখনই বৃষ্টি হচ্ছে"
                                rain?.startInMinutes != null -> "প্রায় ${BanglaUtils.toBanglaDigits(rain.startInMinutes)} মিনিটের মধ্যে"
                                else -> "আগামী কয়েক ঘণ্টায় নেই"
                            }
                            Text(
                                text = etaText,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (rain?.expected == true) Color(0xFF80D8FF) else Color.White
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "তীব্রতা",
                                fontSize = 12.sp,
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
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = intensityColor
                            )
                        }
                    }

                    if (rain?.durationMinutes != null && rain.durationMinutes > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Color(0xFF81D4FA),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "সম্ভাব্য স্থায়িত্ব: প্রায় ${BanglaUtils.toBanglaDigits(rain.durationMinutes)} মিনিট",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }

                    if (!rain?.summaryBn.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = rain?.summaryBn.orEmpty(),
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // 4. Immediate Probability Grid (15m, 30m, 1h, 3h, 6h, 24h)
        item {
            Column {
                Text(
                    text = "🌧️ তাত্ক্ষণিক বৃষ্টির সম্ভাবনা",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScreenProbChip(
                        timeLabel = "১৫ মিনিট",
                        prob = metrics?.next15Minutes ?: 0,
                        modifier = Modifier.weight(1f)
                    )
                    ScreenProbChip(
                        timeLabel = "৩০ মিনিট",
                        prob = metrics?.next30Minutes ?: 0,
                        modifier = Modifier.weight(1f)
                    )
                    ScreenProbChip(
                        timeLabel = "১ ঘণ্টা",
                        prob = metrics?.next1Hour ?: 0,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScreenProbChip(
                        timeLabel = "৩ ঘণ্টা",
                        prob = metrics?.next3Hours ?: 0,
                        modifier = Modifier.weight(1f)
                    )
                    ScreenProbChip(
                        timeLabel = "৬ ঘণ্টা",
                        prob = metrics?.next6Hours ?: 0,
                        modifier = Modifier.weight(1f)
                    )
                    ScreenProbChip(
                        timeLabel = "২৪ ঘণ্টা",
                        prob = metrics?.next24Hours ?: 0,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 5. Nowcast Timeline UI (এখন -> ১৫ মিনিট -> ৩০ মিনিট -> ১ ঘণ্টা -> ২ ঘণ্টা -> ৩ ঘণ্টা)
        if (timeline.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("timeline_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x33102A4E))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "বৃষ্টির সময়রেখা (Nowcast Timeline)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(timeline) { point ->
                                val probColor = when {
                                    point.probability >= 70 -> Color(0xFFFF5252)
                                    point.probability >= 45 -> Color(0xFFFFD54F)
                                    point.probability >= 25 -> Color(0xFF81D4FA)
                                    else -> Color(0xFFB0BEC5)
                                }
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0x22FFFFFF),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, probColor.copy(alpha = 0.35f)),
                                    modifier = Modifier.width(82.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = point.timeLabel,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White.copy(alpha = 0.85f),
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Icon(
                                            imageVector = if (point.probability >= 50) Icons.Default.WaterDrop else Icons.Default.Umbrella,
                                            contentDescription = null,
                                            tint = probColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "${BanglaUtils.toBanglaDigits(point.probability)}%",
                                            fontSize = 13.sp,
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
                        }
                    }
                }
            }
        }

        // 6. Radar Analysis Card
        radar?.let { r ->
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("radar_analysis_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x33102A4E))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Radar,
                                contentDescription = null,
                                tint = Color(0xFF81D4FA),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "ডপলার রাডার পর্যবেক্ষণ",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "রাডার স্ট্যাটাস",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = r.statusTextBn,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (r.approaching) Color(0xFFFFAB40) else Color.White
                                )
                            }
                            r.direction?.let { dir ->
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "মেঘ প্রবাহের দিক",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = dir,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        if (!r.radarMessageBn.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = r.radarMessageBn,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }

        // 7. Nearest BMD Station Info
        data.bmdStation?.let { st ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = Color(0xFF81D4FA),
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "নিকটতম সিনপটিক পর্যবেক্ষণ স্টেশন (BMD)",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "$st" + (data.bmdDistanceKm?.let { " (দূরত্ব: প্রায় ${BanglaUtils.toBanglaDigits(it.toInt())} কিমি)" } ?: ""),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // 8. Hourly Forecast List (if available)
        if (data.hourly.isNotEmpty()) {
            item {
                Text(
                    text = "ঘণ্টাভিত্তিক বৃষ্টিপাত পূর্বাভাস (২৪ ঘণ্টা)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(data.hourly.take(24)) { h ->
                        HourlyRainCard(item = h)
                    }
                }
            }
        }

        // 9. 7-Day Forecast List (if available)
        if (data.daily.isNotEmpty()) {
            item {
                Text(
                    text = "৭ দিনের বৃষ্টিপাত প্রাক্কলন",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    data.daily.take(7).forEach { d ->
                        DailyRainRow(item = d)
                    }
                }
            }
        }

        // 10. Sources and Freshness Footer
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "তথ্যসূত্র:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.7f)
                )
                data.sources.forEach { src ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "•", fontSize = 12.sp, color = Color(0xFF81D4FA))
                        Text(text = src, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                }

                if (data.updatedAt.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "সর্বশেষ আপডেট: ${BanglaUtils.formatBengaliDateTime(data.updatedAt)}",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScreenProbChip(
    timeLabel: String,
    prob: Int,
    modifier: Modifier = Modifier
) {
    val probColor = when {
        prob >= 70 -> Color(0xFFFF5252)
        prob >= 45 -> Color(0xFFFFD54F)
        prob >= 25 -> Color(0xFF81D4FA)
        else -> Color(0xFFB0BEC5)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color(0x22FFFFFF),
        border = androidx.compose.foundation.BorderStroke(1.dp, probColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = timeLabel,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${BanglaUtils.toBanglaDigits(prob)}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = probColor
            )
        }
    }
}

@Composable
private fun HourlyRainCard(item: HourlyRainModel) {
    val probColor = when {
        item.precipitationProbability >= 70 -> Color(0xFFFF5252)
        item.precipitationProbability >= 45 -> Color(0xFFFFD54F)
        item.precipitationProbability >= 25 -> Color(0xFF81D4FA)
        else -> Color(0xFFB0BEC5)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x2A102A4E)),
        border = androidx.compose.foundation.BorderStroke(1.dp, probColor.copy(alpha = 0.3f)),
        modifier = Modifier.width(78.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = item.time,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Icon(
                imageVector = if (item.precipitationProbability >= 40) Icons.Default.WaterDrop else Icons.Default.Umbrella,
                contentDescription = null,
                tint = probColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${BanglaUtils.toBanglaDigits(item.precipitationProbability)}%",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = probColor
            )
            if (item.precipitationMm > 0.0) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${BanglaUtils.toBanglaDigits(String.format(java.util.Locale.US, "%.1f", item.precipitationMm))} মিমি",
                    fontSize = 9.sp,
                    color = Color(0xFF81D4FA)
                )
            }
        }
    }
}

@Composable
private fun DailyRainRow(item: DailyRainModel) {
    val probColor = when {
        item.maxRainProbability >= 70 -> Color(0xFFFF5252)
        item.maxRainProbability >= 45 -> Color(0xFFFFD54F)
        item.maxRainProbability >= 25 -> Color(0xFF81D4FA)
        else -> Color(0xFFB0BEC5)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0x22FFFFFF),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.date,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = probColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${BanglaUtils.toBanglaDigits(item.maxRainProbability)}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = probColor
                )
            }
        }
    }
}
