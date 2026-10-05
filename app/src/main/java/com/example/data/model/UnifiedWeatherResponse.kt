package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UnifiedWeatherResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "location") val location: LocationModel,
    @Json(name = "station") val station: StationModel? = null,
    @Json(name = "current") val current: CurrentWeatherModel,
    @Json(name = "weather") val weather: WeatherSimpleModel? = null,
    @Json(name = "units") val units: WeatherUnitsModel? = null,
    @Json(name = "hourly") val hourly: List<HourlyForecastModel> = emptyList(),
    @Json(name = "daily") val daily: List<DailyForecastModel> = emptyList(),
    @Json(name = "airQuality") val airQuality: AirQualityModel? = null,
    @Json(name = "sunMoon") val sunMoon: SunMoonModel? = null,
    @Json(name = "warnings") val warnings: List<WeatherAlertModel> = emptyList(),
    @Json(name = "alerts") val alerts: List<WeatherAlertModel> = emptyList(),
    @Json(name = "rainPrediction") val rainPrediction: RainPredictionModel? = null,
    @Json(name = "bmd") val bmd: BmdStatusModel? = null,
    @Json(name = "radar") val radar: RadarMetadataModel? = null,
    @Json(name = "updatedAt") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class LocationModel(
    @Json(name = "name") val name: String = "",
    @Json(name = "district") val district: String? = null,
    @Json(name = "country") val country: String = "বাংলাদেশ",
    @Json(name = "latitude") val latitude: Double = 0.0,
    @Json(name = "longitude") val longitude: Double = 0.0,
    @Json(name = "village") val village: String? = null,
    @Json(name = "union") val union: String? = null,
    @Json(name = "upazila") val upazila: String? = null,
    @Json(name = "division") val division: String? = null,
    @Json(name = "displayName") val displayName: String? = null
)

@JsonClass(generateAdapter = true)
data class StationModel(
    @Json(name = "code") val code: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "latitude") val latitude: Double = 0.0,
    @Json(name = "longitude") val longitude: Double = 0.0,
    @Json(name = "distanceKm") val distanceKm: Double = 0.0
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
    @Json(name = "temperature") val temperature: Double? = null,
    @Json(name = "feelsLike") val feelsLike: Double? = null,
    @Json(name = "tempMin") val tempMin: Double? = null,
    @Json(name = "tempMax") val tempMax: Double? = null,
    @Json(name = "humidity") val humidity: Int? = null,
    @Json(name = "windSpeed") val windSpeed: Double? = null, // in km/h
    @Json(name = "windDirection") val windDirection: Int? = null, // in degrees
    @Json(name = "windGust") val windGust: Double? = null,
    @Json(name = "pressure") val pressure: Int? = null, // in hPa
    @Json(name = "visibility") val visibility: Int? = null, // in meters
    @Json(name = "dewPoint") val dewPoint: Double? = null,
    @Json(name = "cloudCoverage") val cloudCoverage: Int? = null,
    @Json(name = "rainfall") val rainfall: Double? = null,
    @Json(name = "rainProbability") val rainProbability: Int? = null,
    @Json(name = "precipitationMm") val precipitationMm: Double? = null,
    @Json(name = "uvIndex") val uvIndex: Double? = null,
    @Json(name = "sunshineDuration") val sunshineDuration: Double? = null,
    @Json(name = "condition") val condition: String? = null,
    @Json(name = "conditionBn") val conditionBn: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "weatherCode") val weatherCode: Int = 800,
    @Json(name = "icon") val icon: String = "01d",
    @Json(name = "sunrise") val sunrise: String? = null,
    @Json(name = "sunset") val sunset: String? = null,
    @Json(name = "recordedAt") val recordedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class HourlyForecastModel(
    @Json(name = "timestamp") val timestamp: Long = 0L,
    @Json(name = "timeString") val timeString: String = "",
    @Json(name = "dateString") val dateString: String? = null,
    @Json(name = "temperature") val temperature: Double? = null,
    @Json(name = "feelsLike") val feelsLike: Double? = null,
    @Json(name = "humidity") val humidity: Int? = null,
    @Json(name = "pressure") val pressure: Int? = null,
    @Json(name = "dewPoint") val dewPoint: Double? = null,
    @Json(name = "cloudCoverage") val cloudCoverage: Int? = null,
    @Json(name = "condition") val condition: String = "Clear",
    @Json(name = "conditionBn") val conditionBn: String? = null,
    @Json(name = "description") val description: String = "",
    @Json(name = "icon") val icon: String = "01d",
    @Json(name = "weatherCode") val weatherCode: Int = 800,
    @Json(name = "rainProbability") val rainProbability: Int = 0,
    @Json(name = "precipitationMm") val precipitationMm: Double = 0.0,
    @Json(name = "windSpeed") val windSpeed: Double = 0.0,
    @Json(name = "windDirection") val windDirection: Int = 0,
    @Json(name = "windGust") val windGust: Double? = null
)

