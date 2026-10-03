package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.UnifiedWeatherResponse
import com.example.domain.repository.Resource
import com.example.domain.repository.WeatherRepository
import com.example.location.BangladeshCity
import com.example.location.LocationTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface WeatherUiState {
    data object Loading : WeatherUiState
    data class Success(
        val data: UnifiedWeatherResponse,
        val isOfflineCached: Boolean = false,
        val userPreferences: UserPreferences
    ) : WeatherUiState
    data class Error(
        val message: String,
        val cachedData: UnifiedWeatherResponse? = null
    ) : WeatherUiState
}

class HomeViewModel(
    private val weatherRepository: WeatherRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val locationTracker: LocationTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        // Every time the ViewModel is initialized on app open, check location and fetch fresh data
        checkLocationAndUpdate(forceRefresh = true)
    }

    /**
     * Checks current GPS device location if permission is granted, updates coordinates,
     * and fetches fresh weather and dynamic location address from OpenWeatherMap & BMD.
     */
    fun checkLocationAndUpdate(forceRefresh: Boolean = true) {
        viewModelScope.launch {
            if (forceRefresh) _isRefreshing.value = true

            val prefs = preferencesRepository.userPreferencesFlow.first()
            var targetLat = prefs.selectedLatitude
            var targetLon = prefs.selectedLongitude
            var targetCity = prefs.selectedCityName

            // Check location permission on every open
            if (locationTracker.hasLocationPermission()) {
                val loc = locationTracker.getCurrentLocation()
                if (loc != null) {
                    targetLat = loc.latitude
                    targetLon = loc.longitude
                    // Empty city name instructs backend and OpenWeather to reverse-geocode
                    // and return the exact dynamic locality/neighborhood/district name
                    targetCity = ""
                }
            }

            weatherRepository.getWeather(targetLat, targetLon, targetCity, forceRefresh).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        if (_uiState.value !is WeatherUiState.Success) {
                            _uiState.value = WeatherUiState.Loading
                        }
                    }
                    is Resource.Success -> {
                        _uiState.value = WeatherUiState.Success(
                            data = resource.data,
                            isOfflineCached = resource.isOfflineCached,
                            userPreferences = prefs
                        )
                        _isRefreshing.value = false

                        // Automatically persist dynamic location name obtained from OpenWeather
                        if (resource.data.location.name.isNotBlank()) {
                            preferencesRepository.setSelectedLocation(
                                name = resource.data.location.name,
                                lat = targetLat,
                                lon = targetLon,
                                useGps = prefs.useGpsLocation
                            )
                        }
                    }
                    is Resource.Error -> {
                        val cached = weatherRepository.getCachedWeather(targetLat, targetLon)
                        _uiState.value = WeatherUiState.Error(
                            message = resource.message,
                            cachedData = cached
                        )
                        _isRefreshing.value = false
                    }
                }
            }
        }
    }

    fun loadWeather(forceRefresh: Boolean = false) {
        checkLocationAndUpdate(forceRefresh)
    }

    fun selectCity(city: BangladeshCity) {
        viewModelScope.launch {
            preferencesRepository.setUseGpsLocation(false)
            preferencesRepository.setSelectedLocation(city.nameEn, city.latitude, city.longitude)
            checkLocationAndUpdate(forceRefresh = true)
        }
    }

    fun useGps() {
        viewModelScope.launch {
            preferencesRepository.setUseGpsLocation(true)
            checkLocationAndUpdate(forceRefresh = true)
        }
    }
}
