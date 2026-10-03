package com.example

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.example.worker.WeatherSyncManager
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

        // Ensure background periodic sync is active
        WeatherSyncManager.schedulePeriodicSync(applicationContext)

        setContent {
            val userPrefs by preferencesRepo.userPreferencesFlow.collectAsStateWithLifecycle(
                initialValue = com.example.data.local.UserPreferences()
            )

            // Localized Context provider for Bengali / English dynamic switching
            val localizedContext = rememberLocalizedContext(LocalContext.current, userPrefs.language)

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalActivityResultRegistryOwner provides this@MainActivity
            ) {
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

                    // Permission launcher for location and background notifications
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                        if (fineGranted || coarseGranted) {
                            homeViewModel.useGps()
                        }
                    }

                    // On app launch: check location permission and refresh dynamic location data
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

                    // Lifecycle observer: Whenever user opens or switches back to the app,
                    // check current location and update weather data immediately
                    val lifecycleOwner = LocalLifecycleOwner.current
                    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
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
    val locale = Locale.forLanguageTag(languageCode)
    Locale.setDefault(locale)
    val config = Configuration(baseContext.resources.configuration)
    config.setLocale(locale)
    return baseContext.createConfigurationContext(config)
}
