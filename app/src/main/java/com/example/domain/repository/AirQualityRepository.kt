package com.example.domain.repository

import com.example.data.model.AirQualityResponse
import kotlinx.coroutines.flow.Flow

interface AirQualityRepository {
    fun getAirQuality(lat: Double, lon: Double, forceRefresh: Boolean = false): Flow<Resource<AirQualityResponse>>
    suspend fun getCachedAirQuality(lat: Double, lon: Double): AirQualityResponse?
}
