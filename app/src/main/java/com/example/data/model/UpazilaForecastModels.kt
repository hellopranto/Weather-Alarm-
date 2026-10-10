package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpazilaBbsRecord(
    @Json(name = "name") val name: String,
    @Json(name = "pcode") val pcode: String,
    @Json(name = "pcode_raw") val pcodeRaw: String? = null,
    @Json(name = "district") val district: String,
    @Json(name = "division") val division: String,
    @Json(name = "lat") val lat: Double,
    @Json(name = "lon") val lon: Double,
    @Json(name = "division_bn") val divisionBn: String? = null,
    @Json(name = "district_bn") val districtBn: String? = null
)

@JsonClass(generateAdapter = true)
data class UpazilaForecastStepItem(
    @Json(name = "step_start") val stepStart: String,
    @Json(name = "step_end") val stepEnd: String,
    @Json(name = "val_min") val valMin: Double? = null,
    @Json(name = "val_avg") val valAvg: Double? = null,
    @Json(name = "val_max") val valMax: Double? = null,
    @Json(name = "val_avg_day") val valAvgDay: Double? = null,
    @Json(name = "val_avg_night") val valAvgNight: Double? = null
)

@JsonClass(generateAdapter = true)
data class UpazilaForecastDataPayload(
    @Json(name = "upazila_name") val upazilaName: String? = null,
    @Json(name = "district_name") val districtName: String? = null,
    @Json(name = "division_name") val divisionName: String? = null,
    @Json(name = "ADM3_PCODE") val adm3Pcode: Any? = null, // Can be int or string
    @Json(name = "forecast_data") val forecastData: Map<String, List<UpazilaForecastStepItem>> = emptyMap()
) {
    val pcodeString: String
        get() = adm3Pcode?.toString()?.replace("BD", "")?.trim() ?: ""
}

@JsonClass(generateAdapter = true)
data class UpazilaForecastEnvelope(
    @Json(name = "error") val error: String? = null,
    @Json(name = "source") val source: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "data") val data: Map<String, UpazilaForecastDataPayload>? = null
) {
    fun getPayloadFor(pcode: String): UpazilaForecastDataPayload? {
        val clean = pcode.replace("BD", "").trim()
        return data?.get(clean) ?: data?.get("BD$clean") ?: data?.values?.firstOrNull()
    }
}

/**
 * Clean UI Domain Model for Upazila Forecast
 */
data class UpazilaUiForecast(
    val upazilaName: String,
    val districtName: String,
    val divisionName: String,
    val pcode: String,
    val source: String,
    val updatedAtFormattedBn: String,
    val dailySteps: List<UpazilaDailyForecastItem>,
    val subDailySteps: List<UpazilaSubDailyForecastItem>,
    val alerts: List<UpazilaForecastAlert>
)

data class UpazilaDailyForecastItem(
    val dateLabelBn: String,
    val tempMin: Double?,
    val tempMax: Double?,
    val tempAvg: Double?,
    val rainfallMm: Double?,
    val humidityPercent: Double?,
    val windSpeedKmh: Double?,
    val cloudCoverPercent: Double?,
    val windGustKmh: Double?,
    val soilMoisture: Double?
)

data class UpazilaSubDailyForecastItem(
    val timeLabelBn: String,
    val temp: Double?,
    val rainfallMm: Double?,
    val humidityPercent: Double?,
    val windSpeedKmh: Double?
)

data class UpazilaForecastAlert(
    val titleBn: String,
    val descriptionBn: String,
    val isSevere: Boolean
)
