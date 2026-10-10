package com.example.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.alerts.AlertsScreen
import com.example.ui.alerts.AlertsViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.hourly.HourlyScreen
import com.example.ui.hourly.HourlyViewModel
import com.example.ui.radar.RadarScreen
import com.example.ui.radar.RadarViewModel
import com.example.ui.rain.RainPredictionScreen
import com.example.ui.rain.RainPredictionViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.stations.BmdStationsScreen
import com.example.ui.stations.BmdStationsViewModel

@Composable
fun AppNavigation(
    homeViewModel: HomeViewModel,
    hourlyViewModel: HourlyViewModel,
    alertsViewModel: AlertsViewModel,
    radarViewModel: RadarViewModel,
    stationsViewModel: BmdStationsViewModel,
    settingsViewModel: SettingsViewModel,
    rainViewModel: RainPredictionViewModel,
    upazilaViewModel: com.example.ui.forecast.UpazilaForecastViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val screens = listOf(
        Screen.Home,
        Screen.Hourly,
        Screen.Upazila,
        Screen.Radar,
        Screen.Stations,
        Screen.Alerts,
        Screen.Settings
    )

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xEE0A1B36),
                contentColor = Color.White
            ) {
                screens.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(imageVector = screen.icon, contentDescription = screen.titleBn)
                        },
                        label = {
                            Text(text = screen.titleBn, fontSize = 10.sp, maxLines = 1)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF0D3268),
                            selectedTextColor = Color(0xFF81D4FA),
                            indicatorColor = Color(0xFF81D4FA),
                            unselectedIconColor = Color.White.copy(alpha = 0.6f),
                            unselectedTextColor = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToAlerts = { navController.navigate(Screen.Alerts.route) },
                    onNavigateToRainPrediction = { navController.navigate(Screen.Rain.route) },
                    onNavigateToUpazila = { navController.navigate(Screen.Upazila.route) }
                )
            }
            composable(Screen.Rain.route) {
                RainPredictionScreen(
                    viewModel = rainViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Hourly.route) {
                HourlyScreen(viewModel = hourlyViewModel)
            }
            composable(Screen.Upazila.route) {
                com.example.ui.forecast.UpazilaForecastScreen(viewModel = upazilaViewModel)
            }
            composable(Screen.Radar.route) {
                RadarScreen(viewModel = radarViewModel)
            }
            composable(Screen.Stations.route) {
                BmdStationsScreen(
                    viewModel = stationsViewModel,
                    onSelectStationForHome = { city ->
                        homeViewModel.selectCity(city)
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Alerts.route) {
                AlertsScreen(viewModel = alertsViewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = settingsViewModel)
            }
        }
    }
}
