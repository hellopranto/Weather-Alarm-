package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UnifiedWeatherResponse(
    @Json(name = "location") val location: LocationModel,
    @Json(name = "current") val current: CurrentWeatherModel,
    @Json(name = "weather") val weather: WeatherSimpleModel? = null,
    @Json(name = "units") val units: WeatherUnitsModel? = null,
    @Json(name = "hourly") val hourly: List<HourlyForecastModel> = emptyList(),
    @Json(name = "daily") val daily: List<DailyForecastModel> = emptyList(),
    @Json(name = "alerts") val alerts: List<WeatherAlertModel> = emptyList(),
    @Json(name = "bmd") val bmd: BmdStatusModel? = null,
    @Json(name = "radar") val radar: RadarMetadataModel? = null,
    @Json(name = "updatedAt") val updatedAt: String = ""
)

@JsonClass(generateAdapter = true)
data class LocationModel(
    @Json(name = "name") val name: String,
    @Json(name = "district") val district: String? = null,
    @Json(name = "country") val country: String = "Bangladesh",
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "village") val village: String? = null,
    @Json(name = "union") val union: String? = null,
    @Json(name = "upazila") val upazila: String? = null,
    @Json(name = "division") val division: String? = null,
    @Json(name = "displayName") val displayName: String? = null
)

@JsonClass(generateAdapter = true)
data class WeatherSimpleModel(
    @Json(name = "temperature") val temperature: Double? = null,
    @Json(name = "feelsLike") val feelsLike: Double? = null,
    @Json(name = "humidity") val humidity: Int? = null,
    @Json(name = "windSpeed") val windSpeed: Double? = null,
    @Json(name = "precipitation") val precipitation: Double? = null,
    @Json(name = "condition") val condition: String? = null,
    @Json(name = "description") val description: String? = null
)

@JsonClass(generateAdapter = true)
data class WeatherUnitsModel(
    @Json(name = "temperature") val temperature: String = "°C",
    @Json(name = "speed") val speed: String = "km/h",
    @Json(name = "precipitation") val precipitation: String = "mm"
)

@JsonClass(generateAdapter = true)
data class CurrentWeatherModel(
    @Json(name = "temperature") val temperature: Double,
    @Json(name = "feelsLike") val feelsLike: Double,
    @Json(name = "tempMin") val tempMin: Double? = null,
    @Json(name = "tempMax") val tempMax: Double? = null,
    @Json(name = "humidity") val humidity: Int,
    @Json(name = "windSpeed") val windSpeed: Double, // in km/h
    @Json(name = "windDirection") val windDirection: Int = 0, // in degrees
    @Json(name = "pressure") val pressure: Int = 1010, // in hPa
    @Json(name = "visibility") val visibility: Int = 10000, // in meters
    @Json(name = "condition") val condition: String = "Clear",
    @Json(name = "description") val description: String = "",
    @Json(name = "weatherCode") val weatherCode: Int = 800,
    @Json(name = "icon") val icon: String = "01d",
    @Json(name = "sunrise") val sunrise: String = "05:50 AM",
    @Json(name = "sunset") val sunset: String = "05:45 PM",
    @Json(name = "rainProbability") val rainProbability: Int = 0,
    @Json(name = "precipitationMm") val precipitationMm: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class HourlyForecastModel(
    @Json(name = "timestamp") val timestamp: Long,
    @Json(name = "timeString") val timeString: String,
    @Json(name = "temperature") val temperature: Double,
    @Json(name = "feelsLike") val feelsLike: Double = 0.0,
    @Json(name = "humidity") val humidity: Int = 70,
    @Json(name = "pressure") val pressure: Int = 1008,
    @Json(name = "condition") val condition: String = "Clear",
    @Json(name = "description") val description: String = "",
    @Json(name = "icon") val icon: String = "01d",
    @Json(name = "weatherCode") val weatherCode: Int = 800,
    @Json(name = "rainProbability") val rainProbability: Int = 0,
    @Json(name = "precipitationMm") val precipitationMm: Double = 0.0,
    @Json(name = "windSpeed") val windSpeed: Double = 10.0,
    @Json(name = "windDirection") val windDirection: Int = 0
)

@JsonClass(generateAdapter = true)
data class DailyForecastModel(
    @Json(name = "date") val date: String,
    @Json(name = "dayName") val dayName: String,
    @Json(name = "tempMin") val tempMin: Double,
    @Json(name = "tempMax") val tempMax: Double,
    @Json(name = "condition") val condition: String,
    @Json(name = "icon") val icon: String = "01d",
    @Json(name = "weatherCode") val weatherCode: Int = 800,
    @Json(name = "rainProbability") val rainProbability: Int = 0,
    @Json(name = "precipitationMm") val precipitationMm: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class WeatherAlertModel(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String,
    @Json(name = "severity") val severity: String = "INFO", // INFO, WARNING, DANGER, GREAT_DANGER
    @Json(name = "startTime") val startTime: String = "",
    @Json(name = "endTime") val endTime: String = "",
    @Json(name = "source") val source: String = "BMD",
    @Json(name = "signalNumber") val signalNumber: Int? = null,
    @Json(name = "regions") val regions: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class BmdStatusModel(
    @Json(name = "available") val available: Boolean = false,
    @Json(name = "station") val station: String? = null,
    @Json(name = "observation") val observation: BmdStationObservationDto? = null
)

@JsonClass(generateAdapter = true)
data class BmdStationObservationDto(
    @Json(name = "stationId") val stationId: String = "",
    @Json(name = "stationName") val stationName: String = "",
    @Json(name = "division") val division: String = "",
    @Json(name = "temperatureC") val temperatureC: Double = 0.0,
    @Json(name = "humidityPercent") val humidityPercent: Int = 0,
    @Json(name = "windSpeedKmh") val windSpeedKmh: Double = 0.0,
    @Json(name = "windDirectionDegrees") val windDirectionDegrees: Int = 0,
    @Json(name = "pressureHpa") val pressureHpa: Double = 1008.0,
    @Json(name = "rainfall24hMm") val rainfall24hMm: Double = 0.0,
    @Json(name = "recordedAt") val recordedAt: String = ""
)

@JsonClass(generateAdapter = true)
data class RadarMetadataModel(
    @Json(name = "available") val available: Boolean = false,
    @Json(name = "host") val host: String? = null,
    @Json(name = "latestPath") val latestPath: String? = null,
    @Json(name = "latestTime") val latestTime: Long? = null,
    @Json(name = "pastFramesCount") val pastFramesCount: Int = 0
)
