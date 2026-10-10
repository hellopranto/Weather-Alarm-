package com.example.domain.repository

import com.example.data.model.UpazilaBbsRecord
import com.example.data.model.UpazilaUiForecast
import kotlinx.coroutines.flow.Flow

interface UpazilaForecastRepository {
    suspend fun getUpazilas(): List<UpazilaBbsRecord>
    suspend fun findNearestUpazila(lat: Double, lon: Double): UpazilaBbsRecord?
    suspend fun findUpazilaByPcode(pcode: String): UpazilaBbsRecord?

    fun getUpazilaForecast(
        pcode: String,
        source: String = "BMDWRF",
        forceRefresh: Boolean = false
    ): Flow<Resource<UpazilaUiForecast>>

    fun getForecastByDate(
        pcode: String,
        fdate: String,
        source: String = "BMDWRF"
    ): Flow<Resource<UpazilaUiForecast>>
}
