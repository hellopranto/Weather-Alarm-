package com.example.domain.repository

import com.example.data.model.UnifiedWeatherResponse
import kotlinx.coroutines.flow.Flow

sealed interface Resource<out T> {
    data class Success<T>(val data: T, val isOfflineCached: Boolean = false) : Resource<T>
    data class Error(val message: String, val cachedData: Any? = null) : Resource<Nothing>
    data object Loading : Resource<Nothing>
}

interface WeatherRepository {
    fun getWeather(lat: Double, lon: Double, cityName: String, forceRefresh: Boolean = false): Flow<Resource<UnifiedWeatherResponse>>
    suspend fun getCachedWeather(lat: Double, lon: Double): UnifiedWeatherResponse?
}
