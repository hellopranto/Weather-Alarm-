package com.example.data.remote

import com.example.data.model.UpazilaForecastEnvelope
import retrofit2.http.GET
import retrofit2.http.Query

interface UpazilaForecastApi {

    /**
     * Recent Daily Forecast
     * e.g. /upazila_forecast_recent?SOURCE=BMDWRF&PARAM=temp&PARAM=rf&PARAM=rh&PARAM=windspd&PCODE=202224
     */
    @GET("upazila_forecast_recent")
    suspend fun getRecentForecast(
        @Query("SOURCE") source: String,
        @Query("PARAM") params: List<String>,
        @Query("PCODE") pcode: String
    ): UpazilaForecastEnvelope

    /**
     * 4-day Sub-daily Time-steps Forecast
     * e.g. /upazila_forecast_steps_recent?SOURCE=BMDWRF&PARAM=temp&PARAM=rf&PARAM=rh&PARAM=windspd&PCODE=202224
     */
    @GET("upazila_forecast_steps_recent")
    suspend fun getStepsForecast(
        @Query("SOURCE") source: String,
        @Query("PARAM") params: List<String>,
        @Query("PCODE") pcode: String
    ): UpazilaForecastEnvelope

    /**
     * Forecast by generation date
     * e.g. /upazila_forecast_date?SOURCE=BMDWRF&FDATE=20261010&PARAM=temp&PCODE=202224
     */
    @GET("upazila_forecast_date")
    suspend fun getForecastByDate(
        @Query("SOURCE") source: String,
        @Query("FDATE") fdate: String,
        @Query("PARAM") params: List<String>,
        @Query("PCODE") pcode: String
    ): UpazilaForecastEnvelope
}
