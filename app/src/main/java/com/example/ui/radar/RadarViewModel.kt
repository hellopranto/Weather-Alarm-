package com.example.ui.radar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.RadarFrameItem
import com.example.data.model.RainViewerResponse
import com.example.domain.repository.RadarRepository
import com.example.domain.repository.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RadarUiState(
    val isLoading: Boolean = true,
    val host: String = "https://tilecache.rainviewer.com",
    val frames: List<RadarFrameItem> = emptyList(),
    val currentFrameIndex: Int = 0,
    val isPlaying: Boolean = false,
    val zoomLevel: Int = 6, // Ideal overview for Bangladesh (lat 20-27, lon 88-93)
    val mapCenterLat: Double = 23.8103, // Dhaka center
    val mapCenterLon: Double = 90.4125,
    val userLat: Double = 23.8103,
    val userLon: Double = 90.4125,
    val error: String? = null
) {
    val currentFrame: RadarFrameItem? get() = frames.getOrNull(currentFrameIndex)
    val formattedTime: String get() {
        val f = currentFrame ?: return "--:--"
        return SimpleDateFormat("hh:mm a", Locale.US).format(Date(f.time * 1000))
    }
}

class RadarViewModel(
    private val radarRepository: RadarRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RadarUiState())
    val uiState: StateFlow<RadarUiState> = _uiState.asStateFlow()

    private var animationJob: Job? = null

    init {
        loadRadar()
    }

    fun loadRadar() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val prefs = preferencesRepository.userPreferencesFlow.first()

            radarRepository.getRadarMetadata().collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.value = _uiState.value.copy(isLoading = true)
                    is Resource.Success -> {
                        val response = resource.data
                        val pastFrames = response.allPastFrames
                        val initialIndex = if (pastFrames.isNotEmpty()) pastFrames.size - 1 else 0


                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            host = response.host,
                            frames = pastFrames,
                            currentFrameIndex = initialIndex,
                            userLat = prefs.selectedLatitude,
                            userLon = prefs.selectedLongitude,
                            error = null
                        )
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

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) {
            pauseAnimation()
        } else {
            playAnimation()
        }
    }

    fun playAnimation() {
        _uiState.value = _uiState.value.copy(isPlaying = true)
        animationJob?.cancel()
        animationJob = viewModelScope.launch {
            while (isActive) {
                delay(800)
                val count = _uiState.value.frames.size
                if (count > 0) {
                    val next = (_uiState.value.currentFrameIndex + 1) % count
                    _uiState.value = _uiState.value.copy(currentFrameIndex = next)
                }
            }
        }
    }

    fun pauseAnimation() {
        animationJob?.cancel()
        _uiState.value = _uiState.value.copy(isPlaying = false)
    }

    fun selectFrameIndex(index: Int) {
        val total = _uiState.value.frames.size
        if (index in 0 until total) {
            _uiState.value = _uiState.value.copy(currentFrameIndex = index)
        }
    }

    fun stepForward() {
        pauseAnimation()
        val total = _uiState.value.frames.size
        if (total > 0) {
            val next = (_uiState.value.currentFrameIndex + 1) % total
            _uiState.value = _uiState.value.copy(currentFrameIndex = next)
        }
    }

    fun stepBack() {
        pauseAnimation()
        val total = _uiState.value.frames.size
        if (total > 0) {
            val prev = if (_uiState.value.currentFrameIndex - 1 < 0) total - 1 else _uiState.value.currentFrameIndex - 1
            _uiState.value = _uiState.value.copy(currentFrameIndex = prev)
        }
    }

    fun zoomIn() {
        if (_uiState.value.zoomLevel < 8) {
            _uiState.value = _uiState.value.copy(zoomLevel = _uiState.value.zoomLevel + 1)
        }
    }

    fun zoomOut() {
        if (_uiState.value.zoomLevel > 4) {
            _uiState.value = _uiState.value.copy(zoomLevel = _uiState.value.zoomLevel - 1)
        }
    }

    fun centerOnUserLocation() {
        _uiState.value = _uiState.value.copy(
            mapCenterLat = _uiState.value.userLat,
            mapCenterLon = _uiState.value.userLon
        )
    }

    fun getRadarTileUrl(z: Int, x: Int, y: Int): String {
        val state = _uiState.value
        val frame = state.currentFrame ?: return ""
        return radarRepository.getTileUrl(
            host = state.host,
            path = frame.path,
            z = z,
            x = x,
            y = y,
            colorScheme = 2, // Universal Blue
            smooth = true
        )
    }
}
