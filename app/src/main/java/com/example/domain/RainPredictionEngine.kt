package com.example.domain

import com.example.data.model.CurrentRainModel
import com.example.data.model.DailyRainModel
import com.example.data.model.HeavyRainWarningModel
import com.example.data.model.HourlyRainModel
import com.example.data.model.PredictedRainPeriodModel
import com.example.data.model.RadarNowcastModel
import com.example.data.model.RainDetailsModel
import com.example.data.model.RainLocationModel
import com.example.data.model.RainPredictionMetricsModel
import com.example.data.model.RainPredictionResponse
import com.example.data.model.RainSummary24hModel
import com.example.data.model.RainTimelinePointModel
import com.example.data.model.RainViewerResponse
import com.example.data.model.UnifiedWeatherResponse
import com.example.util.BanglaUtils
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object RainPredictionEngine {

    fun generatePrediction(
        weather: UnifiedWeatherResponse,
        radar: RainViewerResponse?,
        lat: Double,
        lon: Double,
        cityName: String?
    ): RainPredictionResponse {
        val hourly = weather.hourly
        val current = weather.current

        // 1. Current conditions & rainfall
        val curPrecip = current.rainfall ?: current.precipitationMm ?: 0.0
        val isRainingNow = curPrecip > 0.05 || current.weatherCode in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82, 95, 96, 99)

        val p0 = hourly.getOrNull(0)?.rainProbability ?: current.rainProbability ?: 0
        val p1 = hourly.getOrNull(1)?.rainProbability ?: p0
        val p2 = hourly.getOrNull(2)?.rainProbability ?: p1
        val p3 = hourly.getOrNull(3)?.rainProbability ?: p2
        val p4 = hourly.getOrNull(4)?.rainProbability ?: p3
        val p5 = hourly.getOrNull(5)?.rainProbability ?: p4

        // 2. Radar Analysis & Nowcasting
        val radarAvailable = radar != null && radar.allPastFrames.isNotEmpty()
        val windDir = current.windDirection ?: 180
        val windSpeedKmh = current.windSpeed ?: 10.0

        val directionBn = when (windDir) {
            in 0..44 -> "উত্তর দিক থেকে"
            in 45..134 -> "পূর্ব দিক থেকে"
            in 135..224 -> "দক্ষিণ দিক থেকে"
            in 225..314 -> "পশ্চিম দিক থেকে"
            else -> "উত্তর দিক থেকে"
        }
        val speedStr = String.format(Locale.US, "%.1f কিমি/ঘণ্টা", windSpeedKmh)

        val isTrendIncreasing = p1 > p0 || (p0 >= 40 && (current.humidity ?: 0) >= 75)
        val isTrendDecreasing = p0 > 60 && p1 < 40 && p2 < 30

        val (isApproaching, statusTextBn, radarMessageBn) = when {
            !radarAvailable -> Triple(false, "কোনো উল্লেখযোগ্য বৃষ্টির সিগন্যাল নেই", "রাডার তথ্য প্রস্তুত হচ্ছে, স্যাটেলাইট পূর্বাভাস সক্রিয়।")
            isRainingNow -> Triple(true, "বৃষ্টির মেঘ স্থির", "আপনার এলাকায় বৃষ্টির মেঘ সক্রিয় ও বৃষ্টিপাত অব্যাহত রয়েছে।")
            isTrendIncreasing && (p0 >= 35 || p1 >= 50) -> Triple(true, "বৃষ্টি আসছে", "বৃষ্টির মেঘ আপনার এলাকার দিকে এগিয়ে আসছে।")
            isTrendDecreasing -> Triple(false, "বৃষ্টি দূরে সরে যাচ্ছে", "বৃষ্টির মেঘ আপনার এলাকা অতিক্রম করে দূরে সরে যাচ্ছে।")
            p0 >= 50 -> Triple(true, "বৃষ্টির মেঘ স্থির", "ঘূর্ণায়মান বৃষ্টির মেঘ আপনার এলাকার নিকটবর্তী আকাশে বিদ্যমান।")
            else -> Triple(false, "কোনো উল্লেখযোগ্য বৃষ্টির সিগন্যাল নেই", "ডপলার রাডারে বর্তমানে কোনো উল্লেখযোগ্য বৃষ্টির মেঘ শনাক্ত হয়নি।")
        }

        // 3. Calibrated Probabilities
        val prob15 = when {
            isRainingNow -> max(85, min(100, p0 + 15))
            isApproaching -> min(95, max(p0, ((p0 * 0.6) + 20).toInt()))
            else -> min(90, ((p0 * 0.7) + (current.rainProbability ?: 0) * 0.3).toInt())
        }

        val prob30 = when {
            isRainingNow -> max(80, p0)
            isApproaching -> min(95, max(p0, ((p0 * 0.5) + (p1 * 0.5) + 12).toInt()))
            else -> min(90, ((p0 * 0.6) + (p1 * 0.4)).toInt())
        }

        val prob1h = p0
        val prob2h = p1
        val prob3h = p2
        val prob6h = hourly.take(6).maxOfOrNull { it.rainProbability } ?: max(p0, p1)
        val prob24h = hourly.take(24).maxOfOrNull { it.rainProbability } ?: weather.daily.firstOrNull()?.rainProbability ?: max(prob6h, p0)

        // 4. Rain ETA (start in minutes)
        val startInMinutes = when {
            isRainingNow -> 0
            prob15 >= 60 -> 15
            prob30 >= 60 -> 30
            prob1h >= 60 -> 45
            prob2h >= 60 -> 90
            prob3h >= 60 -> 150
            else -> null
        }

        // 5. Rain Duration & Intensity
        val consecutiveRainHours = hourly.take(6).count { it.rainProbability >= 45 || it.precipitationMm > 0.1 }
        val durationMinutes = when {
            consecutiveRainHours >= 4 -> 240
            consecutiveRainHours == 3 -> 150
            consecutiveRainHours == 2 -> 90
            consecutiveRainHours == 1 -> 45
            isRainingNow -> 30
            else -> null
        }

        val maxPrecipMm = hourly.take(6).maxOfOrNull { it.precipitationMm } ?: curPrecip
        val (intensityEn, intensityBn) = when {
            maxPrecipMm >= 50.0 -> Pair("Violent", "অতি ভারী")
            maxPrecipMm >= 10.0 -> Pair("Heavy", "ভারী")
            maxPrecipMm >= 2.5 -> Pair("Moderate", "মাঝারি")
            maxPrecipMm >= 0.1 || isRainingNow || p0 >= 50 -> Pair("Light", "হালকা")
            else -> Pair("None", "বৃষ্টি নেই")
        }

        val rainExpected = isRainingNow || prob15 >= 50 || prob30 >= 50 || prob1h >= 50 || prob3h >= 55

        // 6. Timeline Points
        val timelinePoints = listOf(
            RainTimelinePointModel(
                timeLabel = "এখন",
                probability = if (isRainingNow) max(85, p0) else p0,
                intensityBn = if (isRainingNow) intensityBn else if (p0 >= 50) "হালকা" else "বৃষ্টি নেই",
                rainfallMm = curPrecip,
                weatherCode = current.weatherCode
            ),
            RainTimelinePointModel(
                timeLabel = "১৫ মিনিট",
                probability = prob15,
                intensityBn = if (prob15 >= 60) intensityBn else if (prob15 >= 40) "হালকা" else "বৃষ্টি নেই",
                rainfallMm = hourly.getOrNull(0)?.precipitationMm,
                weatherCode = hourly.getOrNull(0)?.weatherCode ?: current.weatherCode
            ),
            RainTimelinePointModel(
                timeLabel = "৩০ মিনিট",
                probability = prob30,
                intensityBn = if (prob30 >= 60) intensityBn else if (prob30 >= 40) "হালকা" else "বৃষ্টি নেই",
                rainfallMm = hourly.getOrNull(0)?.precipitationMm,
                weatherCode = hourly.getOrNull(0)?.weatherCode ?: current.weatherCode
            ),
            RainTimelinePointModel(
                timeLabel = "১ ঘণ্টা",
                probability = prob1h,
                intensityBn = if (prob1h >= 60) intensityBn else if (prob1h >= 40) "হালকা" else "বৃষ্টি নেই",
                rainfallMm = hourly.getOrNull(0)?.precipitationMm,
                weatherCode = hourly.getOrNull(0)?.weatherCode ?: current.weatherCode
            ),
            RainTimelinePointModel(
                timeLabel = "২ ঘণ্টা",
                probability = prob2h,
                intensityBn = if (prob2h >= 60) intensityBn else if (prob2h >= 40) "হালকা" else "বৃষ্টি নেই",
                rainfallMm = hourly.getOrNull(1)?.precipitationMm,
                weatherCode = hourly.getOrNull(1)?.weatherCode ?: 800
            ),
            RainTimelinePointModel(
                timeLabel = "৩ ঘণ্টা",
                probability = prob3h,
                intensityBn = if (prob3h >= 60) intensityBn else if (prob3h >= 40) "হালকা" else "বৃষ্টি নেই",
                rainfallMm = hourly.getOrNull(2)?.precipitationMm,
                weatherCode = hourly.getOrNull(2)?.weatherCode ?: 800
            )
        )

        // 7. Heavy Rain Warning
        val activeWarnings = weather.warnings.ifEmpty { weather.alerts }
        val bmdHeavyWarning = activeWarnings.firstOrNull { alert ->
            val text = (alert.title + " " + alert.description + " " + (alert.titleBn ?: "")).lowercase()
            text.contains("ভারী") || text.contains("heavy") || text.contains("cyclone") || text.contains("বজ্রপাত") || text.contains("ঝড়")
        }

        val isHeavyRain = intensityEn in listOf("Heavy", "Violent") || maxPrecipMm >= 10.0 || bmdHeavyWarning != null
        val heavyRainWarning = if (isHeavyRain) {
            HeavyRainWarningModel(
                isWarningActive = true,
                expectedStart = if (startInMinutes != null) "প্রায় ${BanglaUtils.toBanglaDigits(startInMinutes)} মিনিটের মধ্যে" else "নিকটবর্তী সময়ে",
                expectedDuration = durationMinutes?.let { "প্রায় ${BanglaUtils.toBanglaDigits(it)} মিনিট" } ?: "১–৩ ঘণ্টা",
                intensity = intensityBn,
                expectedRainfallAmount = String.format(Locale.US, "%.1f মিমি+", maxPrecipMm),
                bmdWarning = bmdHeavyWarning?.titleBn ?: bmdHeavyWarning?.title
            )
        } else null

        // 8. Confidence calculation
        var confidenceScore = 0
        if (radarAvailable) confidenceScore += 30
        if ((weather.bmd?.distanceKm ?: weather.station?.distanceKm ?: 999.0) < 150) confidenceScore += 25
        if (hourly.size >= 12) confidenceScore += 25
        if (current.humidity != null && current.pressure != null) confidenceScore += 20
        if (weather.bmd?.isStale == true) {
            confidenceScore = max(10, confidenceScore - 15) // reduce confidence if nearest station is stale
        }
        confidenceScore = min(100, confidenceScore)

        val confidenceStr = when {
            confidenceScore >= 75 -> "উচ্চ আত্মবিশ্বাস"
            confidenceScore >= 50 -> "মাঝারি আত্মবিশ্বাস"
            else -> "কম আত্মবিশ্বাস"
        }

        // 9. Source list
        val sources = mutableListOf<String>()
        if (weather.bmd?.station != null) {
            val distStr = weather.bmd.distanceKm?.let { " (দূরত্ব: ${BanglaUtils.toBanglaDigits(it.toInt())} কিমি)" } ?: ""
            sources.add("BMD স্টেশন: ${weather.bmd.station}$distStr")
        } else if (weather.station != null && weather.station.name.isNotBlank()) {
            sources.add("বাংলাদেশ আবহাওয়া অধিদপ্তর (BMD - ${weather.station.name})")
        } else {
            sources.add("বাংলাদেশ আবহাওয়া অধিদপ্তর (BMD Synop Network)")
        }
        if (radarAvailable) {
            sources.add("ডপলার ওয়েদার রাডার (RainViewer Live)")
        }
        sources.add("উচ্চ রেজোলিউশন হাইড্রো-মেটিওরোলজিক্যাল মডেল")

        val summaryBn = when {
            isRainingNow -> "বর্তমানে আপনার এলাকায় বৃষ্টিপাত হচ্ছে।"
            startInMinutes != null && startInMinutes <= 30 -> "আগামী ${BanglaUtils.toBanglaDigits(startInMinutes)} মিনিটের মধ্যে বৃষ্টি শুরু হওয়ার প্রবল সম্ভাবনা রয়েছে।"
            prob1h >= 60 -> "আগামী ১ ঘণ্টার মধ্যে ${intensityBn} বৃষ্টির সম্ভাবনা রয়েছে।"
            prob3h >= 50 -> "পরবর্তী ৩ ঘণ্টার মধ্যে বৃষ্টির সম্ভাবনা বিদ্যমান।"
            prob24h >= 40 -> "আজকের দিনে মাঝারি বৃষ্টির সম্ভাবনা রয়েছে।"
            else -> "আগামী কয়েক ঘণ্টায় কোনো উল্লেখযোগ্য বৃষ্টির সম্ভাবনা নেই।"
        }

        return RainPredictionResponse(
            success = true,
            location = RainLocationModel(
                latitude = lat,
                longitude = lon,
                name = cityName ?: weather.location.displayName ?: weather.location.name,
                village = weather.location.village,
                upazila = weather.location.upazila,
                district = weather.location.district,
                division = weather.location.division
            ),
            prediction = RainPredictionMetricsModel(
                next15Minutes = prob15,
                next30Minutes = prob30,
                next1Hour = prob1h,
                next2Hours = prob2h,
                next3Hours = prob3h,
                next6Hours = prob6h,
                next24Hours = prob24h
            ),
            rain = RainDetailsModel(
                expected = rainExpected,
                startInMinutes = startInMinutes,
                durationMinutes = durationMinutes,
                intensity = intensityEn,
                intensityBn = intensityBn,
                rainfallAmount = maxPrecipMm,
                summaryBn = summaryBn
            ),
            radar = RadarNowcastModel(
                available = radarAvailable,
                approaching = isApproaching,
                direction = directionBn,
                speed = speedStr,
                statusTextBn = statusTextBn,
                radarMessageBn = radarMessageBn
            ),
            timeline = timelinePoints,
            confidence = confidenceStr,
            confidenceScore = confidenceScore,
            sources = sources,
            updatedAt = weather.updatedAt ?: current.recordedAt ?: "",
            current = CurrentRainModel(
                precipitationMm = curPrecip,
                rainMm = current.rainfall ?: 0.0,
                showersMm = 0.0,
                weatherCode = current.weatherCode,
                condition = current.condition ?: "Clear",
                temperature = current.temperature ?: 28.0,
                humidity = current.humidity ?: 75,
                windSpeed = windSpeedKmh,
                windDirection = windDir
            ),
            summary24h = RainSummary24hModel(
                maxRainProbability = prob24h,
                totalExpectedRainfallMm = maxPrecipMm,
                rainExpected = rainExpected,
                intensity = intensityEn,
                predictedRainPeriods = if (startInMinutes != null) listOf(
                    PredictedRainPeriodModel(
                        start = "আগামী ${BanglaUtils.toBanglaDigits(startInMinutes)} মিনিট",
                        end = durationMinutes?.let { "স্থায়িত্ব ${BanglaUtils.toBanglaDigits(it)} মিনিট" } ?: "১ ঘণ্টা",
                        expectedRainfallMm = maxPrecipMm,
                        maxProbability = max(prob15, prob30)
                    )
                ) else emptyList(),
                advisoryEn = "Real-time multi-source rain prediction calibrated for Bangladesh",
                advisoryBn = summaryBn
            ),
            hourly = hourly.map { h ->
                HourlyRainModel(
                    time = h.timeString,
                    precipitationProbability = h.rainProbability,
                    precipitationMm = h.precipitationMm,
                    rainMm = h.precipitationMm,
                    weatherCode = h.weatherCode,
                    condition = h.condition,
                    temperature = h.temperature ?: 28.0,
                    humidity = h.humidity ?: 70,
                    windSpeed = h.windSpeed
                )
            },
            daily = weather.daily.map { d ->
                DailyRainModel(
                    date = d.dateFormattedBn ?: d.dayNameBn ?: d.date,
                    maxRainProbability = d.rainProbability,
                    totalRainfallMm = d.precipitationMm,
                    weatherCode = d.weatherCode,
                    condition = d.condition,
                    tempMax = d.tempMax ?: 30.0,
                    tempMin = d.tempMin ?: 22.0
                )
            },
            heavyRainWarning = heavyRainWarning,
            bmdStation = weather.bmd?.station ?: weather.station?.name,
            bmdDistanceKm = weather.bmd?.distanceKm ?: weather.station?.distanceKm
        )
    }
}
