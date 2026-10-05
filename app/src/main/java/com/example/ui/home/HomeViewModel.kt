package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.UnifiedWeatherResponse
import com.example.domain.repository.Resource
import com.example.domain.repository.WeatherRepository
import com.example.location.BangladeshCity
import com.example.location.LocationItem
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
        val userPreferences: UserPreferences,
        val isLiveGpsActive: Boolean = false
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

    private val _isGpsLocating = MutableStateFlow(false)
    val isGpsLocating: StateFlow<Boolean> = _isGpsLocating.asStateFlow()

    private val _isLiveLocation = MutableStateFlow(true)
    val isLiveLocation: StateFlow<Boolean> = _isLiveLocation.asStateFlow()

    private val _locationMessage = MutableStateFlow<String?>(null)
    val locationMessage: StateFlow<String?> = _locationMessage.asStateFlow()

    fun dismissLocationMessage() {
        _locationMessage.value = null
    }

    init {
        initializeLocationAndWeather()
    }

    private fun initializeLocationAndWeather() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val prefs = preferencesRepository.userPreferencesFlow.first()
            _isLiveLocation.value = prefs.useGpsLocation

            var targetLat = prefs.selectedLatitude
            var targetLon = prefs.selectedLongitude
            var targetCity = prefs.selectedCityName

            // If user previously set GPS as active, verify device location
            if (prefs.useGpsLocation && locationTracker.hasLocationPermission()) {
                val loc = locationTracker.getCurrentLocation()
                if (loc != null) {
                    targetLat = loc.latitude
                    targetLon = loc.longitude
                    targetCity = "" // Instructs backend to reverse-geocode
                }
            }

            fetchWeather(targetLat, targetLon, targetCity, isGps = prefs.useGpsLocation, forceRefresh = true)
        }
    }

    private fun fetchWeather(lat: Double, lon: Double, cityName: String, isGps: Boolean, forceRefresh: Boolean) {
        viewModelScope.launch {
            val prefs = preferencesRepository.userPreferencesFlow.first()
            weatherRepository.getWeather(lat, lon, cityName, forceRefresh).collect { resource ->
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
                            userPreferences = prefs,
                            isLiveGpsActive = isGps
                        )
                        _isRefreshing.value = false

                        // Remember the successfully resolved location
                        val resolvedName = resource.data.location.displayName
                            ?: resource.data.location.name.ifBlank { cityName }
                        preferencesRepository.setSelectedLocation(
                            name = resolvedName,
                            lat = lat,
                            lon = lon,
                            useGps = isGps
                        )
                    }
                    is Resource.Error -> {
                        if (_uiState.value !is WeatherUiState.Success) {
                            val cached = weatherRepository.getCachedWeather(lat, lon)
                            _uiState.value = WeatherUiState.Error(
                                message = resource.message,
                                cachedData = cached
                            )
                        } else {
                            _locationMessage.value = resource.message
                        }
                        _isRefreshing.value = false
                    }
                }
            }
        }
    }

    fun loadWeather(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val prefs = preferencesRepository.userPreferencesFlow.first()
            if (prefs.useGpsLocation) {
                useGps()
            } else {
                fetchWeather(
                    prefs.selectedLatitude,
                    prefs.selectedLongitude,
                    prefs.selectedCityName,
                    isGps = false,
                    forceRefresh = forceRefresh
                )
            }
        }
    }

    fun checkLocationAndUpdate(forceRefresh: Boolean = true) {
        loadWeather(forceRefresh)
    }


    fun selectLocationItem(item: LocationItem) {
        viewModelScope.launch {
            _isLiveLocation.value = false
            _isRefreshing.value = true
            preferencesRepository.setSelectedLocation(
                name = item.nameBn,
                lat = item.latitude,
                lon = item.longitude,
                useGps = false
            )
            _locationMessage.value = "নির্বাচিত স্থান: 📍 ${item.nameBn}"
            fetchWeather(item.latitude, item.longitude, item.nameBn, isGps = false, forceRefresh = true)
        }
    }

    fun selectCity(city: BangladeshCity) {
        viewModelScope.launch {
            _isLiveLocation.value = false
            _isRefreshing.value = true
            preferencesRepository.setSelectedLocation(
                name = city.nameBn,
                lat = city.latitude,
                lon = city.longitude,
                useGps = false
            )
            _locationMessage.value = "নির্বাচিত স্থান: 📍 ${city.nameBn}"
            fetchWeather(city.latitude, city.longitude, city.nameBn, isGps = false, forceRefresh = true)
        }
    }

    fun useGps() {
        viewModelScope.launch {
            if (!locationTracker.hasLocationPermission()) {
                _locationMessage.value = "আপনার বর্তমান এলাকার সঠিক আবহাওয়া পেতে লোকেশন পারমিশন প্রয়োজন। অনুগ্রহ করে ডিভাইসের সেটিংসে গিয়ে লোকেশন পারমিশন সক্রিয় করুন।"
                return@launch
            }

            _isGpsLocating.value = true
            _isRefreshing.value = true
            val loc = locationTracker.getCurrentLocation()
            _isGpsLocating.value = false

            if (loc != null) {
                _isLiveLocation.value = true
                preferencesRepository.setSelectedLocation(
                    name = "",
                    lat = loc.latitude,
                    lon = loc.longitude,
                    useGps = true
                )
                _locationMessage.value = "বর্তমান জিপিএস অবস্থান অনুযায়ী আবহাওয়া আপডেট হয়েছে।"
                fetchWeather(loc.latitude, loc.longitude, "", isGps = true, forceRefresh = true)
            } else {
                _isRefreshing.value = false
                _locationMessage.value = "জিপিএস অবস্থান সনাক্ত করতে সময় শেষ হয়েছে। পূর্ববর্তী সংরক্ষিত অবস্থান রাখা হয়েছে।"
            }
        }
    }
}
