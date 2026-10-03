package com.example.data.remote

import com.example.data.model.BmdObservationsEnvelope
import com.example.data.model.BmdWarningsEnvelope
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface BmdApi {
    @GET("api/bmd/observations")
    suspend fun getObservations(
        @Query("station") station: String? = null
    ): BmdObservationsEnvelope

    @GET("api/bmd/warnings")
    suspend fun getWarnings(): BmdWarningsEnvelope

    @GET
    suspend fun getDirectBmdObservations(
        @Url directUrl: String
    ): BmdObservationsEnvelope
}
