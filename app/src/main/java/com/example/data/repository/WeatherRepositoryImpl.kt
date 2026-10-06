package com.example.data.repository

import com.example.data.local.WeatherCacheEntity
import com.example.data.local.WeatherDao
import com.example.data.model.UnifiedWeatherResponse
import com.example.data.remote.ApiClient
import com.example.data.remote.WeatherApi
import com.example.domain.repository.Resource
import com.example.domain.repository.WeatherRepository
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.Locale

class WeatherRepositoryImpl(
    private val weatherApi: WeatherApi,
    private val weatherDao: WeatherDao,
    private val moshi: Moshi = ApiClient.moshi
) : WeatherRepository {

    private val responseAdapter = moshi.adapter(UnifiedWeatherResponse::class.java)

    override fun getWeather(
        latitude: Double,
        longitude: Double,
        cityName: String?,
        forceRefresh: Boolean
    ): Flow<Resource<UnifiedWeatherResponse>> = flow {
        emit(Resource.Loading)
        val locationKey = formatKey(latitude, longitude)

        // 1. Fetch from Room cache first if not forced refresh
        val cachedEntity = weatherDao.getCacheByKey(locationKey) ?: weatherDao.getLatestCache()
        if (cachedEntity != null && !forceRefresh) {
            val cachedData = parseJson(cachedEntity.responseJson)
            if (cachedData != null) {
                val ageMillis = System.currentTimeMillis() - cachedEntity.timestampMillis
                // If less than 15 minutes old, emit immediately as fresh cache
                if (ageMillis < 15 * 60 * 1000) {
                    emit(Resource.Success(cachedData, isOfflineCached = true))
                    return@flow
                }
            }
        }

        // 2. Fetch from real production backend
        try {
            val networkResponse = weatherApi.getUnifiedWeather(
                lat = latitude,
                lon = longitude,
                name = cityName?.ifBlank { null }
            )

            // Cache successfully resolved response in Room
            try {
                val jsonStr = responseAdapter.toJson(networkResponse)
                weatherDao.insertCache(
                    WeatherCacheEntity(
                        locationKey = locationKey,
                        latitude = latitude,
                        longitude = longitude,
                        cityName = cityName ?: networkResponse.location.name,
                        responseJson = jsonStr,
                        timestampMillis = System.currentTimeMillis()
                    )
                )
            } catch (_: Exception) {}

            emit(Resource.Success(networkResponse, isOfflineCached = false))
        } catch (e: Exception) {
            // If network fails, fallback to cached data if available
            if (cachedEntity != null) {
                val fallbackData = parseJson(cachedEntity.responseJson)
                if (fallbackData != null) {
                    emit(Resource.Success(fallbackData, isOfflineCached = true))
                    return@flow
                }
            }
            val errorMsg = when {
                e.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                    "ইন্টারনেট সংযোগ পাওয়া যায়নি। অনুগ্রহ করে আপনার নেটওয়ার্ক চেক করুন।"
                e.message?.contains("timeout", ignoreCase = true) == true ->
                    "সার্ভার থেকে সাড়া পেতে বিলম্ব হচ্ছে। কিছুক্ষণ পর আবার চেষ্টা করুন।"
                else ->
                    "আবহাওয়ার তথ্য লোড করা যায়নি। (${e.localizedMessage ?: "অজ্ঞাত সমস্যা"})"
            }
            emit(Resource.Error(errorMsg))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getCachedWeather(latitude: Double, longitude: Double): UnifiedWeatherResponse? {
        val entity = weatherDao.getCacheByKey(formatKey(latitude, longitude)) ?: weatherDao.getLatestCache()
        return entity?.let { parseJson(it.responseJson) }
    }

    private fun formatKey(lat: Double, lon: Double): String {
        return String.format(Locale.US, "%.3f_%.3f", lat, lon)
    }

    private fun parseJson(json: String): UnifiedWeatherResponse? {
        return try {
            responseAdapter.fromJson(json)
        } catch (_: Exception) {
            null
        }
    }
}
