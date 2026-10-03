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
    @Json(name = "division") val division: String,
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "temperatureC") val temperatureC: Double,
    @Json(name = "humidityPercent") val humidityPercent: Int,
    @Json(name = "windSpeedKmh") val windSpeedKmh: Double,
    @Json(name = "windDirectionDegrees") val windDirectionDegrees: Int,
    @Json(name = "pressureHpa") val pressureHpa: Double,
    @Json(name = "rainfall24hMm") val rainfall24hMm: Double,
    @Json(name = "recordedAt") val recordedAt: String
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
