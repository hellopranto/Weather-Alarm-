package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_weather_preferences")

enum class TemperatureUnit {
    CELSIUS, FAHRENHEIT
}

data class UserPreferences(
    val selectedCityName: String = "",
    val selectedLatitude: Double = 0.0,
    val selectedLongitude: Double = 0.0,
    val useGpsLocation: Boolean = true,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val notificationsEnabled: Boolean = true
)

class UserPreferencesRepository(private val context: Context) {
    private val prefCityName = stringPreferencesKey("selected_city_name")
    private val prefLatitude = doublePreferencesKey("selected_latitude")
    private val prefLongitude = doublePreferencesKey("selected_longitude")
    private val prefUseGps = booleanPreferencesKey("use_gps_location")
    private val prefTempUnit = stringPreferencesKey("temperature_unit")
    private val prefNotifications = booleanPreferencesKey("notifications_enabled")

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        val cityName = preferences[prefCityName] ?: ""
        val lat = preferences[prefLatitude] ?: 0.0
        val lon = preferences[prefLongitude] ?: 0.0
        val useGps = preferences[prefUseGps] ?: true
        val tempUnitStr = preferences[prefTempUnit] ?: TemperatureUnit.CELSIUS.name
        val tempUnit = try {
            TemperatureUnit.valueOf(tempUnitStr)
        } catch (_: Exception) {
            TemperatureUnit.CELSIUS
        }
        val notifications = preferences[prefNotifications] ?: true

        UserPreferences(
            selectedCityName = cityName,
            selectedLatitude = lat,
            selectedLongitude = lon,
            useGpsLocation = useGps,
            temperatureUnit = tempUnit,
            notificationsEnabled = notifications
        )
    }

    suspend fun setSelectedLocation(name: String, lat: Double, lon: Double, useGps: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[prefCityName] = name
            preferences[prefLatitude] = lat
            preferences[prefLongitude] = lon
            preferences[prefUseGps] = useGps
        }
    }

    suspend fun setTemperatureUnit(unit: TemperatureUnit) {
        context.dataStore.edit { preferences ->
            preferences[prefTempUnit] = unit.name
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[prefNotifications] = enabled
        }
    }
}
