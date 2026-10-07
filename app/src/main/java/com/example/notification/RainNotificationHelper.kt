package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.RainPredictionResponse
import com.example.util.BanglaUtils

object RainNotificationHelper {

    private const val CHANNEL_ID = "rain_alerts_channel"
    private const val CHANNEL_NAME = "বৃষ্টির সতর্কতা ও পূর্বাভাস"
    private const val NOTIFICATION_ID = 2001
    private const val PREFS_NAME = "rain_notification_prefs"
    private const val KEY_LAST_ALERT_TIME = "last_rain_alert_time"
    private const val KEY_LAST_ALERT_TYPE = "last_rain_alert_type"
    private const val COOLDOWN_MILLIS = 45 * 60 * 1000L // 45 minutes

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "আসন্ন বৃষ্টিপাত এবং ভারী বর্ষণের তাৎক্ষণিক সতর্কতা"
                enableVibration(true)
                setShowBadge(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun checkAndNotify(context: Context, prediction: RainPredictionResponse, notificationsEnabled: Boolean) {
        if (!notificationsEnabled) return

        createNotificationChannel(context)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastAlertTime = prefs.getLong(KEY_LAST_ALERT_TIME, 0L)
        val lastAlertType = prefs.getString(KEY_LAST_ALERT_TYPE, "") ?: ""
        val now = System.currentTimeMillis()

        val heavyWarning = prediction.heavyRainWarning
        val isHeavy = heavyWarning != null && heavyWarning.isWarningActive
        val isApproaching = prediction.radar?.approaching == true
        val p30 = prediction.prediction?.next30Minutes ?: 0
        val p15 = prediction.prediction?.next15Minutes ?: 0
        val startMins = prediction.rain?.startInMinutes

        // Trigger condition
        val shouldAlert = isHeavy || (prediction.rain?.expected == true && (p15 >= 60 || p30 >= 60 || isApproaching))

        if (!shouldAlert) return

        val currentAlertType = if (isHeavy) "HEAVY_RAIN" else "RAIN_APPROACHING"

        // Cooldown check: if same type, require 45m cooldown. If escalating to heavy rain, allow immediately.
        if (currentAlertType == lastAlertType && (now - lastAlertTime) < COOLDOWN_MILLIS) {
            return
        }

        val locationName = prediction.location.name.ifBlank { "আপনার এলাকা" }

        val (title, message) = if (isHeavy) {
            val titleText = "⚠️ $locationName — ভারী বৃষ্টির সতর্কতা"
            val bodyText = "আগামী ${heavyWarning?.expectedStart ?: "কিছুক্ষণের মধ্যে"} ভারী বৃষ্টিপাত হতে পারে। সম্ভাব্য পরিমাণ: ${heavyWarning?.expectedRainfallAmount ?: "অধিক"}। সাবধানে থাকুন।"
            Pair(titleText, bodyText)
        } else {
            val etaStr = if (startMins != null && startMins > 0) {
                "প্রায় ${BanglaUtils.toBanglaDigits(startMins)} মিনিটের মধ্যে"
            } else {
                "কিছুক্ষণের মধ্যে"
            }
            val titleText = "🌧️ $locationName — বৃষ্টির সতর্কতা"
            val bodyText = "আপনার এলাকায় $etaStr বৃষ্টি শুরু হতে পারে (সম্ভাবনা: ${BanglaUtils.toBanglaDigits(maxOf(p15, p30))}% )। সাথে ছাতা রাখুন।"
            Pair(titleText, bodyText)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)

        prefs.edit()
            .putLong(KEY_LAST_ALERT_TIME, now)
            .putString(KEY_LAST_ALERT_TYPE, currentAlertType)
            .apply()
    }
}
