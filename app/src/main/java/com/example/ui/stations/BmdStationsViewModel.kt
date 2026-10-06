package com.example.ui.stations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.BmdStationDto
import com.example.domain.repository.BmdWeatherRepository
import com.example.domain.repository.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class StationSortOption {
    DEFAULT,
    RAINFALL_DESC,
    TEMP_DESC,
    TEMP_ASC,
    DISTANCE
}

data class StationsUiState(
    val isLoading: Boolean = true,
    val stations: List<BmdStationDto> = emptyList(),
    val filteredStations: List<BmdStationDto> = emptyList(),
    val selectedDivision: String = "সকল",
    val searchQuery: String = "",
    val sortOption: StationSortOption = StationSortOption.DEFAULT,
    val userLat: Double = 23.8103,
    val userLon: Double = 90.4125,
    val error: String? = null
)

class BmdStationsViewModel(
    private val bmdRepository: BmdWeatherRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StationsUiState())
    val uiState: StateFlow<StationsUiState> = _uiState.asStateFlow()

    init {
        loadStations()
    }

    fun loadStations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val prefs = preferencesRepository.userPreferencesFlow.first()
            val userLat = if (prefs.selectedLatitude != 0.0) prefs.selectedLatitude else 23.8103
            val userLon = if (prefs.selectedLongitude != 0.0) prefs.selectedLongitude else 90.4125

            bmdRepository.getStationObservations().collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.value = _uiState.value.copy(isLoading = true)
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            stations = resource.data,
                            userLat = userLat,
                            userLon = userLon,
                            error = null
                        )
                        applyFilters()
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = resource.message
                        )
                    }
                }
            }
        }
    }

    fun selectDivision(division: String) {
        _uiState.value = _uiState.value.copy(selectedDivision = division)
        applyFilters()
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun setSortOption(option: StationSortOption) {
        _uiState.value = _uiState.value.copy(sortOption = option)
        applyFilters()
    }

    private fun applyFilters() {
        val currentState = _uiState.value
        val all = currentState.stations
        val query = currentState.searchQuery.trim()
        val div = currentState.selectedDivision

        var list = all.filter { station ->
            val matchDiv = (div == "সকল" || station.division.equals(div, ignoreCase = true) ||
                    translateDivision(station.division) == div)
            val matchQuery = query.isBlank() ||
                    station.stationName.contains(query, ignoreCase = true) ||
                    station.division.contains(query, ignoreCase = true)
            matchDiv && matchQuery
        }

        list = when (currentState.sortOption) {
            StationSortOption.RAINFALL_DESC -> list.sortedByDescending { it.rainfall24hMm }
            StationSortOption.TEMP_DESC -> list.sortedByDescending { it.temperatureC }
            StationSortOption.TEMP_ASC -> list.sortedBy { it.temperatureC }
            StationSortOption.DISTANCE -> list.sortedBy {
                distanceBetween(currentState.userLat, currentState.userLon, it.latitude, it.longitude)
            }
            StationSortOption.DEFAULT -> list
        }

        _uiState.value = _uiState.value.copy(filteredStations = list)
    }

    private fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    companion object {
        fun translateDivision(div: String): String {
            return when (div.lowercase()) {
                "dhaka" -> "ঢাকা"
                "chattogram", "chittagong" -> "চট্টগ্রাম"
                "sylhet" -> "সিলেট"
                "rajshahi" -> "রাজশাহী"
                "khulna" -> "খুলনা"
                "barishal", "barisal" -> "বরিশাল"
                "rangpur" -> "রংপুর"
                "mymensingh" -> "ময়মনসিংহ"
                else -> div
            }
        }
    }
}
