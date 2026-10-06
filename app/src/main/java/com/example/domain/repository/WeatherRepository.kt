package com.example.domain.repository

import com.example.data.model.UnifiedWeatherResponse
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {
    fun getWeather(
        latitude: Double,
        longitude: Double,
        cityName: String? = null,
        forceRefresh: Boolean = false
    ): Flow<Resource<UnifiedWeatherResponse>>

    suspend fun getCachedWeather(latitude: Double, longitude: Double): UnifiedWeatherResponse?
}
