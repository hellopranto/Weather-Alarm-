package com.example

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.remote.ApiClient
import com.example.data.repository.BmdWeatherRepositoryImpl
import com.example.data.repository.RadarRepositoryImpl
import com.example.data.repository.WeatherRepositoryImpl
import com.example.location.DefaultLocationTracker
import com.example.navigation.AppNavigation
import com.example.ui.alerts.AlertsViewModel
import com.example.ui.home.HomeViewModel
import com.example.ui.hourly.HourlyViewModel
import com.example.ui.radar.RadarViewModel
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.MyApplicationTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val weatherDao = database.weatherDao()
        val preferencesRepo = UserPreferencesRepository(applicationContext)
        val locationTracker = DefaultLocationTracker(applicationContext)

        val weatherRepo = WeatherRepositoryImpl(ApiClient.weatherApi, weatherDao)
        val bmdRepo = BmdWeatherRepositoryImpl(ApiClient.bmdApi)
        val radarRepo = RadarRepositoryImpl(ApiClient.radarApi)

        setContent {
            val userPrefs by preferencesRepo.userPreferencesFlow.collectAsStateWithLifecycle(
                initialValue = com.example.data.local.UserPreferences()
            )

            // Localized Context provider for Bengali / English dynamic switching
            val localizedContext = rememberLocalizedContext(LocalContext.current, userPrefs.language)

            CompositionLocalProvider(LocalContext provides localizedContext) {
                MyApplicationTheme(themeMode = userPrefs.themeMode) {
                    val navController = rememberNavController()

                    val homeViewModel: HomeViewModel = viewModel(
                        factory = object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return HomeViewModel(weatherRepo, preferencesRepo, locationTracker) as T
                            }
                        }
                    )

                    val hourlyViewModel: HourlyViewModel = viewModel(
                        factory = object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return HourlyViewModel(weatherRepo, preferencesRepo) as T
                            }
                        }
                    )

                    val radarViewModel: RadarViewModel = viewModel(
                        factory = object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return RadarViewModel(radarRepo, preferencesRepo) as T
                            }
                        }
                    )

                    val alertsViewModel: AlertsViewModel = viewModel(
                        factory = object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return AlertsViewModel(weatherRepo, bmdRepo, preferencesRepo) as T
                            }
                        }
                    )

                    val settingsViewModel: SettingsViewModel = viewModel(
                        factory = object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return SettingsViewModel(preferencesRepo) as T
                            }
                        }
                    )

                    // Permission launcher for location
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                        if (fineGranted || coarseGranted) {
                            homeViewModel.useGps()
                        }
                    }

                    LaunchedEffect(Unit) {
                        if (userPrefs.useGpsLocation && !locationTracker.hasLocationPermission()) {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    }

                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNavigation(
                            navController = navController,
                            homeViewModel = homeViewModel,
                            hourlyViewModel = hourlyViewModel,
                            radarViewModel = radarViewModel,
                            alertsViewModel = alertsViewModel,
                            settingsViewModel = settingsViewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun rememberLocalizedContext(baseContext: Context, languageCode: String): Context {
    val locale = Locale(languageCode)
    Locale.setDefault(locale)
    val config = Configuration(baseContext.resources.configuration)
    config.setLocale(locale)
    return baseContext.createConfigurationContext(config)
}
