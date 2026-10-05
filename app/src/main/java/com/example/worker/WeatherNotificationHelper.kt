package com.example.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.UnifiedWeatherResponse

object WeatherNotificationHelper {
    const val CHANNEL_ID = "weather_alerts_channel"
    private const val NOTIFICATION_ID = 2001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.alerts_title)
            val descriptionText = "Severe weather and background sync notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showWeatherSyncNotification(context: Context, weather: UnifiedWeatherResponse) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val tempStr = weather.current.temperature?.let { "${it.toInt()}°C" } ?: "--°C"
        val hasAlerts = weather.alerts.isNotEmpty() || weather.warnings.isNotEmpty()
        val topAlert = if (weather.alerts.isNotEmpty()) weather.alerts[0] else weather.warnings.firstOrNull()

        val title = if (hasAlerts && topAlert != null) {
            "⚠️ ${topAlert.titleBn ?: topAlert.title}"
        } else {
            "🌤️ ${weather.location.displayName ?: weather.location.name}: $tempStr"
        }

        val text = if (hasAlerts && topAlert != null) {
            topAlert.descriptionBn ?: topAlert.description
        } else {
            val cond = weather.current.conditionBn ?: weather.current.condition ?: "আবহাওয়া আপডেট"
            val hum = weather.current.humidity?.let { "$it%" } ?: "--%"
            val wind = weather.current.windSpeed?.let { "$it km/h" } ?: "-- km/h"
            "$cond • আর্দ্রতা: $hum • বাতাস: $wind"
        }


        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(if (hasAlerts) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}
