package com.example.data.model

data class CombinedIntelligence(
    val headlineEn: String,
    val headlineBn: String,
    val detailEn: String,
    val detailBn: String,
    val severity: IntelligenceSeverity,
    val rainRisk: Boolean,
    val pollutionRisk: Boolean
)

enum class IntelligenceSeverity {
    NORMAL,
    ADVISORY,
    WARNING,
    ALERT
}

object CombinedIntelligenceAnalyzer {
    fun analyze(
        weather: UnifiedWeatherResponse?,
        rain: RainPredictionResponse?,
        airQuality: AirQualityResponse?
    ): CombinedIntelligence {
        val rainProb = rain?.summary24h?.maxRainProbability ?: weather?.current?.rainProbability ?: 0
        val rainMm = rain?.summary24h?.totalExpectedRainfallMm ?: weather?.current?.precipitationMm ?: 0.0
        val aqi = airQuality?.current?.usAqi ?: 50
        val pm25 = airQuality?.current?.pm2_5 ?: 25.0

        val isHeavyRain = rainMm >= 25.0 || rain?.summary24h?.intensity == "Heavy" || rain?.summary24h?.intensity == "Violent"
        val isRainLikely = rainProb >= 60 || rainMm >= 3.0
        val isAqiHazardous = aqi >= 300
        val isAqiVeryUnhealthy = aqi >= 200
        val isAqiUnhealthy = aqi >= 150
        val isAqiSensitive = aqi >= 101

        return when {
            isHeavyRain && (isAqiUnhealthy || isAqiVeryUnhealthy || isAqiHazardous) -> {
                CombinedIntelligence(
                    headlineEn = "Heavy Downpour & Unhealthy Air Alert",
                    headlineBn = "ভারী বৃষ্টি ও অস্বাস্থ্যকর বায়ুর দ্বৈত সতর্কতা",
                    detailEn = "Heavy rainfall of ${rainMm}mm expected with elevated AQI ($aqi). Avoid waterlogged roads and wear protective masks outdoors.",
                    detailBn = "${rainMm} মিমি সম্ভাব্য ভারী বৃষ্টি এবং বায়ুর সূচক ($aqi) ক্ষতিকর পর্যায়ে। জলাবদ্ধ রাস্তা পরিহার করুন ও মাস্ক পরুন।",
                    severity = IntelligenceSeverity.ALERT,
                    rainRisk = true,
                    pollutionRisk = true
                )
            }
            isHeavyRain -> {
                CombinedIntelligence(
                    headlineEn = "Heavy Rain Early Warning",
                    headlineBn = "ভারী বৃষ্টিপাতের আগাম সতর্কতা",
                    detailEn = "Significant rain accumulation (${rainMm}mm) forecast in next 24 hours. Keep umbrellas ready and check drainage routes.",
                    detailBn = "আগামী ২৪ ঘণ্টায় ${rainMm} মিমি উল্লেখযোগ্য বৃষ্টিপাতের পূর্বাভাস। ছাতা প্রস্তুত রাখুন ও সাবধানে চলাচল করুন।",
                    severity = IntelligenceSeverity.WARNING,
                    rainRisk = true,
                    pollutionRisk = false
                )
            }
            isAqiHazardous || isAqiVeryUnhealthy -> {
                CombinedIntelligence(
                    headlineEn = "Severe Air Pollution Emergency",
                    headlineBn = "মারাত্মক বায়ু দূষণ জরুরি সতর্কতা",
                    detailEn = "Air Quality Index reached dangerous levels ($aqi) with high PM2.5 (${pm25} µg/m³). Stay indoors and seal windows.",
                    detailBn = "বায়ু দূষণ সূচক মারাত্মক পর্যায়ে ($aqi) এবং PM2.5 (${pm25} µg/m³)। ঘরের ভেতরে অবস্থান করুন ও জানালা বন্ধ রাখুন।",
                    severity = IntelligenceSeverity.ALERT,
                    rainRisk = false,
                    pollutionRisk = true
                )
            }
            isAqiUnhealthy -> {
                CombinedIntelligence(
                    headlineEn = "Unhealthy Air Quality Advisory",
                    headlineBn = "অস্বাস্থ্যকর বায়ুর মানের স্বাস্থ্য পরামর্শ",
                    detailEn = "AQI is $aqi. Sensitive individuals and general public should avoid strenuous outdoor exertion and wear masks.",
                    detailBn = "বায়ু দূষণ সূচক $aqi। বাইরে ভারী পরিশ্রম কমান এবং সংবেদনশীল ব্যক্তিরা অবশ্যই মাস্ক ব্যবহার করুন।",
                    severity = IntelligenceSeverity.ADVISORY,
                    rainRisk = false,
                    pollutionRisk = true
                )
            }
            isRainLikely && isAqiSensitive -> {
                CombinedIntelligence(
                    headlineEn = "Showers & Moderate Particulate Levels",
                    headlineBn = "বৃষ্টির সম্ভাবনা ও সংবেদনশীল বায়ুর মান",
                    detailEn = "Rain probability is $rainProb%. Passing showers may temporarily damp surface dust, but sensitive groups should take care.",
                    detailBn = "বৃষ্টির সম্ভাবনা $rainProb%। সাময়িক পশলা বৃষ্টি ধূলিকণা কমাতে পারে, তবে সংবেদনশীল ব্যক্তিরা সচেতন থাকুন।",
                    severity = IntelligenceSeverity.ADVISORY,
                    rainRisk = true,
                    pollutionRisk = true
                )
            }
            isRainLikely -> {
                CombinedIntelligence(
                    headlineEn = "Rain Expected Soon",
                    headlineBn = "বৃষ্টির জোর সম্ভাবনা",
                    detailEn = "Rain probability stands at $rainProb% with around ${rainMm}mm expected. Carry rain protection during your commute.",
                    detailBn = "বৃষ্টির সম্ভাবনা $rainProb% এবং প্রায় ${rainMm} মিমি বৃষ্টি হতে পারে। যাতায়াতের সময় ছাতা সাথে রাখুন।",
                    severity = IntelligenceSeverity.ADVISORY,
                    rainRisk = true,
                    pollutionRisk = false
                )
            }
            isAqiSensitive -> {
                CombinedIntelligence(
                    headlineEn = "Sensitive Groups Air Advisory",
                    headlineBn = "সংবেদনশীল ব্যক্তিদের জন্য বায়ু সতর্কতা",
                    detailEn = "AQI is $aqi. Children, seniors, and asthma patients should reduce prolonged outdoor playtime.",
                    detailBn = "বায়ু দূষণ সূচক $aqi। শিশু, বৃদ্ধ ও হাঁপানি রোগীদের ক্ষেত্রে বাইরে দীর্ঘক্ষণ থাকা কমানো উচিত।",
                    severity = IntelligenceSeverity.ADVISORY,
                    rainRisk = false,
                    pollutionRisk = true
                )
            }
            else -> {
                CombinedIntelligence(
                    headlineEn = "Favorable Weather & Air Conditions",
                    headlineBn = "অনুকূল আবহাওয়া ও স্বাস্থ্যকর পরিবেশ",
                    detailEn = "No severe rain forecast and air quality is currently satisfactory ($aqi AQI). Great conditions for outdoor activities.",
                    detailBn = "ভারী বৃষ্টির কোনো আশঙ্কা নেই এবং বায়ুর মান সহনীয় ($aqi AQI)। বাইরে সাধারণ চলাচলের জন্য উপযোগী।",
                    severity = IntelligenceSeverity.NORMAL,
                    rainRisk = false,
                    pollutionRisk = false
                )
            }
        }
    }
}
