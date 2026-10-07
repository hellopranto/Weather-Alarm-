package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BmdObservationsEnvelope(
    @Json(name = "available") val available: Boolean = true,
    @Json(name = "source") val source: String = "Bangladesh Meteorological Department",
    @Json(name = "stations") val stations: List<BmdStationDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class BmdStationDto(
    @Json(name = "stationId") val stationId: String,
    @Json(name = "stationName") val stationName: String,
    @Json(name = "division") val division: String = "",
    @Json(name = "latitude") val latitude: Double = 0.0,
    @Json(name = "longitude") val longitude: Double = 0.0,
    @Json(name = "temperatureC") val temperatureC: Double = 0.0,
    @Json(name = "humidityPercent") val humidityPercent: Int = 0,
    @Json(name = "windSpeedKmh") val windSpeedKmh: Double = 0.0,
    @Json(name = "windDirectionDegrees") val windDirectionDegrees: Int = 0,
    @Json(name = "pressureHpa") val pressureHpa: Double = 1013.0,
    @Json(name = "rainfall24hMm") val rainfall24hMm: Double = 0.0,
    @Json(name = "recordedAt") val recordedAt: String = ""
)

@JsonClass(generateAdapter = true)
data class BmdWarningsEnvelope(
    @Json(name = "available") val available: Boolean = true,
    @Json(name = "warnings") val warnings: List<BmdWarningDto> = emptyList(),
    @Json(name = "updatedAt") val updatedAt: String = ""
)

@JsonClass(generateAdapter = true)
data class BmdWarningDto(
    @Json(name = "id") val id: String,
    @Json(name = "type") val type: String,
    @Json(name = "signalNumber") val signalNumber: Int? = null,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String,
    @Json(name = "affectedRegions") val affectedRegions: List<String> = emptyList(),
    @Json(name = "issuedAt") val issuedAt: String = "",
    @Json(name = "validUntil") val validUntil: String = "",
    @Json(name = "severity") val severity: String = "INFO"
)

/**
 * Clean structure matching user specification for GET /api/bmd?lat={lat}&lon={lon}
 */
@JsonClass(generateAdapter = true)
data class BmdNearestResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "location") val location: BmdLocationDto? = null,
    @Json(name = "nearestStation") val nearestStation: BmdNearestStationDto? = null,
    @Json(name = "observation") val observation: BmdObservationDataDto? = null,
    @Json(name = "source") val source: String = "BMD",
    @Json(name = "updatedAt") val updatedAt: String = "",
    @Json(name = "isStale") val isStale: Boolean = false,
    @Json(name = "dataAgeMinutes") val dataAgeMinutes: Int? = null
)

@JsonClass(generateAdapter = true)
data class BmdLocationDto(
    @Json(name = "latitude") val latitude: Double = 0.0,
    @Json(name = "longitude") val longitude: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class BmdNearestStationDto(
    @Json(name = "stationId") val stationId: String = "",
    @Json(name = "stationCode") val stationCode: String = "",
    @Json(name = "stationName") val stationName: String = "",
    @Json(name = "latitude") val latitude: Double = 0.0,
    @Json(name = "longitude") val longitude: Double = 0.0,
    @Json(name = "distanceKm") val distanceKm: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class BmdObservationDataDto(
    @Json(name = "temperature") val temperature: Double? = null,
    @Json(name = "humidity") val humidity: Int? = null,
    @Json(name = "pressure") val pressure: Double? = null,
    @Json(name = "windSpeed") val windSpeed: Double? = null,
    @Json(name = "windDirection") val windDirection: Int? = null,
    @Json(name = "rainfall") val rainfall: Double? = null,
    @Json(name = "weatherCondition") val weatherCondition: String? = null,
    @Json(name = "observationTime") val observationTime: String? = null
)

/**
 * Model representing an active synop station loaded from station_synop.csv
 */
data class BmdCsvStation(
    val stationId: String,
    val stationType: String,
    val stationCode: String,
    val longitude: Double,
    val latitude: Double,
    val stationName: String,
    val active: Boolean
)
