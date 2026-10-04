package com.example.data.remote

import com.example.data.model.UnifiedWeatherResponse
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface WeatherApi {
    @GET("weather")
    suspend fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("name") name: String? = null
    ): UnifiedWeatherResponse

    @GET("api/weather")
    suspend fun getUnifiedWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("name") name: String? = null
    ): UnifiedWeatherResponse

    @GET
    suspend fun getDirectOpenWeatherCurrent(
        @Url url: String,
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String = "metric",
        @Query("appid") appid: String
    ): ResponseBody

    @GET
    suspend fun getDirectOpenWeatherForecast(
        @Url url: String,
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String = "metric",
        @Query("appid") appid: String
    ): ResponseBody
}
