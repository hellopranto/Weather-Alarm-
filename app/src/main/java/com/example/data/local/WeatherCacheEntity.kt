package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey
    val locationKey: String, // e.g. "lat_lon" or "Dhaka"
    val cityName: String,
    val latitude: Double,
    val longitude: Double,
    val jsonPayload: String,
    val timestamp: Long = System.currentTimeMillis()
)
