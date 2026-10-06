package com.example.data.remote

import com.example.data.model.UnifiedWeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("api/weather")
    suspend fun getUnifiedWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("name") name: String? = null
    ): UnifiedWeatherResponse
}
