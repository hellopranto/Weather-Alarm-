package com.example.domain.repository

import com.example.data.model.RainPredictionResponse
import kotlinx.coroutines.flow.Flow

interface RainRepository {
    fun getRainPrediction(lat: Double, lon: Double, forceRefresh: Boolean = false): Flow<Resource<RainPredictionResponse>>
    suspend fun getCachedRainPrediction(lat: Double, lon: Double): RainPredictionResponse?
}
