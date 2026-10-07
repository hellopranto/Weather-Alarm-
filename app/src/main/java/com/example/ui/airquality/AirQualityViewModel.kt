package com.example.ui.airquality

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.AirQualityResponse
import com.example.domain.repository.AirQualityRepository
import com.example.domain.repository.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class AqiStandard {
    US,
    EUROPEAN
}

sealed interface AirQualityUiState {
    data object Loading : AirQualityUiState
    data class Success(
        val data: AirQualityResponse,
        val isOffline: Boolean = false,
        val cityName: String = "",
        val selectedStandard: AqiStandard = AqiStandard.US
    ) : AirQualityUiState
    data class Error(val message: String, val cachedData: AirQualityResponse? = null) : AirQualityUiState
}

class AirQualityViewModel(
    private val airQualityRepository: AirQualityRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AirQualityUiState>(AirQualityUiState.Loading)
    val uiState: StateFlow<AirQualityUiState> = _uiState.asStateFlow()

    private val _selectedStandard = MutableStateFlow(AqiStandard.US)
    val selectedStandard: StateFlow<AqiStandard> = _selectedStandard.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadAirQuality(forceRefresh = false)
    }

    fun setAqiStandard(standard: AqiStandard) {
        _selectedStandard.value = standard
        val current = _uiState.value
        if (current is AirQualityUiState.Success) {
            _uiState.value = current.copy(selectedStandard = standard)
        }
    }

    fun loadAirQuality(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (forceRefresh) _isRefreshing.value = true

            val prefs = preferencesRepository.userPreferencesFlow.first()
            val lat = prefs.selectedLatitude
            val lon = prefs.selectedLongitude
            val cityName = prefs.selectedCityName

            airQualityRepository.getAirQuality(lat, lon, forceRefresh).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        if (_uiState.value !is AirQualityUiState.Success) {
                            _uiState.value = AirQualityUiState.Loading
                        }
                    }
                    is Resource.Success -> {
                        _uiState.value = AirQualityUiState.Success(
                            data = resource.data,
                            isOffline = resource.isOfflineCached,
                            cityName = cityName,
                            selectedStandard = _selectedStandard.value
                        )
                        _isRefreshing.value = false
                    }
                    is Resource.Error -> {
                        val cached = airQualityRepository.getCachedAirQuality(lat, lon)
                        _uiState.value = AirQualityUiState.Error(resource.message, cached)
                        _isRefreshing.value = false
                    }
                }
            }
        }
    }
}
