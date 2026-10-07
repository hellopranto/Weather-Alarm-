package com.example.data.remote

import com.example.data.model.AirQualityResponse
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface AirQualityApi {
    @GET("air-quality")
    suspend fun getAirQuality(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): AirQualityResponse

    @GET("api/air-quality")
    suspend fun getApiAirQuality(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): AirQualityResponse

    @GET
    suspend fun getDirectOpenMeteoAirQuality(
        @Url url: String
    ): ResponseBody
}
