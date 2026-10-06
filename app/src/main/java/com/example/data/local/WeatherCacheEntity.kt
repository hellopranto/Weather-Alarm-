package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey
    val locationKey: String, // e.g. "lat_lon"
    val latitude: Double,
    val longitude: Double,
    val cityName: String,
    val responseJson: String,
    val timestampMillis: Long = System.currentTimeMillis()
)
