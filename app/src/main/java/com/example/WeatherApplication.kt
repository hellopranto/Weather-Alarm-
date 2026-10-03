package com.example

import android.app.Application
import com.example.worker.WeatherNotificationHelper
import com.example.worker.WeatherSyncManager

class WeatherApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        WeatherNotificationHelper.createNotificationChannel(this)
        WeatherSyncManager.schedulePeriodicSync(this)
    }
}
