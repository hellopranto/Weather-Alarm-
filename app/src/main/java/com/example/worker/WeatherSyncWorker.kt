package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.WeatherCacheEntity
import com.example.data.model.UnifiedWeatherResponse
import com.example.data.remote.ApiClient
import com.example.location.DefaultLocationTracker
import com.squareup.moshi.JsonAdapter
import kotlinx.coroutines.flow.first
import java.util.Locale

class WeatherSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val jsonAdapter: JsonAdapter<UnifiedWeatherResponse> by lazy {
        ApiClient.moshi.adapter(UnifiedWeatherResponse::class.java)
    }

    override suspend fun doWork(): Result {
        return try {
            val preferencesRepo = UserPreferencesRepository(context)
            val locationTracker = DefaultLocationTracker(context)
            val prefs = preferencesRepo.userPreferencesFlow.first()

            var targetLat = prefs.selectedLatitude
            var targetLon = prefs.selectedLongitude
            var targetCity = prefs.selectedCityName

            // Dynamic location check on every run
            if (locationTracker.hasLocationPermission()) {
                val loc = locationTracker.getCurrentLocation()
                if (loc != null) {
                    targetLat = loc.latitude
                    targetLon = loc.longitude
                    targetCity = "" // Empty query allows OpenWeather to resolve the dynamic locality name
                }
            }

            val remoteData = ApiClient.weatherApi.getUnifiedWeather(
                lat = targetLat,
                lon = targetLon,
                name = targetCity
            )

            // Cache in Room Database
            val key = String.format(Locale.US, "%.2f_%.2f", targetLat, targetLon)
            val now = System.currentTimeMillis()
            val database = AppDatabase.getInstance(context)
            val jsonStr = jsonAdapter.toJson(remoteData)

            database.weatherDao().insertWeather(
                WeatherCacheEntity(
                    locationKey = key,
                    cityName = remoteData.location.name,
                    latitude = targetLat,
                    longitude = targetLon,
                    jsonPayload = jsonStr,
                    timestamp = now
                )
            )

            // If dynamic locality found from OpenWeather, keep selected preferences updated
            if (remoteData.location.name.isNotBlank()) {
                preferencesRepo.setSelectedLocation(
                    name = remoteData.location.name,
                    lat = targetLat,
                    lon = targetLon,
                    useGps = prefs.useGpsLocation
                )
            }

            // Post notification if severe alerts exist or notify of fresh sync
            if (remoteData.alerts.isNotEmpty()) {
                WeatherNotificationHelper.showWeatherSyncNotification(context, remoteData)
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
