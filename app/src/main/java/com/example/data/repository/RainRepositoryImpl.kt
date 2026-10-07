package com.example.data.repository

import android.content.Context
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.RainPredictionResponse
import com.example.data.remote.ApiClient
import com.example.data.remote.RadarApi
import com.example.data.remote.RainApi
import com.example.data.remote.WeatherApi
import com.example.domain.RainPredictionEngine
import com.example.domain.repository.RainRepository
import com.example.domain.repository.Resource
import com.example.notification.RainNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class RainRepositoryImpl(
    private val rainApi: RainApi = ApiClient.rainApi,
    private val weatherApi: WeatherApi = ApiClient.weatherApi,
    private val radarApi: RadarApi = ApiClient.radarApi,
    private val userPreferencesRepository: UserPreferencesRepository? = null,
    private val appContext: Context? = null
) : RainRepository {

    private val inMemoryCache = ConcurrentHashMap<String, Pair<RainPredictionResponse, Long>>()
    private val CACHE_EXPIRY_MS = 10 * 60 * 1000 // 10 minutes

    override fun getRainPrediction(
        lat: Double,
        lon: Double,
        forceRefresh: Boolean
    ): Flow<Resource<RainPredictionResponse>> = flow {
        emit(Resource.Loading)

        val key = String.format(Locale.US, "%.3f_%.3f", lat, lon)
        val now = System.currentTimeMillis()
        val cached = inMemoryCache[key]

        if (!forceRefresh && cached != null && (now - cached.second) < CACHE_EXPIRY_MS) {
            emit(Resource.Success(cached.first, isOfflineCached = true))
            return@flow
        }

        try {
            // 1. Try dedicated Backend endpoint /api/rain-prediction first
            val remoteData = try {
                val directResponse = try {
                    rainApi.getApiRainPrediction(lat, lon)
                } catch (_: Exception) {
                    rainApi.getRainPrediction(lat, lon)
                }
                // Verify the response has data
                if (directResponse.timeline.isNotEmpty() || directResponse.prediction != null) {
                    directResponse
                } else {
                    synthesizeFromLiveSources(lat, lon)
                }
            } catch (_: Exception) {
                // If /api/rain-prediction is not deployed yet on Vercel, synthesize from real /api/weather + live Radar
                synthesizeFromLiveSources(lat, lon)
            }

            inMemoryCache[key] = Pair(remoteData, now)

            // Trigger smart rain notification if applicable
            appContext?.let { ctx ->
                val prefs = userPreferencesRepository?.userPreferencesFlow?.firstOrNull()
                val notificationsEnabled = prefs?.notificationsEnabled ?: true
                RainNotificationHelper.checkAndNotify(ctx, remoteData, notificationsEnabled)
            }

            emit(Resource.Success(remoteData, isOfflineCached = false))
        } catch (e: Exception) {
            if (cached != null) {
                emit(Resource.Success(cached.first, isOfflineCached = true))
            } else {
                emit(Resource.Error("বৃষ্টিপাতের পূর্বাভাস পেতে ব্যর্থ হয়েছে: ${e.localizedMessage ?: "নেটওয়ার্ক সমস্যা"}"))
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getCachedRainPrediction(lat: Double, lon: Double): RainPredictionResponse? {
        val key = String.format(Locale.US, "%.3f_%.3f", lat, lon)
        return inMemoryCache[key]?.first
    }

    private suspend fun synthesizeFromLiveSources(lat: Double, lon: Double): RainPredictionResponse {
        val weather = weatherApi.getUnifiedWeather(lat = lat, lon = lon, name = null)
        val radar = try {
            radarApi.getBackendRadarMaps()
        } catch (_: Exception) {
            try {
                radarApi.getRainViewerPublicMaps()
            } catch (_: Exception) {
                null
            }
        }

        val locationName = weather.location.displayName
            ?: listOfNotNull(weather.location.upazila, weather.location.district).joinToString(", ")
            .ifBlank { weather.location.name }

        return RainPredictionEngine.generatePrediction(
            weather = weather,
            radar = radar,
            lat = lat,
            lon = lon,
            cityName = locationName
        )
    }
}
