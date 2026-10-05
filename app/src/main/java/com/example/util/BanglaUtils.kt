package com.example.util

import com.example.data.local.TemperatureUnit
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object BanglaUtils {

    private val BANGLA_DIGITS = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun toBanglaDigits(input: Any?): String {
        if (input == null) return ""
        val str = input.toString()
        val sb = StringBuilder(str.length)
        for (ch in str) {
            if (ch in '0'..'9') {
                sb.append(BANGLA_DIGITS[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatTemp(celsius: Double?, unit: TemperatureUnit = TemperatureUnit.CELSIUS): String {
        if (celsius == null) return "--"
        val value = when (unit) {
            TemperatureUnit.CELSIUS -> Math.round(celsius).toInt()
            TemperatureUnit.FAHRENHEIT -> Math.round(celsius * 9 / 5 + 32).toInt()
        }
        val symbol = if (unit == TemperatureUnit.CELSIUS) "°C" else "°F"
        return "${toBanglaDigits(value)}$symbol"
    }

    fun formatFeelsLike(celsius: Double?, unit: TemperatureUnit = TemperatureUnit.CELSIUS): String {
        if (celsius == null) return ""
        return "অনুভূত তাপমাত্রা ${formatTemp(celsius, unit)}"
    }

    fun formatWind(speedKmh: Double?): String {
        if (speedKmh == null) return "--"
        val formatted = if (speedKmh % 1.0 == 0.0) {
            speedKmh.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", speedKmh)
        }
        return "${toBanglaDigits(formatted)} কিমি/ঘণ্টা"
    }

    fun formatPressure(pressureHpa: Int?): String {
        if (pressureHpa == null) return "--"
        return "${toBanglaDigits(pressureHpa)} hPa"
    }

    fun formatHumidity(humidity: Int?): String {
        if (humidity == null) return "--"
        return "${toBanglaDigits(humidity)}%"
    }

    fun formatRainfall(rainfallMm: Double?): String {
        if (rainfallMm == null) return "--"
        val formatted = if (rainfallMm % 1.0 == 0.0) {
            rainfallMm.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", rainfallMm)
        }
        return "${toBanglaDigits(formatted)} মিমি"
    }

    fun formatVisibility(visibilityMeters: Int?): String {
        if (visibilityMeters == null) return "--"
        val km = visibilityMeters / 1000
        return "${toBanglaDigits(km)} কিমি"
    }

    fun formatUvIndex(uv: Double?): String {
        if (uv == null) return "--"
        val formatted = if (uv % 1.0 == 0.0) uv.toInt().toString() else String.format(Locale.US, "%.1f", uv)
        return toBanglaDigits(formatted)
    }

    fun mapCondition(condition: String?, weatherCode: Int = 800): String {
        if (condition.isNullOrBlank()) return "পরিষ্কার আকাশ"
        val norm = condition.lowercase(Locale.ROOT)
        return when {
            norm.contains("thunder") || weatherCode in 200..299 -> "বজ্রসহ বৃষ্টি"
            norm.contains("heavy rain") || weatherCode in listOf(502, 503, 504, 522) -> "ভারী বৃষ্টি"
            norm.contains("drizzle") || norm.contains("light rain") || weatherCode in listOf(300, 301, 500) -> "হালকা বৃষ্টি"
            norm.contains("rain") || weatherCode in 500..599 -> "বৃষ্টি"
            norm.contains("fog") || norm.contains("mist") || weatherCode in listOf(701, 741) -> "কুয়াশা"
            norm.contains("haze") || weatherCode == 721 -> "কুয়াশাচ্ছন্ন"
            norm.contains("dust") || norm.contains("sand") -> "ধুলোময়"
            norm.contains("overcast") || weatherCode == 804 -> "মেঘাচ্ছন্ন"
            norm.contains("mostly cloudy") || norm.contains("mainly cloudy") -> "প্রধানত মেঘলা"
            norm.contains("partly cloudy") || weatherCode in listOf(801, 802) -> "আংশিক মেঘলা"
            norm.contains("cloud") || weatherCode == 803 -> "মেঘলা আকাশ"
            norm.contains("clear") || weatherCode == 800 -> "পরিষ্কার আকাশ"
            else -> condition
        }
    }

    fun getTodayBengaliDateString(): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"))
        val dayIndex = cal.get(Calendar.DAY_OF_WEEK)
        val dayNames = arrayOf("", "রবিবার", "সোমবার", "মঙ্গলবার", "বুধবার", "বৃহস্পতিবার", "শুক্রবার", "শনিবার")
        val dayName = if (dayIndex in 1..7) dayNames[dayIndex] else ""

        val dayOfMonth = toBanglaDigits(String.format(Locale.US, "%02d", cal.get(Calendar.DAY_OF_MONTH)))
        val months = arrayOf(
            "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
            "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
        )
        val monthName = months[cal.get(Calendar.MONTH)]
        val year = toBanglaDigits(cal.get(Calendar.YEAR))

        return "$dayName, $dayOfMonth $monthName, $year"
    }

    fun formatHourBengali(timeStr: String): String {
        // e.g. "03:00 PM" -> "০৩:০০ অপরাহ্ন" or "০৩:০০"
        return toBanglaDigits(timeStr)
            .replace("AM", "পূর্বাহ্ন")
            .replace("PM", "অপরাহ্ন")
            .replace("am", "পূর্বাহ্ন")
            .replace("pm", "অপরাহ্ন")
    }
}
