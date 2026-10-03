package com.example.domain.repository

import com.example.data.model.BmdStationDto
import com.example.data.model.BmdWarningDto
import kotlinx.coroutines.flow.Flow

interface BmdWeatherRepository {
    fun getStationObservations(stationQuery: String? = null): Flow<Resource<List<BmdStationDto>>>
    fun getWeatherWarnings(): Flow<Resource<List<BmdWarningDto>>>
    suspend fun getObservationForCoordinates(lat: Double, lon: Double): BmdStationDto?
}
