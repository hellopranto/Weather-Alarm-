package com.example.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.WeatherAlertModel
import com.example.domain.repository.BmdWeatherRepository
import com.example.domain.repository.Resource
import com.example.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AlertsUiState(
    val isLoading: Boolean = true,
    val alerts: List<WeatherAlertModel> = emptyList(),
    val error: String? = null
)

class AlertsViewModel(
    private val weatherRepository: WeatherRepository,
    private val bmdRepository: BmdWeatherRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    init {
        loadAlerts()
    }

    fun loadAlerts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val prefs = preferencesRepository.userPreferencesFlow.first()

            weatherRepository.getWeather(
                prefs.selectedLatitude,
                prefs.selectedLongitude,
                prefs.selectedCityName
            ).collect { res ->
                when (res) {
                    is Resource.Loading -> _uiState.value = _uiState.value.copy(isLoading = true)
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            alerts = res.data.alerts,
                            error = null
                        )
                    }
                    is Resource.Error -> {
                        // Check if BMD warnings can be fetched separately
                        bmdRepository.getWeatherWarnings().collect { bmdRes ->
                            if (bmdRes is Resource.Success) {
                                val mapped = bmdRes.data.map { w ->
                                    WeatherAlertModel(
                                        id = w.id,
                                        title = w.title,
                                        description = w.description,
                                        severity = w.severity,
                                        startTime = w.issuedAt,
                                        endTime = w.validUntil,
                                        source = "Bangladesh Meteorological Department (BMD)",
                                        signalNumber = w.signalNumber,
                                        regions = w.affectedRegions
                                    )
                                }
                                _uiState.value = _uiState.value.copy(isLoading = false, alerts = mapped)
                            } else {
                                _uiState.value = _uiState.value.copy(isLoading = false, error = res.message)
                            }
                        }
                    }
                }
            }
        }
    }
}
