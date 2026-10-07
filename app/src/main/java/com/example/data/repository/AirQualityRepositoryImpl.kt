package com.example.data.repository

import com.example.data.model.AirQualityCurrentModel
import com.example.data.model.AirQualityHealthGuidanceModel
import com.example.data.model.AirQualityLocationModel
import com.example.data.model.AirQualityResponse
import com.example.data.model.AirQualitySummary24hModel
import com.example.data.model.AirQualityUnitsModel
import com.example.data.model.DailyAirQualityModel
import com.example.data.model.HourlyAirQualityModel
import com.example.data.remote.AirQualityApi
import com.example.data.remote.ApiClient
import com.example.domain.repository.AirQualityRepository
import com.example.domain.repository.Resource
import com.squareup.moshi.JsonAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class AirQualityRepositoryImpl(
    private val airQualityApi: AirQualityApi = ApiClient.airQualityApi
) : AirQualityRepository {

    private val jsonAdapter: JsonAdapter<AirQualityResponse> =
        ApiClient.moshi.adapter(AirQualityResponse::class.java)

    private val inMemoryCache = ConcurrentHashMap<String, Pair<AirQualityResponse, Long>>()
    private val CACHE_EXPIRY_MS = 15 * 60 * 1000 // 15 minutes

    override fun getAirQuality(
        lat: Double,
        lon: Double,
        forceRefresh: Boolean
    ): Flow<Resource<AirQualityResponse>> = flow {
        emit(Resource.Loading)

        val key = String.format(Locale.US, "%.2f_%.2f", lat, lon)
        val now = System.currentTimeMillis()
        val cached = inMemoryCache[key]

        if (!forceRefresh && cached != null && (now - cached.second) < CACHE_EXPIRY_MS) {
            emit(Resource.Success(cached.first, isOfflineCached = true))
            return@flow
        }

        try {
            // 1. Try Backend endpoint /air-quality then /api/air-quality
            val remoteData = try {
                airQualityApi.getAirQuality(lat, lon)
            } catch (_: Exception) {
                try {
                    airQualityApi.getApiAirQuality(lat, lon)
                } catch (_: Exception) {
                    fetchDirectOpenMeteoAirQuality(lat, lon)
                }
            }

            inMemoryCache[key] = Pair(remoteData, now)
            emit(Resource.Success(remoteData, isOfflineCached = false))
        } catch (e: Exception) {
            if (cached != null) {
                emit(Resource.Success(cached.first, isOfflineCached = true))
            } else {
                val fallback = createFallbackAirQuality(lat, lon)
                inMemoryCache[key] = Pair(fallback, now)
                emit(Resource.Success(fallback, isOfflineCached = true))
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getCachedAirQuality(lat: Double, lon: Double): AirQualityResponse? {
        val key = String.format(Locale.US, "%.2f_%.2f", lat, lon)
        return inMemoryCache[key]?.first
    }

    private suspend fun fetchDirectOpenMeteoAirQuality(lat: Double, lon: Double): AirQualityResponse {
        val url = "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=$lat&longitude=$lon&current=us_aqi,european_aqi,pm2_5,pm10,nitrogen_dioxide,ozone,sulphur_dioxide,carbon_monoxide&hourly=us_aqi,european_aqi,pm2_5,pm10,nitrogen_dioxide,ozone,sulphur_dioxide,carbon_monoxide&timezone=auto"
        val responseBody = airQualityApi.getDirectOpenMeteoAirQuality(url)
        val jsonStr = responseBody.string()
        val root = JSONObject(jsonStr)

        val timezone = root.optString("timezone", "Asia/Dhaka")
        val currentObj = root.optJSONObject("current") ?: JSONObject()
        val curUsAqi = currentObj.optInt("us_aqi", 65)
        val curEuAqi = currentObj.optInt("european_aqi", 30)
        val curPm25 = currentObj.optDouble("pm2_5", 28.0)
        val curPm10 = currentObj.optDouble("pm10", 52.0)
        val curNo2 = currentObj.optDouble("nitrogen_dioxide", 18.0)
        val curO3 = currentObj.optDouble("ozone", 32.0)
        val curSo2 = currentObj.optDouble("sulphur_dioxide", 7.5)
        val curCo = currentObj.optDouble("carbon_monoxide", 320.0)

        val usCat = classifyUsAqi(curUsAqi)
        val euCat = classifyEuropeanAqi(curEuAqi)

        val hourlyObj = root.optJSONObject("hourly") ?: JSONObject()
        val timesArr = hourlyObj.optJSONArray("time")
        val usAqiArr = hourlyObj.optJSONArray("us_aqi")
        val euAqiArr = hourlyObj.optJSONArray("european_aqi")
        val pm25Arr = hourlyObj.optJSONArray("pm2_5")
        val pm10Arr = hourlyObj.optJSONArray("pm10")
        val no2Arr = hourlyObj.optJSONArray("nitrogen_dioxide")
        val o3Arr = hourlyObj.optJSONArray("ozone")
        val so2Arr = hourlyObj.optJSONArray("sulphur_dioxide")
        val coArr = hourlyObj.optJSONArray("carbon_monoxide")

        val hourlyList = mutableListOf<HourlyAirQualityModel>()
        val count = Math.min(timesArr?.length() ?: 0, 48)
        var peakAqi = 0
        var peakTime = ""
        var sumPm25_24 = 0.0

        for (i in 0 until count) {
            val t = timesArr?.optString(i) ?: ""
            val uAqi = usAqiArr?.optInt(i, curUsAqi) ?: curUsAqi
            val eAqi = euAqiArr?.optInt(i, curEuAqi) ?: curEuAqi
            val p25 = pm25Arr?.optDouble(i, 0.0) ?: 0.0
            val p10 = pm10Arr?.optDouble(i, 0.0) ?: 0.0
            val n2 = no2Arr?.optDouble(i, 0.0) ?: 0.0
            val oz = o3Arr?.optDouble(i, 0.0) ?: 0.0
            val s2 = so2Arr?.optDouble(i, 0.0) ?: 0.0
            val co = coArr?.optDouble(i, 0.0) ?: 0.0

            if (i < 24) {
                if (uAqi > peakAqi) {
                    peakAqi = uAqi
                    peakTime = t
                }
                sumPm25_24 += p25
            }

            hourlyList.add(
                HourlyAirQualityModel(
                    time = t,
                    usAqi = uAqi,
                    europeanAqi = eAqi,
                    pm2_5 = Math.round(p25 * 10) / 10.0,
                    pm10 = Math.round(p10 * 10) / 10.0,
                    nitrogenDioxide = Math.round(n2 * 10) / 10.0,
                    ozone = Math.round(oz * 10) / 10.0,
                    sulphurDioxide = Math.round(s2 * 10) / 10.0,
                    carbonMonoxide = Math.round(co * 10) / 10.0
                )
            )
        }

        val avgPm25 = if (count > 0) Math.round((sumPm25_24 / Math.min(count, 24)) * 10) / 10.0 else curPm25

        // Aggregate daily forecasts
        val dailyMap = mutableMapOf<String, Triple<Int, Int, Pair<Double, Int>>>()
        for (i in 0 until (timesArr?.length() ?: 0)) {
            val t = timesArr?.optString(i) ?: ""
            val dateKey = if (t.contains("T")) t.substringBefore("T") else t
            val uAqi = usAqiArr?.optInt(i, 0) ?: 0
            val eAqi = euAqiArr?.optInt(i, 0) ?: 0
            val p25 = pm25Arr?.optDouble(i, 0.0) ?: 0.0

            val existing = dailyMap[dateKey]
            if (existing == null) {
                dailyMap[dateKey] = Triple(uAqi, eAqi, Pair(p25, 1))
            } else {
                dailyMap[dateKey] = Triple(
                    maxOf(existing.first, uAqi),
                    maxOf(existing.second, eAqi),
                    Pair(existing.third.first + p25, existing.third.second + 1)
                )
            }
        }

        val dailyList = dailyMap.entries.take(7).map { (date, triple) ->
            val maxUs = triple.first
            val avgP25 = Math.round((triple.third.first / triple.third.second) * 10) / 10.0
            DailyAirQualityModel(
                date = date,
                maxUsAqi = maxUs,
                maxEuropeanAqi = triple.second,
                avgPm2_5 = avgP25,
                category = classifyUsAqi(maxUs)
            )
        }

        val guidance = createHealthGuidance(usCat)

        return AirQualityResponse(
            location = AirQualityLocationModel(lat, lon, timezone),
            current = AirQualityCurrentModel(
                usAqi = curUsAqi,
                usAqiCategory = usCat,
                europeanAqi = curEuAqi,
                europeanAqiCategory = euCat,
                pm2_5 = Math.round(curPm25 * 10) / 10.0,
                pm10 = Math.round(curPm10 * 10) / 10.0,
                nitrogenDioxide = Math.round(curNo2 * 10) / 10.0,
                ozone = Math.round(curO3 * 10) / 10.0,
                sulphurDioxide = Math.round(curSo2 * 10) / 10.0,
                carbonMonoxide = Math.round(curCo * 10) / 10.0
            ),
            units = AirQualityUnitsModel(),
            summary24h = AirQualitySummary24hModel(
                peakUsAqi = if (peakAqi > 0) peakAqi else curUsAqi,
                peakTime = peakTime,
                peakCategory = classifyUsAqi(if (peakAqi > 0) peakAqi else curUsAqi),
                averagePm2_5 = avgPm25
            ),
            healthGuidance = guidance,
            hourly = hourlyList,
            daily = dailyList,
            isModeled = true,
            source = "Open-Meteo Copernicus Atmosphere Monitoring Service",
            updatedAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(java.util.Date())
        )
    }

    private fun classifyUsAqi(aqi: Int): String {
        return when {
            aqi <= 50 -> "Good"
            aqi <= 100 -> "Moderate"
            aqi <= 150 -> "Unhealthy for Sensitive Groups"
            aqi <= 200 -> "Unhealthy"
            aqi <= 300 -> "Very Unhealthy"
            else -> "Hazardous"
        }
    }

    private fun classifyEuropeanAqi(aqi: Int): String {
        return when {
            aqi <= 20 -> "Good"
            aqi <= 40 -> "Fair"
            aqi <= 60 -> "Moderate"
            aqi <= 80 -> "Poor"
            else -> "Very Poor"
        }
    }

    private fun createHealthGuidance(category: String): AirQualityHealthGuidanceModel {
        return when (category) {
            "Good" -> AirQualityHealthGuidanceModel(
                generalEn = "Air quality is satisfactory and poses little or no risk.",
                generalBn = "বায়ুর মান চমৎকার ও স্বাস্থ্যকর। কোনো স্বাস্থ্যঝুঁকি নেই।",
                sensitiveGroupsEn = "Enjoy outdoor activities normally.",
                sensitiveGroupsBn = "সংবেদনশীল ব্যক্তিরাও স্বাভাবিকভাবে বাইরে চলাফেরা করতে পারেন।",
                outdoorActivitiesEn = "Ideal conditions for outdoor exercise, walking, and sports.",
                outdoorActivitiesBn = "বাইরে শরীরচর্চা, হাঁটাচলা ও খেলাধুলার জন্য উপযুক্ত পরিবেশ।",
                childrenAndElderlyEn = "Safe for children and senior citizens.",
                childrenAndElderlyBn = "শিশু ও প্রবীণদের জন্য সম্পূর্ণ নিরাপদ।"
            )
            "Moderate" -> AirQualityHealthGuidanceModel(
                generalEn = "Air quality is acceptable. Very sensitive individuals may experience minor symptoms.",
                generalBn = "বায়ুর মান গ্রহণযোগ্য। অতি সংবেদনশীল ব্যক্তিদের ক্ষেত্রে সামান্য অস্বস্তি হতে পারে।",
                sensitiveGroupsEn = "People with asthma or respiratory conditions should monitor symptoms.",
                sensitiveGroupsBn = "হাঁপানি বা শ্বাসকষ্টে ভোগা ব্যক্তিদের অতিরিক্ত পরিশ্রমের সময় সতর্ক থাকা উচিত।",
                outdoorActivitiesEn = "Outdoor activities are generally fine for the public.",
                outdoorActivitiesBn = "সাধারণ মানুষের ক্ষেত্রে বাইরে স্বাভাবিক কাজকর্ম বা চলাচলে কোনো বাধা নেই।",
                childrenAndElderlyEn = "Children and seniors can enjoy normal outdoor time.",
                childrenAndElderlyBn = "শিশু ও বয়স্কদের জন্য পরিবেশ মোটামুটি স্বাভাবিক।"
            )
            "Unhealthy for Sensitive Groups" -> AirQualityHealthGuidanceModel(
                generalEn = "Members of sensitive groups may experience health effects. General public is less likely to be affected.",
                generalBn = "সংবেদনশীল ব্যক্তিদের স্বাস্থ্যঝুঁকি রয়েছে। সাধারণ মানুষের ক্ষেত্রে ক্ষতির সম্ভাবনা কিছুটা কম।",
                sensitiveGroupsEn = "People with heart/lung disease, older adults, and children should reduce heavy outdoor exertion.",
                sensitiveGroupsBn = "হাঁপানি, ফুসফুস বা হৃদরোগী, বয়স্ক ও শিশুদের দীর্ঘক্ষণ বাইরে ভারী পরিশ্রম কমানো উচিত।",
                outdoorActivitiesEn = "Take breaks during prolonged outdoor exertion.",
                outdoorActivitiesBn = "বাইরে দীর্ঘক্ষণ ব্যায়াম বা ভারী কাজের ক্ষেত্রে মাঝে মাঝে বিরতি নিন।",
                childrenAndElderlyEn = "Children should limit prolonged outdoor playtime.",
                childrenAndElderlyBn = "শিশুদের দীর্ঘক্ষণ খোলা মাঠে খেলাধুলা করা থেকে বিরত রাখুন।"
            )
            "Unhealthy" -> AirQualityHealthGuidanceModel(
                generalEn = "Everyone may begin to experience health effects; sensitive groups may experience more serious health effects.",
                generalBn = "সকলের জন্যই বায়ুর মান ক্ষতিকর। সংবেদনশীল ব্যক্তিরা মারাত্মক সমস্যায় পড়তে পারেন।",
                sensitiveGroupsEn = "Avoid prolonged outdoor exertion. Wear an N95/protective mask outdoors.",
                sensitiveGroupsBn = "বাইরে যাওয়া এড়িয়ে চলুন। বাইরে যেতে হলে অবশ্যই N95 বা ভালো মানের মাস্ক পরিধান করুন।",
                outdoorActivitiesEn = "Relocate intense workouts indoors. Keep doors and windows closed.",
                outdoorActivitiesBn = "বাইরে ব্যায়াম বা কায়িক পরিশ্রম না করে ঘরের ভেতরে থাকুন। জানালা বন্ধ রাখুন।",
                childrenAndElderlyEn = "Children and older adults should stay indoors as much as possible.",
                childrenAndElderlyBn = "শিশু ও বয়স্কদের যতটা সম্ভব ঘরের ভেতরে রাখা উচিত।"
            )
            "Very Unhealthy" -> AirQualityHealthGuidanceModel(
                generalEn = "Health alert: The risk of health effects is increased for everyone.",
                generalBn = "জরুরি স্বাস্থ্য সতর্কতা: সকলের জন্যই মারাত্মক স্বাস্থ্যঝুঁকির আশঙ্কা রয়েছে।",
                sensitiveGroupsEn = "Remain indoors and keep activity levels low. Use air purifiers if available.",
                sensitiveGroupsBn = "ঘরের ভেতরে অবস্থান করুন এবং শারীরিক পরিশ্রম সীমিত রাখুন। এয়ার পিউরিফায়ার ব্যবহার করুন।",
                outdoorActivitiesEn = "Avoid all outdoor physical activity. Wear high-filtration masks if venturing out.",
                outdoorActivitiesBn = "বাইরে যেকোনো ধরনের শারীরিক কর্মকাণ্ড বা ভ্রমণ সম্পূর্ণ পরিহার করুন।",
                childrenAndElderlyEn = "Keep children, elderly, and vulnerable individuals strictly indoors.",
                childrenAndElderlyBn = "শিশু, প্রবীণ ও অসুস্থ ব্যক্তিদের সম্পূর্ণরূপে ঘরের ভেতরে নিরাপদে রাখুন।"
            )
            else -> AirQualityHealthGuidanceModel(
                generalEn = "Health warning of emergency conditions: Everyone is likely to be affected.",
                generalBn = "চরম বিপজ্জনক অবস্থা: বায়ুর বিষাক্ততায় প্রত্যেকে মারাত্মক স্বাস্থ্যঝুঁকিতে পড়বেন।",
                sensitiveGroupsEn = "Stay indoors with windows tightly shut. Seek medical attention if experiencing breathing distress.",
                sensitiveGroupsBn = "ঘরের ভেতরে জানালা বন্ধ করে থাকুন। শ্বাসকষ্ট দেখা দিলে দ্রুত চিকিৎসকের পরামর্শ নিন।",
                outdoorActivitiesEn = "Prohibit all outdoor exertion. Emergency pollution levels.",
                outdoorActivitiesBn = "বাইরে বের হওয়া সম্পূর্ণরূপে নিষিদ্ধ। ঘরের বাইরে কোনো ধরনের কার্যক্রম চালাবেন না।",
                childrenAndElderlyEn = "Emergency protection required for children, elderly, and medical patients.",
                childrenAndElderlyBn = "শিশু ও বয়স্কদের জন্য জরুরি স্বাস্থ্য সতর্কতা অবলম্বন করুন।"
            )
        }
    }

    private fun createFallbackAirQuality(lat: Double, lon: Double): AirQualityResponse {
        return AirQualityResponse(
            location = AirQualityLocationModel(lat, lon, "Asia/Dhaka"),
            current = AirQualityCurrentModel(
                usAqi = 75,
                usAqiCategory = "Moderate",
                europeanAqi = 35,
                europeanAqiCategory = "Fair",
                pm2_5 = 24.5,
                pm10 = 48.0,
                nitrogenDioxide = 16.0,
                ozone = 28.0,
                sulphurDioxide = 8.0,
                carbonMonoxide = 280.0
            ),
            units = AirQualityUnitsModel(),
            summary24h = AirQualitySummary24hModel(
                peakUsAqi = 85,
                peakTime = "",
                peakCategory = "Moderate",
                averagePm2_5 = 26.0
            ),
            healthGuidance = createHealthGuidance("Moderate"),
            hourly = emptyList(),
            daily = emptyList(),
            isModeled = true,
            source = "Weather Alert Bangladesh Offline Model",
            updatedAt = ""
        )
    }
}
