package com.example.data.repository

import com.example.data.local.WeatherCacheEntity
import com.example.data.local.WeatherDao
import com.example.data.model.UnifiedWeatherResponse
import com.example.data.remote.ApiClient
import com.example.data.remote.WeatherApi
import com.example.domain.repository.Resource
import com.example.domain.repository.WeatherRepository
import com.squareup.moshi.JsonAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.Locale

class WeatherRepositoryImpl(
    private val weatherApi: WeatherApi,
    private val weatherDao: WeatherDao
) : WeatherRepository {

    private val jsonAdapter: JsonAdapter<UnifiedWeatherResponse> by lazy {
        ApiClient.moshi.adapter(UnifiedWeatherResponse::class.java)
    }

    override fun getWeather(
        lat: Double,
        lon: Double,
        cityName: String,
        forceRefresh: Boolean
    ): Flow<Resource<UnifiedWeatherResponse>> = flow {
        emit(Resource.Loading)

        val key = String.format(Locale.US, "%.2f_%.2f", lat, lon)
        val cached = weatherDao.getWeatherByKey(key)

        var cachedResponse: UnifiedWeatherResponse? = null
        if (cached != null) {
            try {
                cachedResponse = jsonAdapter.fromJson(cached.jsonPayload)
            } catch (_: Exception) {}
        }

        // If not force refreshing and cached data is younger than 5 minutes, emit immediately
        val now = System.currentTimeMillis()
        if (!forceRefresh && cached != null && cachedResponse != null && (now - cached.timestamp < 300_000)) {
            emit(Resource.Success(cachedResponse, isOfflineCached = false))
            return@flow
        }

        // Emit cached data first while refreshing in background if available
        if (cachedResponse != null && !forceRefresh) {
            emit(Resource.Success(cachedResponse, isOfflineCached = true))
        }

        try {
            // Attempt to call unified /api/weather endpoint with fallback to /weather
            val queryName = if (cityName.isNotBlank()) cityName else null
            val remoteData = try {
                weatherApi.getUnifiedWeather(lat = lat, lon = lon, name = queryName)
            } catch (_: Exception) {
                weatherApi.getWeather(lat = lat, lon = lon, name = queryName)
            }

            // Save real data to room cache
            val jsonStr = jsonAdapter.toJson(remoteData)
            weatherDao.insertWeather(
                WeatherCacheEntity(
                    locationKey = key,
                    cityName = remoteData.location.displayName ?: remoteData.location.name.ifBlank { cityName },
                    latitude = lat,
                    longitude = lon,
                    jsonPayload = jsonStr,
                    timestamp = now
                )
            )
            emit(Resource.Success(remoteData, isOfflineCached = false))
        } catch (e: Exception) {

            // If network fails, serve authentic cached data if available; NEVER fabricate fake weather
            if (cachedResponse != null) {
                emit(Resource.Success(cachedResponse, isOfflineCached = true))
            } else {
                emit(Resource.Error(e.message ?: "BMD থেকে সর্বশেষ তথ্য পাওয়া যাচ্ছে না।"))
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getCachedWeather(lat: Double, lon: Double): UnifiedWeatherResponse? {
        val key = String.format(Locale.US, "%.2f_%.2f", lat, lon)
        val cached = weatherDao.getWeatherByKey(key) ?: return null
        return try {
            jsonAdapter.fromJson(cached.jsonPayload)
        } catch (_: Exception) {
            null
        }
    }
}
