package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeUtils {
    fun formatEpochToHour(epochSeconds: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.US)
        return sdf.format(Date(epochSeconds * 1000))
    }

    fun formatEpochToDate(epochSeconds: Long): String {
        val sdf = SimpleDateFormat("dd MMM", Locale.US)
        return sdf.format(Date(epochSeconds * 1000))
    }
}
