package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.navigation.AppNavigation
import com.example.ui.alerts.AlertsViewModel
import com.example.ui.home.HomeViewModel
import com.example.ui.hourly.HourlyViewModel
import com.example.ui.radar.RadarViewModel
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.WeatherTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as WeatherApplication
        val weatherRepo = app.weatherRepository
        val bmdRepo = app.bmdRepository
        val radarRepo = app.radarRepository
        val rainRepo = app.rainRepository
        val preferencesRepo = app.userPreferencesRepository
        val locationTracker = app.locationTracker

        setContent {
            WeatherTheme {
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

                val alertsViewModel: AlertsViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return AlertsViewModel(weatherRepo, bmdRepo, preferencesRepo) as T
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

                val stationsViewModel: com.example.ui.stations.BmdStationsViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return com.example.ui.stations.BmdStationsViewModel(bmdRepo, preferencesRepo) as T
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

                val rainViewModel: com.example.ui.rain.RainPredictionViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return com.example.ui.rain.RainPredictionViewModel(rainRepo, preferencesRepo) as T
                        }
                    }
                )

                val upazilaViewModel: com.example.ui.forecast.UpazilaForecastViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return com.example.ui.forecast.UpazilaForecastViewModel(
                                repository = app.upazilaForecastRepository,
                                preferencesRepository = preferencesRepo,
                                locationTracker = locationTracker
                            ) as T
                        }
                    }
                )

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
                    val neededPermissions = mutableListOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    if (!locationTracker.hasLocationPermission()) {
                        permissionLauncher.launch(neededPermissions.toTypedArray())
                    } else {
                        homeViewModel.checkLocationAndUpdate(forceRefresh = true)
                    }
                }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            if (locationTracker.hasLocationPermission()) {
                                homeViewModel.checkLocationAndUpdate(forceRefresh = true)
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        homeViewModel = homeViewModel,
                        hourlyViewModel = hourlyViewModel,
                        alertsViewModel = alertsViewModel,
                        radarViewModel = radarViewModel,
                        stationsViewModel = stationsViewModel,
                        settingsViewModel = settingsViewModel,
                        rainViewModel = rainViewModel,
                        upazilaViewModel = upazilaViewModel
                    )
                }
            }
        }
    }
}
