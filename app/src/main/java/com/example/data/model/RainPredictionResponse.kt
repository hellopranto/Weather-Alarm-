package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RainPredictionResponse(
    @Json(name = "location") val location: RainLocationModel = RainLocationModel(),
    @Json(name = "current") val current: CurrentRainModel = CurrentRainModel(),
    @Json(name = "summary24h") val summary24h: RainSummary24hModel = RainSummary24hModel(),
    @Json(name = "hourly") val hourly: List<HourlyRainModel> = emptyList(),
    @Json(name = "daily") val daily: List<DailyRainModel> = emptyList(),
    @Json(name = "source") val source: String = "Open-Meteo High-Resolution Weather Model",
    @Json(name = "updatedAt") val updatedAt: String = ""
)

@JsonClass(generateAdapter = true)
data class RainLocationModel(
    @Json(name = "latitude") val latitude: Double = 23.8103,
    @Json(name = "longitude") val longitude: Double = 90.4125,
    @Json(name = "timezone") val timezone: String = "Asia/Dhaka"
)

@JsonClass(generateAdapter = true)
data class CurrentRainModel(
    @Json(name = "precipitationMm") val precipitationMm: Double = 0.0,
    @Json(name = "rainMm") val rainMm: Double = 0.0,
    @Json(name = "showersMm") val showersMm: Double = 0.0,
    @Json(name = "weatherCode") val weatherCode: Int = 0,
    @Json(name = "condition") val condition: String = "Clear",
    @Json(name = "temperature") val temperature: Double = 28.0,
    @Json(name = "humidity") val humidity: Int = 70,
    @Json(name = "windSpeed") val windSpeed: Double = 0.0,
    @Json(name = "windDirection") val windDirection: Int = 0
)

@JsonClass(generateAdapter = true)
data class RainSummary24hModel(
    @Json(name = "maxRainProbability") val maxRainProbability: Int = 0,
    @Json(name = "totalExpectedRainfallMm") val totalExpectedRainfallMm: Double = 0.0,
    @Json(name = "rainExpected") val rainExpected: Boolean = false,
    @Json(name = "intensity") val intensity: String = "None", // None, Light, Moderate, Heavy, Violent
    @Json(name = "predictedRainPeriods") val predictedRainPeriods: List<PredictedRainPeriodModel> = emptyList(),
    @Json(name = "advisoryEn") val advisoryEn: String = "",
    @Json(name = "advisoryBn") val advisoryBn: String = ""
)

@JsonClass(generateAdapter = true)
data class PredictedRainPeriodModel(
    @Json(name = "start") val start: String = "",
    @Json(name = "end") val end: String = "",
    @Json(name = "expectedRainfallMm") val expectedRainfallMm: Double = 0.0,
    @Json(name = "maxProbability") val maxProbability: Int = 0
)

@JsonClass(generateAdapter = true)
data class HourlyRainModel(
    @Json(name = "time") val time: String = "",
    @Json(name = "precipitationProbability") val precipitationProbability: Int = 0,
    @Json(name = "precipitationMm") val precipitationMm: Double = 0.0,
    @Json(name = "rainMm") val rainMm: Double = 0.0,
    @Json(name = "showersMm") val showersMm: Double = 0.0,
    @Json(name = "weatherCode") val weatherCode: Int = 0,
    @Json(name = "condition") val condition: String = "Clear",
    @Json(name = "temperature") val temperature: Double = 28.0,
    @Json(name = "humidity") val humidity: Int = 70,
    @Json(name = "windSpeed") val windSpeed: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class DailyRainModel(
    @Json(name = "date") val date: String = "",
    @Json(name = "maxRainProbability") val maxRainProbability: Int = 0,
    @Json(name = "totalRainfallMm") val totalRainfallMm: Double = 0.0,
    @Json(name = "weatherCode") val weatherCode: Int = 0,
    @Json(name = "condition") val condition: String = "Clear",
    @Json(name = "tempMax") val tempMax: Double = 30.0,
    @Json(name = "tempMin") val tempMin: Double = 22.0
)
