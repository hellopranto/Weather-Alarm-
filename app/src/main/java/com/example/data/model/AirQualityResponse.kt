package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AirQualityResponse(
    @Json(name = "location") val location: AirQualityLocationModel = AirQualityLocationModel(),
    @Json(name = "current") val current: AirQualityCurrentModel = AirQualityCurrentModel(),
    @Json(name = "units") val units: AirQualityUnitsModel = AirQualityUnitsModel(),
    @Json(name = "summary24h") val summary24h: AirQualitySummary24hModel = AirQualitySummary24hModel(),
    @Json(name = "healthGuidance") val healthGuidance: AirQualityHealthGuidanceModel = AirQualityHealthGuidanceModel(),
    @Json(name = "hourly") val hourly: List<HourlyAirQualityModel> = emptyList(),
    @Json(name = "daily") val daily: List<DailyAirQualityModel> = emptyList(),
    @Json(name = "isModeled") val isModeled: Boolean = true,
    @Json(name = "source") val source: String = "Open-Meteo Global Air Quality Model",
    @Json(name = "updatedAt") val updatedAt: String = ""
)

@JsonClass(generateAdapter = true)
data class AirQualityLocationModel(
    @Json(name = "latitude") val latitude: Double = 23.8103,
    @Json(name = "longitude") val longitude: Double = 90.4125,
    @Json(name = "timezone") val timezone: String = "Asia/Dhaka"
)

@JsonClass(generateAdapter = true)
data class AirQualityCurrentModel(
    @Json(name = "usAqi") val usAqi: Int = 50,
    @Json(name = "usAqiCategory") val usAqiCategory: String = "Good",
    @Json(name = "europeanAqi") val europeanAqi: Int = 25,
    @Json(name = "europeanAqiCategory") val europeanAqiCategory: String = "Good",
    @Json(name = "pm2_5") val pm2_5: Double = 25.0,
    @Json(name = "pm10") val pm10: Double = 45.0,
    @Json(name = "nitrogenDioxide") val nitrogenDioxide: Double = 15.0,
    @Json(name = "ozone") val ozone: Double = 30.0,
    @Json(name = "sulphurDioxide") val sulphurDioxide: Double = 8.0,
    @Json(name = "carbonMonoxide") val carbonMonoxide: Double = 250.0
)

@JsonClass(generateAdapter = true)
data class AirQualityUnitsModel(
    @Json(name = "aqi") val aqi: String = "index",
    @Json(name = "pm2_5") val pm2_5: String = "µg/m³",
    @Json(name = "pm10") val pm10: String = "µg/m³",
    @Json(name = "nitrogenDioxide") val nitrogenDioxide: String = "µg/m³",
    @Json(name = "ozone") val ozone: String = "µg/m³",
    @Json(name = "sulphurDioxide") val sulphurDioxide: String = "µg/m³",
    @Json(name = "carbonMonoxide") val carbonMonoxide: String = "µg/m³"
)

@JsonClass(generateAdapter = true)
data class AirQualitySummary24hModel(
    @Json(name = "peakUsAqi") val peakUsAqi: Int = 50,
    @Json(name = "peakTime") val peakTime: String = "",
    @Json(name = "peakCategory") val peakCategory: String = "Good",
    @Json(name = "averagePm2_5") val averagePm2_5: Double = 25.0
)

@JsonClass(generateAdapter = true)
data class AirQualityHealthGuidanceModel(
    @Json(name = "generalEn") val generalEn: String = "",
    @Json(name = "generalBn") val generalBn: String = "",
    @Json(name = "sensitiveGroupsEn") val sensitiveGroupsEn: String = "",
    @Json(name = "sensitiveGroupsBn") val sensitiveGroupsBn: String = "",
    @Json(name = "outdoorActivitiesEn") val outdoorActivitiesEn: String = "",
    @Json(name = "outdoorActivitiesBn") val outdoorActivitiesBn: String = "",
    @Json(name = "childrenAndElderlyEn") val childrenAndElderlyEn: String = "",
    @Json(name = "childrenAndElderlyBn") val childrenAndElderlyBn: String = ""
)

@JsonClass(generateAdapter = true)
data class HourlyAirQualityModel(
    @Json(name = "time") val time: String = "",
    @Json(name = "usAqi") val usAqi: Int = 50,
    @Json(name = "europeanAqi") val europeanAqi: Int = 25,
    @Json(name = "pm2_5") val pm2_5: Double = 0.0,
    @Json(name = "pm10") val pm10: Double = 0.0,
    @Json(name = "nitrogenDioxide") val nitrogenDioxide: Double = 0.0,
    @Json(name = "ozone") val ozone: Double = 0.0,
    @Json(name = "sulphurDioxide") val sulphurDioxide: Double = 0.0,
    @Json(name = "carbonMonoxide") val carbonMonoxide: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class DailyAirQualityModel(
    @Json(name = "date") val date: String = "",
    @Json(name = "maxUsAqi") val maxUsAqi: Int = 50,
    @Json(name = "maxEuropeanAqi") val maxEuropeanAqi: Int = 25,
    @Json(name = "avgPm2_5") val avgPm2_5: Double = 0.0,
    @Json(name = "category") val category: String = "Good"
)
