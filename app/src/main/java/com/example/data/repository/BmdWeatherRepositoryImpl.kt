package com.example.data.repository

import com.example.data.model.BmdStationDto
import com.example.data.model.BmdWarningDto
import com.example.data.remote.BmdApi
import com.example.domain.repository.BmdWeatherRepository
import com.example.domain.repository.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class BmdWeatherRepositoryImpl(
    private val bmdApi: BmdApi
) : BmdWeatherRepository {

    override fun getStationObservations(stationQuery: String?): Flow<Resource<List<BmdStationDto>>> = flow {
        emit(Resource.Loading)
        try {
            val response = bmdApi.getObservations(stationQuery)
            val stations = if (!stationQuery.isNullOrBlank()) {
                response.stations.filter {
                    it.stationName.contains(stationQuery, ignoreCase = true) ||
                    it.division.contains(stationQuery, ignoreCase = true)
                }
            } else {
                response.stations
            }
            emit(Resource.Success(stations))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "BMD স্টেশন পর্যবেক্ষণ তথ্য পাওয়া যায়নি।"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getWeatherWarnings(): Flow<Resource<List<BmdWarningDto>>> = flow {
        emit(Resource.Loading)
        try {
            val response = bmdApi.getWarnings()
            emit(Resource.Success(response.warnings))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "BMD সতর্কতা বুলেটিন পাওয়া যায়নি।"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getObservationForCoordinates(lat: Double, lon: Double): BmdStationDto? {
        return try {
            val response = bmdApi.getObservations(null)
            response.stations.minByOrNull { station ->
                distanceBetween(lat, lon, station.latitude, station.longitude)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
