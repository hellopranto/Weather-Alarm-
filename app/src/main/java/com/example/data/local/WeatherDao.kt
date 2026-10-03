package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {
    @Query("SELECT * FROM weather_cache WHERE locationKey = :key LIMIT 1")
    fun getWeatherByKeyFlow(key: String): Flow<WeatherCacheEntity?>

    @Query("SELECT * FROM weather_cache WHERE locationKey = :key LIMIT 1")
    suspend fun getWeatherByKey(key: String): WeatherCacheEntity?

    @Query("SELECT * FROM weather_cache ORDER BY timestamp DESC LIMIT 1")
    fun getLatestWeatherFlow(): Flow<WeatherCacheEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather(cache: WeatherCacheEntity)

    @Query("DELETE FROM weather_cache WHERE timestamp < :expirationTimestamp")
    suspend fun purgeExpired(expirationTimestamp: Long)
}
