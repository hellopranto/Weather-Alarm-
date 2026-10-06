package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WeatherDao {
    @Query("SELECT * FROM weather_cache WHERE locationKey = :key LIMIT 1")
    suspend fun getCacheByKey(key: String): WeatherCacheEntity?

    @Query("SELECT * FROM weather_cache ORDER BY timestampMillis DESC LIMIT 1")
    suspend fun getLatestCache(): WeatherCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(entity: WeatherCacheEntity)

    @Query("DELETE FROM weather_cache WHERE timestampMillis < :olderThanMillis")
    suspend fun deleteOldCache(olderThanMillis: Long)
}
