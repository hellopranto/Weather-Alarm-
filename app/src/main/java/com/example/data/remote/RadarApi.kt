package com.example.data.remote

import com.example.data.model.RainViewerResponse
import retrofit2.http.GET
import retrofit2.http.Url

interface RadarApi {
    @GET("api/radar")
    suspend fun getBackendRadarMaps(): RainViewerResponse

    @GET("https://api.rainviewer.com/public/weather-maps.json")
    suspend fun getRainViewerPublicMaps(): RainViewerResponse

    @GET
    suspend fun getRadarMapsFromUrl(
        @Url url: String
    ): RainViewerResponse
}
