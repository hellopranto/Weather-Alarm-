package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RainPredictionResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "location") val location: RainLocationModel = RainLocationModel(),
    @Json(name = "prediction") val prediction: RainPredictionMetricsModel? = null,
    @Json(name = "rain") val rain: RainDetailsModel? = null,
    @Json(name = "radar") val radar: RadarNowcastModel? = null,
    @Json(name = "timeline") val timeline: List<RainTimelinePointModel> = emptyList(),
    @Json(name = "confidence") val confidence: String? = null,
    @Json(name = "confidenceScore") val confidenceScore: Int? = null,
    @Json(name = "sources") val sources: List<String> = emptyList(),
    @Json(name = "updatedAt") val updatedAt: String = "",
    @Json(name = "current") val current: CurrentRainModel = CurrentRainModel(),
    @Json(name = "summary24h") val summary24h: RainSummary24hModel = RainSummary24hModel(),
    @Json(name = "hourly") val hourly: List<HourlyRainModel> = emptyList(),
    @Json(name = "daily") val daily: List<DailyRainModel> = emptyList(),
    @Json(name = "heavyRainWarning") val heavyRainWarning: HeavyRainWarningModel? = null,
    @Json(name = "bmdStation") val bmdStation: String? = null,
    @Json(name = "bmdDistanceKm") val bmdDistanceKm: Double? = null,
    @Json(name = "source") val source: String = "Bangladesh Meteorological Department + Doppler Radar"
)

@JsonClass(generateAdapter = true)
data class RainPredictionMetricsModel(
    @Json(name = "next15Minutes") val next15Minutes: Int = 0,
    @Json(name = "next30Minutes") val next30Minutes: Int = 0,
    @Json(name = "next1Hour") val next1Hour: Int = 0,
    @Json(name = "next2Hours") val next2Hours: Int = 0,
    @Json(name = "next3Hours") val next3Hours: Int = 0,
    @Json(name = "next6Hours") val next6Hours: Int = 0,
    @Json(name = "next24Hours") val next24Hours: Int = 0
)

@JsonClass(generateAdapter = true)
data class RainDetailsModel(
    @Json(name = "expected") val expected: Boolean = false,
    @Json(name = "startInMinutes") val startInMinutes: Int? = null,
    @Json(name = "durationMinutes") val durationMinutes: Int? = null,
    @Json(name = "intensity") val intensity: String = "None", // None, Light, Moderate, Heavy, Violent
    @Json(name = "intensityBn") val intensityBn: String = "বৃষ্টি নেই", // বৃষ্টি নেই, হালকা, মাঝারি, ভারী, অতি ভারী
    @Json(name = "rainfallAmount") val rainfallAmount: Double? = null,
    @Json(name = "summaryBn") val summaryBn: String? = null
)

@JsonClass(generateAdapter = true)
data class RadarNowcastModel(
    @Json(name = "available") val available: Boolean = false,
    @Json(name = "approaching") val approaching: Boolean = false,
    @Json(name = "direction") val direction: String? = null,
    @Json(name = "speed") val speed: String? = null,
    @Json(name = "statusTextBn") val statusTextBn: String = "কোনো উল্লেখযোগ্য বৃষ্টির সিগন্যাল নেই",
    @Json(name = "radarMessageBn") val radarMessageBn: String? = null
)

@JsonClass(generateAdapter = true)
data class RainTimelinePointModel(
    @Json(name = "timeLabel") val timeLabel: String = "",
    @Json(name = "probability") val probability: Int = 0,
    @Json(name = "intensityBn") val intensityBn: String = "বৃষ্টি নেই",
    @Json(name = "rainfallMm") val rainfallMm: Double? = null,
    @Json(name = "weatherCode") val weatherCode: Int = 800
)

@JsonClass(generateAdapter = true)
data class HeavyRainWarningModel(
    @Json(name = "isWarningActive") val isWarningActive: Boolean = false,
    @Json(name = "expectedStart") val expectedStart: String? = null,
    @Json(name = "expectedDuration") val expectedDuration: String? = null,
    @Json(name = "intensity") val intensity: String = "ভারী",
    @Json(name = "expectedRainfallAmount") val expectedRainfallAmount: String? = null,
    @Json(name = "bmdWarning") val bmdWarning: String? = null
)

@JsonClass(generateAdapter = true)
data class RainLocationModel(
    @Json(name = "latitude") val latitude: Double = 23.8103,
    @Json(name = "longitude") val longitude: Double = 90.4125,
    @Json(name = "name") val name: String = "",
    @Json(name = "village") val village: String? = null,
    @Json(name = "upazila") val upazila: String? = null,
    @Json(name = "district") val district: String? = null,
    @Json(name = "division") val division: String? = null,
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
    @Json(name = "intensity") val intensity: String = "None",
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
