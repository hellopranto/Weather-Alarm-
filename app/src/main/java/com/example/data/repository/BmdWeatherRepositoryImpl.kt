package com.example.data.repository

import com.example.data.model.BmdCsvStation
import com.example.data.model.BmdLocationDto
import com.example.data.model.BmdNearestResponse
import com.example.data.model.BmdNearestStationDto
import com.example.data.model.BmdObservationDataDto
import com.example.data.model.BmdStationDto
import com.example.data.model.BmdWarningDto
import com.example.data.remote.ApiClient
import com.example.data.remote.BmdApi
import com.example.domain.repository.BmdWeatherRepository
import com.example.domain.repository.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class BmdWeatherRepositoryImpl(
    private val bmdApi: BmdApi,
    private val httpClient: OkHttpClient = ApiClient.okHttpClient
) : BmdWeatherRepository {

    private var cachedStations: List<BmdCsvStation> = emptyList()
    private var stationsLastFetched: Long = 0L
    private val STATIONS_CACHE_TTL_MS = 60 * 60 * 1000L // 1 hour

    /**
     * Exact Haversine formula specified for geographic distance
     */
    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val dist = r * c
        return (dist * 10.0).roundToInt() / 10.0
    }

    /**
     * Dynamically loads active BMD stations from https://mobile.bmd.gov.bd/bmdmobile/station_synop.csv
     */
    suspend fun loadActiveStations(): List<BmdCsvStation> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedStations.isNotEmpty() && now - stationsLastFetched < STATIONS_CACHE_TTL_MS) {
            return@withContext cachedStations
        }

        try {
            val request = Request.Builder()
                .url("https://mobile.bmd.gov.bd/bmdmobile/station_synop.csv")
                .header("User-Agent", "WeatherAlertBangladesh/2.0")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val csvBody = response.body?.string().orEmpty()
                    val parsedStations = parseStationsCsv(csvBody)
                    if (parsedStations.isNotEmpty()) {
                        cachedStations = parsedStations
                        stationsLastFetched = now
                        return@withContext parsedStations
                    }
                }
            }
        } catch (_: Exception) {
            // Network fallback or retry
        }

        cachedStations
    }

    private fun parseStationsCsv(csv: String): List<BmdCsvStation> {
        val list = mutableListOf<BmdCsvStation>()
        val lines = csv.lines()
        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isBlank()) continue

            // Split handling potential quotes
            val parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
            if (parts.size >= 7) {
                try {
                    val stationId = parts[0].replace("\"", "").trim()
                    val stationType = parts[1].replace("\"", "").trim()
                    val stationCode = parts[2].replace("\"", "").trim()
                    val lon = parts[3].replace("\"", "").trim().toDouble()
                    val lat = parts[4].replace("\"", "").trim().toDouble()
                    val stationName = parts[5].replace("\"", "").trim()
                    val activeStr = parts[6].replace("\"", "").trim().lowercase()

                    val isActive = activeStr == "yes" || activeStr == "true" || activeStr == "1"
                    if (isActive) {
                        list.add(
                            BmdCsvStation(
                                stationId = stationId,
                                stationType = stationType,
                                stationCode = stationCode,
                                longitude = lon,
                                latitude = lat,
                                stationName = stationName,
                                active = true
                            )
                        )
                    }
                } catch (_: Exception) {
                    // skip malformed row
                }
            }
        }
        return list
    }

    /**
     * Fetches AWS observation data from https://mobile.bmd.gov.bd/bmdmobile/aws_data.php
     */
    private suspend fun fetchAwsObservations(): Map<String, BmdObservationDataDto> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, BmdObservationDataDto>()
        try {
            val request = Request.Builder()
                .url("https://mobile.bmd.gov.bd/bmdmobile/aws_data.php")
                .header("User-Agent", "WeatherAlertBangladesh/2.0")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val csv = response.body?.string().orEmpty()
                    val lines = csv.lines()
                    for (i in 1 until lines.size) {
                        val line = lines[i].trim()
                        if (line.isBlank()) continue
                        val parts = line.split(",")
                        if (parts.size >= 11) {
                            val stId = parts[1].trim()
                            val param = parts[2].trim().lowercase()
                            val value = parts[4].trim().toDoubleOrNull()
                            val dateStr = parts[10].trim()

                            val current = result[stId] ?: BmdObservationDataDto(observationTime = dateStr)
                            val updated = when {
                                param.contains("temp") -> current.copy(temperature = value)
                                param.contains("hum") -> current.copy(humidity = value?.roundToInt())
                                param.contains("press") -> current.copy(pressure = value)
                                param.contains("wind_spd") || param.contains("wind speed") -> current.copy(windSpeed = value?.let { (it * 3.6 * 10).roundToInt() / 10.0 })
                                param.contains("wind_dir") || param.contains("wind direction") -> current.copy(windDirection = value?.roundToInt())
                                param.contains("rain") -> current.copy(rainfall = value)
                                else -> current
                            }
                            result[stId] = updated
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Optional AWS data
        }
        result
    }

    override fun getNearestBmdData(lat: Double, lon: Double): Flow<Resource<BmdNearestResponse>> = flow {
        emit(Resource.Loading)

        // 1. First, attempt backend endpoint: GET /api/bmd?lat={lat}&lon={lon}
        try {
            val backendResponse = bmdApi.getNearestBmdData(lat, lon)
            if (backendResponse.success && backendResponse.nearestStation != null) {
                // Check freshness
                val freshnessEvaluated = evaluateFreshness(backendResponse)
                emit(Resource.Success(freshnessEvaluated))
                return@flow
            }
        } catch (_: Exception) {
            // Backend endpoint not deployed yet or temporarily unavailable -> fallback to client dynamic calculation
        }

        // 2. Client fallback: Load active stations dynamically from BMD CSV
        try {
            val stations = loadActiveStations()
            if (stations.isEmpty()) {
                emit(Resource.Error("BMD সক্রিয় স্টেশন তালিকা লোড করা সম্ভব হয়নি।"))
                return@flow
            }

            // Calculate distance to each station and sort ascending
            data class StationWithDist(val station: BmdCsvStation, val distKm: Double)
            val sorted = stations.map { station ->
                StationWithDist(station, distanceKm(lat, lon, station.latitude, station.longitude))
            }.sortedBy { it.distKm }

            val awsObs = fetchAwsObservations()

            // Station fallback: find the nearest station with valid recent data
            var chosenStation = sorted.first()
            var chosenObs = awsObs[chosenStation.station.stationId] ?: awsObs[chosenStation.station.stationCode]

            if (chosenObs == null || (chosenObs.temperature == null && chosenObs.humidity == null)) {
                // Check next nearest active stations
                for (item in sorted.drop(1).take(5)) {
                    val obs = awsObs[item.station.stationId] ?: awsObs[item.station.stationCode]
                    if (obs != null && (obs.temperature != null || obs.humidity != null)) {
                        chosenStation = item
                        chosenObs = obs
                        break
                    }
                }
            }

            val nearestDto = BmdNearestStationDto(
                stationId = chosenStation.station.stationId,
                stationCode = chosenStation.station.stationCode,
                stationName = chosenStation.station.stationName,
                latitude = chosenStation.station.latitude,
                longitude = chosenStation.station.longitude,
                distanceKm = chosenStation.distKm
            )

            val observationDto = chosenObs ?: BmdObservationDataDto(
                temperature = null,
                humidity = null,
                pressure = null,
                windSpeed = null,
                windDirection = null,
                rainfall = null,
                weatherCondition = null,
                observationTime = null
            )

            val response = BmdNearestResponse(
                success = true,
                location = BmdLocationDto(latitude = lat, longitude = lon),
                nearestStation = nearestDto,
                observation = observationDto,
                source = "BMD",
                updatedAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.format(java.util.Date())
            )

            emit(Resource.Success(evaluateFreshness(response)))
        } catch (e: Exception) {
            emit(Resource.Error("নিকটবর্তী BMD স্টেশন তথ্য প্রাপ্তিতে সমস্যা হয়েছে: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    private fun evaluateFreshness(response: BmdNearestResponse): BmdNearestResponse {
        val obsTime = response.observation?.observationTime ?: return response
        return try {
            val formats = listOf(
                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US),
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US),
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            )
            var parsedDate: java.util.Date? = null
            for (fmt in formats) {
                try {
                    parsedDate = fmt.parse(obsTime)
                    if (parsedDate != null) break
                } catch (_: Exception) {}
            }

            if (parsedDate != null) {
                val ageMinutes = ((System.currentTimeMillis() - parsedDate.time) / (1000 * 60)).toInt()
                val isStale = ageMinutes > 180 // 3 hours
                response.copy(isStale = isStale, dataAgeMinutes = ageMinutes)
            } else {
                response
            }
        } catch (_: Exception) {
            response
        }
    }

    override fun getStationObservations(stationQuery: String?): Flow<Resource<List<BmdStationDto>>> = flow {
        emit(Resource.Loading)
        try {
            val response = bmdApi.getObservations(stationQuery)
            val stations = if (!stationQuery.isNullOrBlank()) {
                response.stations.filter {
                    it.stationName.contains(stationQuery, ignoreCase = true) ||
                    it.division.contains(stationQuery, ignoreCase = true)
                }
            } else {
                response.stations
            }
            emit(Resource.Success(stations))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "BMD স্টেশন পর্যবেক্ষণ তথ্য পাওয়া যায়নি।"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getWeatherWarnings(): Flow<Resource<List<BmdWarningDto>>> = flow {
        emit(Resource.Loading)
        try {
            val response = bmdApi.getWarnings()
            emit(Resource.Success(response.warnings))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "BMD সতর্কতা বুলেটিন পাওয়া যায়নি।"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getObservationForCoordinates(lat: Double, lon: Double): BmdStationDto? {
        return try {
            val response = bmdApi.getObservations(null)
            response.stations.minByOrNull { station ->
                distanceKm(lat, lon, station.latitude, station.longitude)
            }
        } catch (_: Exception) {
            null
        }
    }
}
