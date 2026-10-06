package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TemperatureUnit
import com.example.data.model.CurrentWeatherModel
import com.example.data.model.LocationModel
import com.example.data.model.StationModel
import com.example.util.BanglaUtils

@Composable
fun WeatherTopHeader(
    location: LocationModel,
    isRefreshing: Boolean,
    isGpsLocating: Boolean,
    isLiveLocationActive: Boolean = false,
    updatedAt: String? = null,
    onOpenSearch: () -> Unit,
    onRefresh: () -> Unit,
    onUseGps: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locationTitle = when {
        !location.displayName.isNullOrBlank() -> location.displayName
        !location.upazila.isNullOrBlank() && !location.district.isNullOrBlank() -> "${location.upazila}, ${location.district}"
        !location.district.isNullOrBlank() -> location.district
        location.name.isNotBlank() -> location.name
        location.latitude != 0.0 -> "অক্ষাংশ: ${BanglaUtils.toBanglaDigits(location.latitude)}, দ্রাঘিমাংশ: ${BanglaUtils.toBanglaDigits(location.longitude)}"
        else -> "বাংলাদেশ"
    }

    val dateSubtitle = BanglaUtils.getTodayBengaliDateString()
    val headerSubtitle = if (!updatedAt.isNullOrBlank()) {
        try {
            val cleanStr = if (updatedAt.contains(".")) updatedAt.substringBefore(".") else updatedAt.substringBefore("Z")
            val isoParser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            isoParser.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val date = isoParser.parse(cleanStr)
            if (date != null) {
                val localTime = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US).format(date)
                "$dateSubtitle • ${BanglaUtils.formatHourBengali(localTime)}"
            } else {
                dateSubtitle
            }
        } catch (_: Exception) {
            dateSubtitle
        }
    } else {
        dateSubtitle
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag("home_top_header")
    ) {
        // Top line: Menu ☰ | 📍 Location Title & Date | Refresh
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onOpenSearch,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0x2BFFFFFF))
                    .testTag("btn_menu_drawer")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Search / Menu",
                    tint = Color.White
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenSearch() }
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFFFFCC80),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = locationTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = headerSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }

            IconButton(
                onClick = onRefresh,
                enabled = !isRefreshing,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0x2BFFFFFF))
                    .testTag("btn_refresh_top")
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action row: Live GPS Indicator / Switcher + Search trigger
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isLiveLocationActive) Color(0x334CAF50) else Color(0x2BFFFFFF))
                    .clickable { onUseGps() }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .testTag("btn_quick_live_gps")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isGpsLocating) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = Color(0xFF81D4FA),
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = if (isLiveLocationActive) Color(0xFF81C784) else Color(0xFF81D4FA),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = if (isLiveLocationActive) "লাইভ GPS অবস্থান সক্রিয়" else "বর্তমান অবস্থান (GPS)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocationHierarchyCard(
    location: LocationModel,
    modifier: Modifier = Modifier
) {
    val items = mutableListOf<Pair<String, String>>()
    if (!location.village.isNullOrBlank()) items.add("গ্রাম" to location.village)
    if (!location.union.isNullOrBlank()) items.add("ইউনিয়ন" to location.union)
    if (!location.upazila.isNullOrBlank()) items.add("উপজেলা" to location.upazila)
    if (!location.district.isNullOrBlank()) items.add("জেলা" to location.district)
    if (!location.division.isNullOrBlank()) items.add("বিভাগ" to location.division)
    if (!location.country.isNullOrBlank()) items.add("দেশ" to location.country)
    if (location.latitude != 0.0 && location.longitude != 0.0) {
        items.add("স্থানাঙ্ক" to "${BanglaUtils.toBanglaDigits(location.latitude)}° N, ${BanglaUtils.toBanglaDigits(location.longitude)}° E")
    }

    if (items.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x22102A4E))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "প্রশাসনিক অবস্থান বিবরণ",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF81D4FA)
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items.forEach { (label, value) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x26FFFFFF))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$label: $value",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherHeroCard(
    current: CurrentWeatherModel,
    station: StationModel?,
    tempUnit: TemperatureUnit,
    modifier: Modifier = Modifier
) {
    val tempText = BanglaUtils.formatTemp(current.temperature, tempUnit)
    val feelsLikeText = BanglaUtils.formatFeelsLike(current.feelsLike, tempUnit)
    val conditionText = current.conditionBn ?: BanglaUtils.mapCondition(current.condition, current.weatherCode)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("main_hero_weather_card"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x33102A4E)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WeatherConditionIcon(
                weatherCode = current.weatherCode,
                size = 72.dp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = tempText,
                fontFamily = com.example.ui.theme.AnekBanglaFontFamily,
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 76.sp,
                modifier = Modifier.testTag("hero_temperature_text")
            )

            if (feelsLikeText.isNotBlank()) {
                Text(
                    text = feelsLikeText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = conditionText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE1F5FE)
            )

            if (current.tempMax != null || current.tempMin != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val high = BanglaUtils.formatTemp(current.tempMax, tempUnit)
                val low = BanglaUtils.formatTemp(current.tempMin, tempUnit)
                Text(
                    text = "সর্বোচ্চ $high  |  সর্বনিম্ন $low",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // BMD Data Source badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x33FFFFFF))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = "BMD",
                        tint = Color(0xFF80DEEA),
                        modifier = Modifier.size(16.dp)
                    )

                    val sourceText = if (station != null && station.name.isNotBlank()) {
                        val cleanName = formatStationName(station.name)
                        val absDistance = kotlin.math.abs(station.distanceKm)
                        val distStr = BanglaUtils.toBanglaDigits(absDistance)
                        val codeStr = if (station.code.isNotBlank()) " (${station.code})" else ""
                        "BMD $cleanName$codeStr • $distStr কিমি দূরে"
                    } else {
                        "BMD পর্যবেক্ষণ স্টেশন"
                    }

                    Text(
                        text = sourceText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

fun formatStationName(rawName: String): String {
    val clean = rawName
        .replace(",yes", "", ignoreCase = true)
        .replace(",no", "", ignoreCase = true)
        .replace("_", " ")
        .trim()
    return when {
        clean.contains("Cox", ignoreCase = true) -> "কক্সবাজার"
        clean.contains("Dhaka", ignoreCase = true) -> "ঢাকা"
        clean.contains("Chattogram", ignoreCase = true) || clean.contains("Chittagong", ignoreCase = true) -> "চট্টগ্রাম"
        clean.contains("Sylhet", ignoreCase = true) -> "সিলেট"
        clean.contains("Rajshahi", ignoreCase = true) -> "রাজশাহী"
        clean.contains("Khulna", ignoreCase = true) -> "খুলনা"
        clean.contains("Barishal", ignoreCase = true) || clean.contains("Barisal", ignoreCase = true) -> "বরিশাল"
        clean.contains("Rangpur", ignoreCase = true) -> "রংপুর"
        clean.contains("Mymensingh", ignoreCase = true) -> "ময়মনসিংহ"
        clean.contains("Bogura", ignoreCase = true) || clean.contains("Bogra", ignoreCase = true) -> "বগুড়া"
        clean.contains("Teknaf", ignoreCase = true) -> "টেকনাফ"
        clean.contains("Sreemangal", ignoreCase = true) -> "শ্রীমঙ্গল"
        clean.contains("Satkhira", ignoreCase = true) -> "সাতক্ষীরা"
        clean.contains("Mongla", ignoreCase = true) -> "মোংলা"
        clean.contains("Patuakhali", ignoreCase = true) -> "পটুয়াখালী"
        clean.contains("Bhola", ignoreCase = true) -> "ভোলা"
        clean.contains("Feni", ignoreCase = true) -> "ফেনী"
        clean.contains("Cumilla", ignoreCase = true) || clean.contains("Comilla", ignoreCase = true) -> "কুমিল্লা"
        clean.contains("Chandpur", ignoreCase = true) -> "চাঁদপুর"
        clean.contains("Noakhali", ignoreCase = true) || clean.contains("Maijdee", ignoreCase = true) -> "নোয়াখালী"
        clean.contains("Tangail", ignoreCase = true) -> "টাঙ্গাইল"
        clean.contains("Faridpur", ignoreCase = true) -> "ফরিদপুর"
        clean.contains("Madaripur", ignoreCase = true) -> "মাদারীপুর"
        clean.contains("Sandwip", ignoreCase = true) -> "সন্দ্বীপ"
        clean.contains("Hatiya", ignoreCase = true) -> "হাতিয়া"
        clean.contains("Kutubdia", ignoreCase = true) -> "কুতুবদিয়া"
        clean.contains("Dinajpur", ignoreCase = true) -> "দিনাজপুর"
        clean.contains("Jessore", ignoreCase = true) || clean.contains("Jashore", ignoreCase = true) -> "যশোর"
        clean.contains("Kushtia", ignoreCase = true) -> "কুষ্টিয়া"
        clean.contains("Kurigram", ignoreCase = true) -> "কুড়িগ্রাম"
        else -> clean
    }
}
