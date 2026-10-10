package com.example.data.repository

import android.content.Context
import com.example.data.model.UpazilaBbsRecord
import com.example.data.model.UpazilaDailyForecastItem
import com.example.data.model.UpazilaForecastAlert
import com.example.data.model.UpazilaForecastEnvelope
import com.example.data.model.UpazilaSubDailyForecastItem
import com.example.data.model.UpazilaUiForecast
import com.example.data.remote.UpazilaForecastApi
import com.example.domain.repository.Resource
import com.example.domain.repository.UpazilaForecastRepository
import com.example.util.BanglaUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class UpazilaForecastRepositoryImpl(
    private val api: UpazilaForecastApi,
    private val context: Context,
    private val moshi: Moshi
) : UpazilaForecastRepository {

    private var cachedUpazilas: List<UpazilaBbsRecord>? = null

    // In-memory cache for forecast to reduce redundant API calls and enable fast UI transitions
    private val forecastCache = mutableMapOf<String, Pair<Long, UpazilaUiForecast>>()
    private val CACHE_EXPIRY_MS = 15 * 60 * 1000L // 15 mins

    override suspend fun getUpazilas(): List<UpazilaBbsRecord> = withContext(Dispatchers.IO) {
        cachedUpazilas?.let { return@withContext it }

        try {
            val jsonString = context.assets.open("upazilas_bbs.json").bufferedReader().use { it.readText() }
            val listType = Types.newParameterizedType(List::class.java, UpazilaBbsRecord::class.java)
            val adapter = moshi.adapter<List<UpazilaBbsRecord>>(listType)
            val list = adapter.fromJson(jsonString) ?: emptyList()
            cachedUpazilas = list
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun findNearestUpazila(lat: Double, lon: Double): UpazilaBbsRecord? = withContext(Dispatchers.Default) {
        val upazilas = getUpazilas()
        if (upazilas.isEmpty()) return@withContext null

        var minDistance = Double.MAX_VALUE
        var nearest: UpazilaBbsRecord? = null

        for (u in upazilas) {
            val dist = haversineDistanceKm(lat, lon, u.lat, u.lon)
            if (dist < minDistance) {
                minDistance = dist
                nearest = u
            }
        }
        nearest
    }

    override suspend fun findUpazilaByPcode(pcode: String): UpazilaBbsRecord? = withContext(Dispatchers.Default) {
        val clean = pcode.replace("BD", "").trim()
        val upazilas = getUpazilas()
        upazilas.firstOrNull { it.pcode == clean || it.pcodeRaw == pcode }
    }

    private fun haversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    override fun getUpazilaForecast(
        pcode: String,
        source: String,
        forceRefresh: Boolean
    ): Flow<Resource<UpazilaUiForecast>> = flow {
        val cleanPcode = pcode.replace("BD", "").trim()
        val cacheKey = "${source}_$cleanPcode"

        val cached = forecastCache[cacheKey]
        val now = System.currentTimeMillis()
        if (!forceRefresh && cached != null && (now - cached.first < CACHE_EXPIRY_MS)) {
            emit(Resource.Success(cached.second))
            return@flow
        }

        // Emit loading
        emit(Resource.Loading)

        val allParams = listOf(
            "rf", "temp", "rh", "tempdew", "smois",
            "windspd", "winddir", "cldcvr", "windgust",
            "thi", "tempbc", "thi_broilers", "thi_layers"
        )

        try {
            // Fetch recent daily forecast and steps forecast in parallel or sequentially
            val recentResp = api.getRecentForecast(
                source = source,
                params = allParams,
                pcode = cleanPcode
            )

            val stepsResp = try {
                api.getStepsForecast(
                    source = source,
                    params = listOf("rf", "temp", "rh", "windspd"),
                    pcode = cleanPcode
                )
            } catch (e: Exception) {
                null
            }

            val recentPayload = recentResp.getPayloadFor(cleanPcode)
            if (recentPayload == null) {
                if (cached != null) {
                    emit(Resource.Error("পূর্বাভাস পাওয়া যায়নি: ${recentResp.error ?: "ডেটা অনুপস্থিত"}", cached.second))
                } else {
                    emit(Resource.Error("পূর্বাভাস পাওয়া যায়নি: ${recentResp.error ?: "ডেটা অনুপস্থিত"}"))
                }
                return@flow
            }

            val upazilaMeta = findUpazilaByPcode(cleanPcode)
            val uiForecast = mapToUiForecast(
                pcode = cleanPcode,
                source = source,
                updatedAt = recentResp.updatedAt,
                payload = recentPayload,
                stepsPayload = stepsResp?.getPayloadFor(cleanPcode),
                meta = upazilaMeta
            )

            forecastCache[cacheKey] = Pair(now, uiForecast)
            emit(Resource.Success(uiForecast))

        } catch (e: Exception) {
            e.printStackTrace()
            val msg = e.localizedMessage ?: "নেটওয়ার্ক বা সার্ভার সমস্যা"
            if (cached != null) {
                emit(Resource.Error("পূর্বাভাস লোড করা যায়নি ($msg)", cached.second))
            } else {
                emit(Resource.Error("পূর্বাভাস লোড করা যায়নি ($msg)"))
            }
        }
    }.flowOn(Dispatchers.IO)

    override fun getForecastByDate(
        pcode: String,
        fdate: String,
        source: String
    ): Flow<Resource<UpazilaUiForecast>> = flow {
        val cleanPcode = pcode.replace("BD", "").trim()
        emit(Resource.Loading)

        try {
            val resp = api.getForecastByDate(
                source = source,
                fdate = fdate,
                params = listOf("rf", "temp", "rh", "windspd"),
                pcode = cleanPcode
            )
            val payload = resp.getPayloadFor(cleanPcode)
            if (payload == null) {
                emit(Resource.Error("নির্দিষ্ট তারিখের ($fdate) জন্য কোনো পূর্বাভাস পাওয়া যায়নি"))
                return@flow
            }

            val upazilaMeta = findUpazilaByPcode(cleanPcode)
            val uiForecast = mapToUiForecast(
                pcode = cleanPcode,
                source = source,
                updatedAt = fdate,
                payload = payload,
                stepsPayload = null,
                meta = upazilaMeta
            )
            emit(Resource.Success(uiForecast))
        } catch (e: Exception) {
            emit(Resource.Error("তারিখ অনুযায়ী পূর্বাভাস লোড করা যায়নি: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    private fun mapToUiForecast(
        pcode: String,
        source: String,
        updatedAt: String?,
        payload: com.example.data.model.UpazilaForecastDataPayload,
        stepsPayload: com.example.data.model.UpazilaForecastDataPayload?,
        meta: UpazilaBbsRecord?
    ): UpazilaUiForecast {
        val upazilaName = payload.upazilaName?.ifBlank { meta?.name } ?: meta?.name ?: "উপজেলা"
        val districtName = payload.districtName?.ifBlank { meta?.district } ?: meta?.district ?: "জেলা"
        val divisionName = payload.divisionName?.ifBlank { meta?.division } ?: meta?.division ?: "বিভাগ"

        val updatedBn = if (!updatedAt.isNullOrBlank()) {
            formatUpdatedAtToBangla(updatedAt)
        } else {
            "সর্বশেষ হালনাগাদ"
        }

        val forecastData = payload.forecastData
        val tempList = forecastData["temp"] ?: emptyList()
        val rfList = forecastData["rf"] ?: emptyList()
        val rhList = forecastData["rh"] ?: emptyList()
        val windList = forecastData["windspd"] ?: emptyList()
        val cldList = forecastData["cldcvr"] ?: emptyList()
        val gustList = forecastData["windgust"] ?: emptyList()
        val smoisList = forecastData["smois"] ?: emptyList()

        val dailyCount = maxOf(tempList.size, rfList.size)
        val dailyItems = mutableListOf<UpazilaDailyForecastItem>()

        for (i in 0 until dailyCount) {
            val tempItem = tempList.getOrNull(i)
            val rfItem = rfList.getOrNull(i)
            val rhItem = rhList.getOrNull(i)
            val windItem = windList.getOrNull(i)
            val cldItem = cldList.getOrNull(i)
            val gustItem = gustList.getOrNull(i)
            val smoisItem = smoisList.getOrNull(i)

            val rawDate = tempItem?.stepStart ?: rfItem?.stepStart ?: ""
            val dateLabelBn = formatDateToBangla(rawDate, i)

            dailyItems.add(
                UpazilaDailyForecastItem(
                    dateLabelBn = dateLabelBn,
                    tempMin = tempItem?.valMin,
                    tempMax = tempItem?.valMax,
                    tempAvg = tempItem?.valAvg,
                    rainfallMm = rfItem?.valMax ?: rfItem?.valAvg ?: 0.0,
                    humidityPercent = rhItem?.valAvg,
                    windSpeedKmh = windItem?.valAvg,
                    cloudCoverPercent = cldItem?.valAvg,
                    windGustKmh = gustItem?.valMax,
                    soilMoisture = smoisItem?.valAvg
                )
            )
        }

        // Sub-daily steps (3-hour intervals from stepsPayload)
        val subDailyItems = mutableListOf<UpazilaSubDailyForecastItem>()
        if (stepsPayload != null) {
            val stepTemps = stepsPayload.forecastData["temp"] ?: emptyList()
            val stepRfs = stepsPayload.forecastData["rf"] ?: emptyList()
            val stepRhs = stepsPayload.forecastData["rh"] ?: emptyList()
            val stepWinds = stepsPayload.forecastData["windspd"] ?: emptyList()

            // Take the first 16 steps (2 days of 3-hr steps)
            val count = minOf(stepTemps.size, 16)
            for (i in 0 until count) {
                val sTemp = stepTemps.getOrNull(i)
                val sRf = stepRfs.getOrNull(i)
                val sRh = stepRhs.getOrNull(i)
                val sWind = stepWinds.getOrNull(i)

                val start = sTemp?.stepStart ?: ""
                val labelBn = formatStepTimeToBangla(start)

                subDailyItems.add(
                    UpazilaSubDailyForecastItem(
                        timeLabelBn = labelBn,
                        temp = sTemp?.valAvg,
                        rainfallMm = sRf?.valMax ?: sRf?.valAvg ?: 0.0,
                        humidityPercent = sRh?.valAvg,
                        windSpeedKmh = sWind?.valAvg
                    )
                )
            }
        }

        // Check for severe weather alerts in forecast
        val alerts = mutableListOf<UpazilaForecastAlert>()

        // Heavy Rain check (if any day has >= 25 mm rain)
        val maxRain = dailyItems.maxOfOrNull { it.rainfallMm ?: 0.0 } ?: 0.0
        if (maxRain >= 44.0) {
            alerts.add(
                UpazilaForecastAlert(
                    titleBn = "অতি ভারী বর্ষণের সতর্কতা",
                    descriptionBn = "এই উপজেলায় সর্বোচ্চ ${BanglaUtils.toBanglaDigits(maxRain.toInt())} মিমি অতি ভারী বৃষ্টিপাতের পূর্বাভাস রয়েছে।",
                    isSevere = true
                )
            )
        } else if (maxRain >= 22.0) {
            alerts.add(
                UpazilaForecastAlert(
                    titleBn = "ভারী বর্ষণের পূর্বাভাস",
                    descriptionBn = "উপজেলায় সর্বোচ্চ ${BanglaUtils.toBanglaDigits(maxRain.toInt())} মিমি পর্যন্ত ভারী বৃষ্টিপাত হতে পারে।",
                    isSevere = false
                )
            )
        }

        // High Temp check
        val maxTemp = dailyItems.maxOfOrNull { it.tempMax ?: 0.0 } ?: 0.0
        if (maxTemp >= 38.0) {
            alerts.add(
                UpazilaForecastAlert(
                    titleBn = "তীব্র তাপপ্রবাহের পূর্বাভাস",
                    descriptionBn = "দিনের তাপমাত্রা সর্বোচ্চ ${BanglaUtils.toBanglaDigits(maxTemp.toInt())}°C পর্যন্ত বৃদ্ধি পেতে পারে।",
                    isSevere = true
                )
            )
        } else if (maxTemp >= 36.0) {
            alerts.add(
                UpazilaForecastAlert(
                    titleBn = "মৃদু থেকে মাঝারি তাপপ্রবাহ",
                    descriptionBn = "সর্বোচ্চ তাপমাত্রা প্রায় ${BanglaUtils.toBanglaDigits(maxTemp.toInt())}°C হতে পারে। পর্যাপ্ত পানি পান করুন।",
                    isSevere = false
                )
            )
        }

        // Wind gust alert
        val maxWind = dailyItems.maxOfOrNull { it.windGustKmh ?: it.windSpeedKmh ?: 0.0 } ?: 0.0
        if (maxWind >= 50.0) {
            alerts.add(
                UpazilaForecastAlert(
                    titleBn = "দমকা বা ঝড়ো হাওয়ার সতর্কতা",
                    descriptionBn = "বাতাসের গতিবেগ ঘণ্টায় ${BanglaUtils.toBanglaDigits(maxWind.toInt())} কিমি পর্যন্ত পৌঁছাতে পারে।",
                    isSevere = true
                )
            )
        }

        return UpazilaUiForecast(
            upazilaName = upazilaName,
            districtName = districtName,
            divisionName = divisionName,
            pcode = pcode,
            source = source,
            updatedAtFormattedBn = updatedBn,
            dailySteps = dailyItems,
            subDailySteps = subDailyItems,
            alerts = alerts
        )
    }

    private fun formatDateToBangla(dateStr: String, index: Int): String {
        if (index == 0) return "আজ"
        if (index == 1) return "আগামীকাল"

        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val d = parser.parse(dateStr)
            if (d != null) {
                val dayFormat = SimpleDateFormat("dd MMM, EEE", Locale.US)
                val formatted = dayFormat.format(d)
                // Convert day names and numbers to Bangla
                translateDateStringToBangla(formatted)
            } else {
                "${BanglaUtils.toBanglaDigits(index + 1)}ম দিন"
            }
        } catch (_: Exception) {
            "${BanglaUtils.toBanglaDigits(index + 1)}ম দিন"
        }
    }

    private fun formatStepTimeToBangla(stepStart: String): String {
        return try {
            // step_start format e.g. "2026-10-10T00:06:00"
            // The step times in the API use UTC or standard steps
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            parser.timeZone = TimeZone.getTimeZone("UTC")
            val date = parser.parse(stepStart)
            if (date != null) {
                // Bangladesh is UTC+6
                val bstFormat = SimpleDateFormat("hh:mm a, dd MMM", Locale.US)
                bstFormat.timeZone = TimeZone.getTimeZone("Asia/Dhaka")
                val formatted = bstFormat.format(date)
                translateDateStringToBangla(formatted)
            } else {
                stepStart
            }
        } catch (_: Exception) {
            stepStart
        }
    }

    private fun formatUpdatedAtToBangla(updatedAt: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val date = parser.parse(updatedAt.take(19))
            if (date != null) {
                val out = SimpleDateFormat("hh:mm a, dd MMM yyyy", Locale.US)
                val str = out.format(date)
                "হালনাগাদ: ${translateDateStringToBangla(str)}"
            } else {
                "হালনাগাদ: ${BanglaUtils.toBanglaDigits(updatedAt)}"
            }
        } catch (_: Exception) {
            "হালনাগাদ: ${BanglaUtils.toBanglaDigits(updatedAt)}"
        }
    }

    private fun translateDateStringToBangla(str: String): String {
        var res = str
            .replace("AM", "সকাল")
            .replace("PM", "বিকাল/রাত")
            .replace("Mon", "সোম")
            .replace("Tue", "মঙ্গল")
            .replace("Wed", "বুধ")
            .replace("Thu", "বৃহস্পতি")
            .replace("Fri", "শুক্র")
            .replace("Sat", "শনি")
            .replace("Sun", "রবি")
            .replace("Jan", "জানু")
            .replace("Feb", "ফেব্রু")
            .replace("Mar", "মার্চ")
            .replace("Apr", "এপ্রিল")
            .replace("May", "মে")
            .replace("Jun", "জুন")
            .replace("Jul", "জুলাই")
            .replace("Aug", "আগস্ট")
            .replace("Sep", "সেপ্টে")
            .replace("Oct", "অক্টো")
            .replace("Nov", "নভে")
            .replace("Dec", "ডিসে")

        return BanglaUtils.toBanglaDigits(res)
    }
}
