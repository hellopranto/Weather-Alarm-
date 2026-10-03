package com.example.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R

sealed class Screen(
    val route: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector,
    val testTag: String
) {
    data object Home : Screen("home", R.string.nav_home, Icons.Filled.Home, "nav_item_home")
    data object Hourly : Screen("hourly", R.string.nav_hourly, Icons.Filled.Schedule, "nav_item_hourly")
    data object Radar : Screen("radar", R.string.nav_radar, Icons.Filled.Radar, "nav_item_radar")
    data object Alerts : Screen("alerts", R.string.nav_alerts, Icons.Filled.NotificationsActive, "nav_item_alerts")
    data object Settings : Screen("settings", R.string.nav_settings, Icons.Filled.Settings, "nav_item_settings")

    companion object {
        val bottomNavItems = listOf(Home, Hourly, Radar, Alerts, Settings)
    }
}
