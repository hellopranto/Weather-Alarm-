package com.example.ui.hourly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.UnifiedWeatherResponse
import com.example.domain.repository.Resource
import com.example.domain.repository.WeatherRepository
import com.example.ui.home.WeatherUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HourlyViewModel(
    private val weatherRepository: WeatherRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    init {
        loadForecast()
    }

    fun loadForecast() {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            val prefs = preferencesRepository.userPreferencesFlow.first()
            weatherRepository.getWeather(
                prefs.selectedLatitude,
                prefs.selectedLongitude,
                prefs.selectedCityName
            ).collect { res ->
                when (res) {
                    is Resource.Loading -> _uiState.value = WeatherUiState.Loading
                    is Resource.Success -> _uiState.value = WeatherUiState.Success(
                        data = res.data,
                        isOfflineCached = res.isOfflineCached,
                        userPreferences = prefs
                    )
                    is Resource.Error -> {
                        val cached = weatherRepository.getCachedWeather(prefs.selectedLatitude, prefs.selectedLongitude)
                        _uiState.value = WeatherUiState.Error(res.message, cached)
                    }
                }
            }
        }
    }
}
