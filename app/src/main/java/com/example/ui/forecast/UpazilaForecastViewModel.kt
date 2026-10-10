package com.example.ui.forecast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.UpazilaBbsRecord
import com.example.data.model.UpazilaUiForecast
import com.example.domain.repository.Resource
import com.example.domain.repository.UpazilaForecastRepository
import com.example.location.LocationTracker
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class UpazilaForecastUiState {
    data object Loading : UpazilaForecastUiState()
    data class Success(
        val forecast: UpazilaUiForecast,
        val isCached: Boolean = false
    ) : UpazilaForecastUiState()
    data class Error(
        val message: String,
        val cachedForecast: UpazilaUiForecast? = null
    ) : UpazilaForecastUiState()
}

class UpazilaForecastViewModel(
    private val repository: UpazilaForecastRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val locationTracker: LocationTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow<UpazilaForecastUiState>(UpazilaForecastUiState.Loading)
    val uiState: StateFlow<UpazilaForecastUiState> = _uiState.asStateFlow()

    private val _selectedSource = MutableStateFlow("BMDWRF")
    val selectedSource: StateFlow<String> = _selectedSource.asStateFlow()

    private val _currentUpazila = MutableStateFlow<UpazilaBbsRecord?>(null)
    val currentUpazila: StateFlow<UpazilaBbsRecord?> = _currentUpazila.asStateFlow()

    private val _allUpazilas = MutableStateFlow<List<UpazilaBbsRecord>>(emptyList())
    val allUpazilas: StateFlow<List<UpazilaBbsRecord>> = _allUpazilas.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var fetchJob: Job? = null

    init {
        loadUpazilasAndCurrentLocation()
    }

    private fun loadUpazilasAndCurrentLocation() {
        viewModelScope.launch {
            val list = repository.getUpazilas()
            _allUpazilas.value = list

            val prefs = preferencesRepository.userPreferencesFlow.first()
            if (prefs.useGpsLocation && locationTracker.hasLocationPermission()) {
                val loc = locationTracker.getCurrentLocation()
                if (loc != null) {
                    val nearest = repository.findNearestUpazila(loc.latitude, loc.longitude)
                    if (nearest != null) {
                        _currentUpazila.value = nearest
                        loadForecast(nearest.pcode, _selectedSource.value)
                        return@launch
                    }
                }
            }

            // Fallback to saved coordinates or default Dhaka / selected upazila
            val lat = if (prefs.selectedLatitude != 0.0) prefs.selectedLatitude else 23.8103
            val lon = if (prefs.selectedLongitude != 0.0) prefs.selectedLongitude else 90.4125
            val nearest = repository.findNearestUpazila(lat, lon)
            if (nearest != null) {
                _currentUpazila.value = nearest
                loadForecast(nearest.pcode, _selectedSource.value)
            }
        }
    }

    fun setSource(source: String) {
        if (_selectedSource.value == source) return
        _selectedSource.value = source
        _currentUpazila.value?.let {
            loadForecast(it.pcode, source, forceRefresh = true)
        }
    }

    fun selectUpazila(upazila: UpazilaBbsRecord) {
        _currentUpazila.value = upazila
        loadForecast(upazila.pcode, _selectedSource.value, forceRefresh = true)
    }

    fun refresh() {
        _currentUpazila.value?.let {
            loadForecast(it.pcode, _selectedSource.value, forceRefresh = true)
        } ?: loadUpazilasAndCurrentLocation()
    }

    fun useCurrentGpsLocation() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val loc = locationTracker.getCurrentLocation()
            if (loc != null) {
                val nearest = repository.findNearestUpazila(loc.latitude, loc.longitude)
                if (nearest != null) {
                    _currentUpazila.value = nearest
                    loadForecast(nearest.pcode, _selectedSource.value, forceRefresh = true)
                }
            }
            _isRefreshing.value = false
        }
    }

    private fun loadForecast(pcode: String, source: String, forceRefresh: Boolean = false) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            _isRefreshing.value = true
            repository.getUpazilaForecast(pcode, source, forceRefresh).collect { res ->
                when (res) {
                    is Resource.Loading -> {
                        if (_uiState.value !is UpazilaForecastUiState.Success) {
                            _uiState.value = UpazilaForecastUiState.Loading
                        }
                    }
                    is Resource.Success -> {
                        _uiState.value = UpazilaForecastUiState.Success(res.data, isCached = res.isOfflineCached)
                        _isRefreshing.value = false
                    }
                    is Resource.Error -> {
                        val cached = (res.cachedData as? UpazilaUiForecast)
                        _uiState.value = UpazilaForecastUiState.Error(res.message, cached)
                        _isRefreshing.value = false
                    }
                }
            }
        }
    }
}
