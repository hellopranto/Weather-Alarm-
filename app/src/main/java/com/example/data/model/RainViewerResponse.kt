package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RainViewerResponse(
    @Json(name = "version") val version: String? = null,
    @Json(name = "generated") val generated: Long = 0L,
    @Json(name = "host") val host: String = "https://tilecache.rainviewer.com",
    @Json(name = "radar") val radar: RadarSeries? = null,
    @Json(name = "satellite") val satellite: SatelliteSeries? = null
)

@JsonClass(generateAdapter = true)
data class RadarSeries(
    @Json(name = "past") val past: List<RadarFrameItem> = emptyList(),
    @Json(name = "nowcast") val nowcast: List<RadarFrameItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class RadarFrameItem(
    @Json(name = "time") val time: Long,
    @Json(name = "path") val path: String
)

@JsonClass(generateAdapter = true)
data class SatelliteSeries(
    @Json(name = "infrared") val infrared: List<RadarFrameItem> = emptyList()
)
