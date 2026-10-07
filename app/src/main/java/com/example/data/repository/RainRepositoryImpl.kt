package com.example.data.repository

import com.example.data.model.CurrentRainModel
import com.example.data.model.DailyRainModel
import com.example.data.model.HourlyRainModel
import com.example.data.model.PredictedRainPeriodModel
import com.example.data.model.RainLocationModel
import com.example.data.model.RainPredictionResponse
import com.example.data.model.RainSummary24hModel
import com.example.data.remote.ApiClient
import com.example.data.remote.RainApi
import com.example.domain.repository.RainRepository
import com.example.domain.repository.Resource
import com.squareup.moshi.JsonAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class RainRepositoryImpl(
    private val rainApi: RainApi = ApiClient.rainApi
) : RainRepository {

    private val jsonAdapter: JsonAdapter<RainPredictionResponse> =
        ApiClient.moshi.adapter(RainPredictionResponse::class.java)

    private val inMemoryCache = ConcurrentHashMap<String, Pair<RainPredictionResponse, Long>>()
    private val CACHE_EXPIRY_MS = 10 * 60 * 1000 // 10 minutes

    override fun getRainPrediction(
        lat: Double,
        lon: Double,
        forceRefresh: Boolean
    ): Flow<Resource<RainPredictionResponse>> = flow {
        emit(Resource.Loading)

        val key = String.format(Locale.US, "%.2f_%.2f", lat, lon)
        val now = System.currentTimeMillis()
        val cached = inMemoryCache[key]

        if (!forceRefresh && cached != null && (now - cached.second) < CACHE_EXPIRY_MS) {
            emit(Resource.Success(cached.first, isOfflineCached = true))
            return@flow
        }

        try {
            // 1. Try Backend endpoint /rain-prediction then /api/rain-prediction
            val remoteData = try {
                rainApi.getRainPrediction(lat, lon)
            } catch (_: Exception) {
                try {
                    rainApi.getApiRainPrediction(lat, lon)
                } catch (_: Exception) {
                    fetchDirectOpenMeteo(lat, lon)
                }
            }

            inMemoryCache[key] = Pair(remoteData, now)
            emit(Resource.Success(remoteData, isOfflineCached = false))
        } catch (e: Exception) {
            if (cached != null) {
                emit(Resource.Success(cached.first, isOfflineCached = true))
            } else {
                val fallback = createFallbackRain(lat, lon)
                inMemoryCache[key] = Pair(fallback, now)
                emit(Resource.Success(fallback, isOfflineCached = true))
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getCachedRainPrediction(lat: Double, lon: Double): RainPredictionResponse? {
        val key = String.format(Locale.US, "%.2f_%.2f", lat, lon)
        return inMemoryCache[key]?.first
    }

    private suspend fun fetchDirectOpenMeteo(lat: Double, lon: Double): RainPredictionResponse {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,precipitation,rain,showers,weather_code,wind_speed_10m,wind_direction_10m&hourly=precipitation_probability,precipitation,rain,showers,weather_code,temperature_2m,relative_humidity_2m,wind_speed_10m,wind_direction_10m&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max,rain_sum,showers_sum&timezone=auto"
        val responseBody = rainApi.getDirectOpenMeteoForecast(url)
        val jsonStr = responseBody.string()
        val root = JSONObject(jsonStr)

        val timezone = root.optString("timezone", "Asia/Dhaka")
        val currentObj = root.optJSONObject("current") ?: JSONObject()
        val curPrecip = currentObj.optDouble("precipitation", 0.0)
        val curRain = currentObj.optDouble("rain", 0.0)
        val curShowers = currentObj.optDouble("showers", 0.0)
        val curCode = currentObj.optInt("weather_code", 0)
        val curTemp = currentObj.optDouble("temperature_2m", 28.0)
        val curHumidity = currentObj.optInt("relative_humidity_2m", 75)
        val curWindSpeed = currentObj.optDouble("wind_speed_10m", 0.0)
        val curWindDir = currentObj.optInt("wind_direction_10m", 0)

        val hourlyObj = root.optJSONObject("hourly") ?: JSONObject()
        val timesArr = hourlyObj.optJSONArray("time")
        val probArr = hourlyObj.optJSONArray("precipitation_probability")
        val precipArr = hourlyObj.optJSONArray("precipitation")
        val rainArr = hourlyObj.optJSONArray("rain")
        val showersArr = hourlyObj.optJSONArray("showers")
        val codeArr = hourlyObj.optJSONArray("weather_code")
        val tempArr = hourlyObj.optJSONArray("temperature_2m")
        val humidArr = hourlyObj.optJSONArray("relative_humidity_2m")
        val windArr = hourlyObj.optJSONArray("wind_speed_10m")

        val hourlyList = mutableListOf<HourlyRainModel>()
        val count = Math.min(timesArr?.length() ?: 0, 48)
        var maxProb24 = 0
        var sumPrecip24 = 0.0
        var maxRate24 = 0.0

        for (i in 0 until count) {
            val t = timesArr?.optString(i) ?: ""
            val p = probArr?.optInt(i, 0) ?: 0
            val pr = precipArr?.optDouble(i, 0.0) ?: 0.0
            val rn = rainArr?.optDouble(i, 0.0) ?: 0.0
            val sh = showersArr?.optDouble(i, 0.0) ?: 0.0
            val cd = codeArr?.optInt(i, 0) ?: 0
            val tm = tempArr?.optDouble(i, 28.0) ?: 28.0
            val hm = humidArr?.optInt(i, 70) ?: 70
            val ws = windArr?.optDouble(i, 0.0) ?: 0.0

            if (i < 24) {
                if (p > maxProb24) maxProb24 = p
                sumPrecip24 += pr
                if (pr > maxRate24) maxRate24 = pr
            }

            hourlyList.add(
                HourlyRainModel(
                    time = t,
                    precipitationProbability = p,
                    precipitationMm = pr,
                    rainMm = rn,
                    showersMm = sh,
                    weatherCode = cd,
                    condition = getWeatherCondition(cd),
                    temperature = tm,
                    humidity = hm,
                    windSpeed = ws
                )
            )
        }

        val dailyObj = root.optJSONObject("daily") ?: JSONObject()
        val dTimes = dailyObj.optJSONArray("time")
        val dCodes = dailyObj.optJSONArray("weather_code")
        val dProbMax = dailyObj.optJSONArray("precipitation_probability_max")
        val dPrecipSum = dailyObj.optJSONArray("precipitation_sum")
        val dTempMax = dailyObj.optJSONArray("temperature_2m_max")
        val dTempMin = dailyObj.optJSONArray("temperature_2m_min")

        val dailyList = mutableListOf<DailyRainModel>()
        val dCount = Math.min(dTimes?.length() ?: 0, 7)
        for (i in 0 until dCount) {
            dailyList.add(
                DailyRainModel(
                    date = dTimes?.optString(i) ?: "",
                    maxRainProbability = dProbMax?.optInt(i, 0) ?: 0,
                    totalRainfallMm = dPrecipSum?.optDouble(i, 0.0) ?: 0.0,
                    weatherCode = dCodes?.optInt(i, 0) ?: 0,
                    condition = getWeatherCondition(dCodes?.optInt(i, 0) ?: 0),
                    tempMax = dTempMax?.optDouble(i, 30.0) ?: 30.0,
                    tempMin = dTempMin?.optDouble(i, 22.0) ?: 22.0
                )
            )
        }

        val intensity = when {
            maxRate24 <= 0.05 -> "None"
            maxRate24 < 2.5 -> "Light"
            maxRate24 < 10.0 -> "Moderate"
            maxRate24 < 50.0 -> "Heavy"
            else -> "Violent"
        }

        val rainExpected = maxProb24 >= 40 || sumPrecip24 >= 1.0

        val periods = mutableListOf<PredictedRainPeriodModel>()
        var currentP: PredictedRainPeriodModel? = null
        for (i in 0 until Math.min(hourlyList.size, 24)) {
            val h = hourlyList[i]
            val isRaining = h.precipitationProbability >= 40 || h.precipitationMm >= 0.2
            if (isRaining) {
                if (currentP == null) {
                    currentP = PredictedRainPeriodModel(
                        start = h.time,
                        end = h.time,
                        expectedRainfallMm = h.precipitationMm,
                        maxProbability = h.precipitationProbability
                    )
                } else {
                    currentP = currentP.copy(
                        end = h.time,
                        expectedRainfallMm = currentP.expectedRainfallMm + h.precipitationMm,
                        maxProbability = maxOf(currentP.maxProbability, h.precipitationProbability)
                    )
                }
            } else if (currentP != null) {
                periods.add(currentP)
                currentP = null
            }
        }
        if (currentP != null) periods.add(currentP)

        val (advEn, advBn) = when {
            intensity == "Violent" || sumPrecip24 > 50 -> Pair(
                "Heavy downpour warning! Waterlogging in low-lying areas and dangerous roads possible.",
                "ভারী বৃষ্টির সতর্কতা! নিম্নাঞ্চলে জলাবদ্ধতা ও পিচ্ছিল রাস্তার ঝুঁকি রয়েছে।"
            )
            intensity == "Heavy" || sumPrecip24 >= 25 -> Pair(
                "Significant rain forecast over next 24 hours. Be prepared with rain gear.",
                "আগামী ২৪ ঘণ্টায় উল্লেখযোগ্য বৃষ্টিপাতের পূর্বাভাস। ছাতা বা রেইনকোট সাথে রাখুন।"
            )
            intensity == "Moderate" || sumPrecip24 >= 5 -> Pair(
                "Moderate showers predicted. Carry an umbrella when commuting.",
                "মাঝারি ধরনের বৃষ্টির সম্ভাবনা রয়েছে। যাতায়াতের সময় ছাতা সাথে রাখা শ্রেয়।"
            )
            rainExpected -> Pair(
                "Passing light showers or drizzle possible. Low impact on daily routine.",
                "হালকা গুঁড়ি গুঁড়ি বৃষ্টি বা সাময়িক পশলা বৃষ্টির সম্ভাবনা।"
            )
            else -> Pair(
                "No significant rainfall expected over the next 24 hours.",
                "আগামী ২৪ ঘণ্টায় ভারী বৃষ্টির কোনো আশঙ্কা নেই।"
            )
        }

        return RainPredictionResponse(
            location = RainLocationModel(lat, lon, timezone),
            current = CurrentRainModel(
                precipitationMm = curPrecip,
                rainMm = curRain,
                showersMm = curShowers,
                weatherCode = curCode,
                condition = getWeatherCondition(curCode),
                temperature = curTemp,
                humidity = curHumidity,
                windSpeed = curWindSpeed,
                windDirection = curWindDir
            ),
            summary24h = RainSummary24hModel(
                maxRainProbability = maxProb24,
                totalExpectedRainfallMm = Math.round(sumPrecip24 * 10) / 10.0,
                rainExpected = rainExpected,
                intensity = intensity,
                predictedRainPeriods = periods,
                advisoryEn = advEn,
                advisoryBn = advBn
            ),
            hourly = hourlyList,
            daily = dailyList,
            source = "Open-Meteo High-Resolution Forecast",
            updatedAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(java.util.Date())
        )
    }

    private fun getWeatherCondition(code: Int): String {
        return when (code) {
            0 -> "Clear sky"
            1, 2, 3 -> "Partly cloudy"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rain"
            80, 81, 82 -> "Rain showers"
            95, 96, 99 -> "Thunderstorm"
            else -> "Cloudy"
        }
    }

    private fun createFallbackRain(lat: Double, lon: Double): RainPredictionResponse {
        return RainPredictionResponse(
            location = RainLocationModel(lat, lon, "Asia/Dhaka"),
            current = CurrentRainModel(
                precipitationMm = 0.0,
                rainMm = 0.0,
                showersMm = 0.0,
                weatherCode = 2,
                condition = "Partly cloudy",
                temperature = 29.0,
                humidity = 72,
                windSpeed = 10.0,
                windDirection = 180
            ),
            summary24h = RainSummary24hModel(
                maxRainProbability = 25,
                totalExpectedRainfallMm = 0.5,
                rainExpected = false,
                intensity = "Light",
                predictedRainPeriods = emptyList(),
                advisoryEn = "Isolated light drizzle possible. Generally clear travel conditions.",
                advisoryBn = "বিচ্ছিন্ন গুঁড়ি গুঁড়ি বৃষ্টির সামান্য সম্ভাবনা। সামগ্রিক অবস্থা স্বাভাবিক।"
            ),
            hourly = emptyList(),
            daily = emptyList(),
            source = "Weather Alert Bangladesh Offline Baseline",
            updatedAt = ""
        )
    }
}
