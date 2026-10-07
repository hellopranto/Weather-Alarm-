package com.example.util

import com.example.data.local.TemperatureUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BanglaUtils {
    private val englishDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    private val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun toBanglaDigits(number: Any?): String {
        if (number == null) return "--"
        val str = when (number) {
            is Double -> if (number % 1.0 == 0.0) number.toInt().toString() else String.format(Locale.US, "%.1f", number)
            is Float -> if (number % 1.0f == 0.0f) number.toInt().toString() else String.format(Locale.US, "%.1f", number)
            else -> number.toString()
        }
        val sb = java.lang.StringBuilder()
        for (ch in str) {
            val idx = englishDigits.indexOf(ch)
            if (idx != -1) {
                sb.append(banglaDigits[idx])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatTemp(celsius: Double?, unit: TemperatureUnit): String {
        if (celsius == null) return "--°"
        val value = when (unit) {
            TemperatureUnit.CELSIUS -> celsius
            TemperatureUnit.FAHRENHEIT -> (celsius * 9 / 5) + 32
        }
        val unitSymbol = when (unit) {
            TemperatureUnit.CELSIUS -> "°C"
            TemperatureUnit.FAHRENHEIT -> "°F"
        }
        return "${toBanglaDigits(kotlin.math.round(value).toInt())}$unitSymbol"
    }

    fun formatFeelsLike(celsius: Double?, unit: TemperatureUnit): String {
        if (celsius == null) return ""
        val value = when (unit) {
            TemperatureUnit.CELSIUS -> celsius
            TemperatureUnit.FAHRENHEIT -> (celsius * 9 / 5) + 32
        }
        val unitSymbol = when (unit) {
            TemperatureUnit.CELSIUS -> "°C"
            TemperatureUnit.FAHRENHEIT -> "°F"
        }
        return "অনুভূত হচ্ছে ${toBanglaDigits(kotlin.math.round(value).toInt())}$unitSymbol"
    }

    fun formatWind(kmh: Double?): String {
        if (kmh == null) return "--"
        return "${toBanglaDigits(kmh)} কিমি/ঘ"
    }

    fun formatPressure(hpa: Int?): String {
        if (hpa == null) return "--"
        return "${toBanglaDigits(hpa)} hPa"
    }

    fun formatRainfall(mm: Double?): String {
        if (mm == null) return "--"
        return "${toBanglaDigits(mm)} মিমি"
    }

    fun formatHumidity(percent: Int?): String {
        if (percent == null) return "--"
        return "${toBanglaDigits(percent)}%"
    }

    fun formatVisibility(meters: Int?): String {
        if (meters == null) return "--"
        val km = meters / 1000.0
        return "${toBanglaDigits(km)} কিমি"
    }

    fun formatUvIndex(uv: Double?): String {
        if (uv == null) return "--"
        return toBanglaDigits(uv)
    }

    fun formatHourBengali(timeString: String): String {
        return timeString.replace("AM", "পূর্বাহ্ন")
            .replace("PM", "অপরাহ্ন")
            .replace("am", "পূর্বাহ্ন")
            .replace("pm", "অপরাহ্ন")
            .let { toBanglaDigits(it) }
    }

    fun getTodayBengaliDateString(): String {
        val bengaliMonths = listOf(
            "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
            "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
        )
        val bengaliDays = listOf(
            "রবিবার", "সোমবার", "মঙ্গলবার", "বুধবার", "বৃহস্পতিবার", "শুক্রবার", "শনিবার"
        )
        val cal = java.util.Calendar.getInstance()
        val dayOfWeek = bengaliDays[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]
        val dayOfMonth = toBanglaDigits(cal.get(java.util.Calendar.DAY_OF_MONTH))
        val month = bengaliMonths[cal.get(java.util.Calendar.MONTH)]
        return "$dayOfWeek, $dayOfMonth $month"
    }

    fun mapCondition(condition: String?, code: Int): String {
        return when (code) {
            0, 800 -> "পরিষ্কার আকাশ"
            1, 801 -> "প্রধানত পরিষ্কার"
            2, 802 -> "আংশিক মেঘলা"
            3, 803, 804 -> "মেঘলা আকাশ"
            45, 48, 701, 741 -> "কুয়াশাচ্ছন্ন"
            51, 53, 55, 300 -> "হালকা গুঁড়ি গুঁড়ি বৃষ্টি"
            61, 63, 65, 500, 501, 502 -> "বৃষ্টিপাত"
            80, 81, 82 -> "বজ্রবৃষ্টিসহ ঝোড়ো হাওয়া"
            95, 96, 99, 200, 211 -> "তীব্র বজ্রঝড়"
            else -> condition ?: "স্বাভাবিক আবহাওয়া"
        }
    }

    fun formatBengaliDateTime(isoString: String): String {
        if (isoString.isBlank()) return ""
        return try {
            val cleanStr = isoString.substringBefore("Z").substringBefore("+")
            val parser = if (cleanStr.contains(".")) {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)
            } else if (cleanStr.contains("T")) {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            } else {
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            }
            val date = parser.parse(cleanStr)
            if (date != null) {
                val output = SimpleDateFormat("hh:mm a, dd MMMM", Locale.US)
                val formatted = output.format(date)
                formatHourBengali(formatted)
            } else {
                toBanglaDigits(isoString)
            }
        } catch (_: Exception) {
            toBanglaDigits(isoString)
        }
    }
}
