package com.example.data.remote

import com.example.data.model.RainPredictionResponse
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface RainApi {
    @GET("rain-prediction")
    suspend fun getRainPrediction(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): RainPredictionResponse

    @GET("api/rain-prediction")
    suspend fun getApiRainPrediction(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): RainPredictionResponse

    @GET
    suspend fun getDirectOpenMeteoForecast(
        @Url url: String
    ): ResponseBody
}
