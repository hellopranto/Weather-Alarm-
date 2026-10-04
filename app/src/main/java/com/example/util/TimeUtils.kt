package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TimeUtils {
    val BANGLADESH_TIMEZONE: TimeZone = TimeZone.getTimeZone("Asia/Dhaka")

    /**
     * Formats hourly forecast timestamp into Bangladesh Timezone (Asia/Dhaka, UTC+6).
     */
    fun formatBangladeshHour(timestampSec: Long, fallbackTimeString: String = ""): String {
        if (timestampSec > 0) {
            val date = Date(timestampSec * 1000)
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault()).apply {
                timeZone = BANGLADESH_TIMEZONE
            }
            return sdf.format(date)
        }
        return fallbackTimeString
    }

    /**
     * Formats sunrise/sunset to ensure it matches Bangladesh solar time (Asia/Dhaka).
     * If the backend returns a UTC-based string (e.g., 11:54 PM for sunrise or 11:45 AM for sunset),
     * this method adjusts it to the actual Bangladesh local time (+6h: 05:54 AM / 05:45 PM).
     */
    fun formatBangladeshSunTime(timeStr: String): String {
        if (timeStr.isBlank()) return "--:--"
        return try {
            val parser = SimpleDateFormat("hh:mm a", Locale.US)
            val date = parser.parse(timeStr.trim())
            if (date != null) {
                val cal = Calendar.getInstance()
                cal.time = date
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                // When generated on UTC servers, BD sunrise (05:xx AM) is 23:xx (11:xx PM)
                // and BD sunset (05:xx PM) is 11:xx (11:xx AM). Add 6 hours to convert to BD time.
                if (hour in 21..23 || hour in 10..13) {
                    cal.add(Calendar.HOUR_OF_DAY, 6)
                    parser.format(cal.time)
                } else {
                    timeStr
                }
            } else {
                timeStr
            }
        } catch (_: Exception) {
            timeStr
        }
    }

    /**
     * Formats last update ISO timestamp into local Bangladesh Time (Asia/Dhaka).
     */
    fun formatLastUpdateTime(isoOrDate: String): String {
        if (isoOrDate.isBlank()) return "Just now"
        return try {
            val date = if (isoOrDate.contains("T")) {
                val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                iso.parse(isoOrDate.substringBefore("."))
            } else null

            if (date != null) {
                val display = SimpleDateFormat("hh:mm a", Locale.getDefault()).apply {
                    timeZone = BANGLADESH_TIMEZONE
                }
                display.format(date)
            } else {
                isoOrDate.take(16).replace("T", " ")
            }
        } catch (_: Exception) {
            if (isoOrDate.contains(":")) isoOrDate.take(16).replace("T", " ") else "Just now"
        }
    }
}
