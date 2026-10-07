package com.example.ui.rain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.RainPredictionResponse
import com.example.domain.repository.RainRepository
import com.example.domain.repository.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface RainUiState {
    data object Loading : RainUiState
    data class Success(
        val data: RainPredictionResponse,
        val isOffline: Boolean = false,
        val cityName: String = ""
    ) : RainUiState
    data class Error(val message: String, val cachedData: RainPredictionResponse? = null) : RainUiState
}

class RainPredictionViewModel(
    private val rainRepository: RainRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RainUiState>(RainUiState.Loading)
    val uiState: StateFlow<RainUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadRainPrediction(forceRefresh = false)
    }

    fun loadRainPrediction(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (forceRefresh) _isRefreshing.value = true

            val prefs = preferencesRepository.userPreferencesFlow.first()
            val lat = prefs.selectedLatitude
            val lon = prefs.selectedLongitude
            val cityName = prefs.selectedCityName

            rainRepository.getRainPrediction(lat, lon, forceRefresh).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        if (_uiState.value !is RainUiState.Success) {
                            _uiState.value = RainUiState.Loading
                        }
                    }
                    is Resource.Success -> {
                        _uiState.value = RainUiState.Success(
                            data = resource.data,
                            isOffline = resource.isOfflineCached,
                            cityName = cityName
                        )
                        _isRefreshing.value = false
                    }
                    is Resource.Error -> {
                        val cached = rainRepository.getCachedRainPrediction(lat, lon)
                        _uiState.value = RainUiState.Error(resource.message, cached)
                        _isRefreshing.value = false
                    }
                }
            }
        }
    }
}
