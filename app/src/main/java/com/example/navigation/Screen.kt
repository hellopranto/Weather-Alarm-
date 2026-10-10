package com.example.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val titleBn: String, val icon: ImageVector) {
    data object Home : Screen("home", "হোম", Icons.Default.WbSunny)
    data object Hourly : Screen("hourly", "২৪ ঘণ্টা", Icons.Default.HourglassTop)
    data object Radar : Screen("radar", "রাডার", Icons.Default.Map)
    data object Stations : Screen("stations", "স্টেশন", Icons.Default.Sensors)
    data object Alerts : Screen("alerts", "সতর্কতা", Icons.Default.NotificationsActive)
    data object Settings : Screen("settings", "সেটিংস", Icons.Default.Settings)
    data object Rain : Screen("rain", "বৃষ্টির পূর্বাভাস", Icons.Default.Umbrella)
    data object Upazila : Screen("upazila", "উপজেলা", Icons.Default.LocationCity)
}