@JsonClass(generateAdapter = true)
data class DailyForecastModel(
    @Json(name = "date") val date: String = "",
    @Json(name = "dayName") val dayName: String = "",
    @Json(name = "dayNameBn") val dayNameBn: String? = null,
    @Json(name = "dateFormattedBn") val dateFormattedBn: String? = null,
    @Json(name = "tempMin") val tempMin: Double? = null,
    @Json(name = "tempMax") val tempMax: Double? = null,
    @Json(name = "condition") val condition: String = "",
    @Json(name = "conditionBn") val conditionBn: String? = null,
    @Json(name = "icon") val icon: String = "01d",
    @Json(name = "weatherCode") val weatherCode: Int = 800,
    @Json(name = "rainProbability") val rainProbability: Int = 0,
    @Json(name = "precipitationMm") val precipitationMm: Double = 0.0,
    @Json(name = "windSpeed") val windSpeed: Double? = null,
    @Json(name = "humidity") val humidity: Int? = null,
    @Json(name = "cloudCoverage") val cloudCoverage: Int? = null,
    @Json(name = "sunrise") val sunrise: String? = null,
    @Json(name = "sunset") val sunset: String? = null
)

@JsonClass(generateAdapter = true)
data class AirQualityModel(
    @Json(name = "aqi") val aqi: Int? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "categoryBn") val categoryBn: String? = null,
    @Json(name = "pm25") val pm25: Double? = null,
    @Json(name = "pm10") val pm10: Double? = null,
    @Json(name = "co") val co: Double? = null,
    @Json(name = "no2") val no2: Double? = null,
    @Json(name = "so2") val so2: Double? = null,
    @Json(name = "o3") val o3: Double? = null,
    @Json(name = "healthRecommendation") val healthRecommendation: String? = null
)

@JsonClass(generateAdapter = true)
data class SunMoonModel(
    @Json(name = "sunrise") val sunrise: String? = null,
    @Json(name = "sunset") val sunset: String? = null,
    @Json(name = "moonrise") val moonrise: String? = null,
    @Json(name = "moonset") val moonset: String? = null,
    @Json(name = "moonPhase") val moonPhase: String? = null,
    @Json(name = "moonPhaseBn") val moonPhaseBn: String? = null
)

@JsonClass(generateAdapter = true)
data class WeatherAlertModel(
    @Json(name = "id") val id: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "titleBn") val titleBn: String? = null,
    @Json(name = "description") val description: String = "",
    @Json(name = "descriptionBn") val descriptionBn: String? = null,
    @Json(name = "severity") val severity: String = "INFO", // INFO, WARNING, DANGER, GREAT_DANGER
    @Json(name = "area") val area: String? = null,
    @Json(name = "startTime") val startTime: String = "",
    @Json(name = "endTime") val endTime: String = "",
    @Json(name = "source") val source: String = "BMD",
    @Json(name = "signalNumber") val signalNumber: Int? = null,
    @Json(name = "regions") val regions: List<String> = emptyList(),
    @Json(name = "instructions") val instructions: String? = null
)

@JsonClass(generateAdapter = true)
data class RainPredictionModel(
    @Json(name = "expectedNextHours") val expectedNextHours: String? = null,
    @Json(name = "rainProbability") val rainProbability: Int? = null,
    @Json(name = "summaryBn") val summaryBn: String? = null
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
