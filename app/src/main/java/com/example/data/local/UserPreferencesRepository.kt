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

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_weather_settings")

enum class TemperatureUnit(val symbol: String) {
    CELSIUS("°C"),
    FAHRENHEIT("°F")
}

enum class WindUnit(val label: String) {
    KMH("km/h"),
    MS("m/s"),
    MPH("mph")
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class UserPreferences(
    val language: String = "bn", // Default to Bengali as per Bangladesh focus
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val windUnit: WindUnit = WindUnit.KMH,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useGpsLocation: Boolean = true,
    val selectedCityName: String = "Dhaka",
    val selectedLatitude: Double = 23.8103,
    val selectedLongitude: Double = 90.4125
)

class UserPreferencesRepository(private val context: Context) {
    private val prefLanguage = stringPreferencesKey("app_language")
    private val prefTempUnit = stringPreferencesKey("temp_unit")
    private val prefWindUnit = stringPreferencesKey("wind_unit")
    private val prefThemeMode = stringPreferencesKey("theme_mode")
    private val prefUseGps = booleanPreferencesKey("use_gps")
    private val prefCityName = stringPreferencesKey("selected_city_name")
    private val prefLatitude = doublePreferencesKey("selected_latitude")
    private val prefLongitude = doublePreferencesKey("selected_longitude")

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        val lang = preferences[prefLanguage] ?: "bn"
        val temp = when (preferences[prefTempUnit]) {
            "F" -> TemperatureUnit.FAHRENHEIT
            else -> TemperatureUnit.CELSIUS
        }
        val wind = when (preferences[prefWindUnit]) {
            "ms" -> WindUnit.MS
            "mph" -> WindUnit.MPH
            else -> WindUnit.KMH
        }
        val theme = when (preferences[prefThemeMode]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
        val useGps = preferences[prefUseGps] ?: true
        val city = preferences[prefCityName] ?: "Dhaka"
        val lat = preferences[prefLatitude] ?: 23.8103
        val lon = preferences[prefLongitude] ?: 90.4125

        UserPreferences(
            language = lang,
            temperatureUnit = temp,
            windUnit = wind,
            themeMode = theme,
            useGpsLocation = useGps,
            selectedCityName = city,
            selectedLatitude = lat,
            selectedLongitude = lon
        )
    }

    suspend fun setLanguage(languageCode: String) {
        context.dataStore.edit { it[prefLanguage] = languageCode }
    }

    suspend fun setTemperatureUnit(unit: TemperatureUnit) {
        context.dataStore.edit { it[prefTempUnit] = if (unit == TemperatureUnit.FAHRENHEIT) "F" else "C" }
    }

    suspend fun setWindUnit(unit: WindUnit) {
        context.dataStore.edit {
            it[prefWindUnit] = when (unit) {
                WindUnit.MS -> "ms"
                WindUnit.MPH -> "mph"
                WindUnit.KMH -> "kmh"
            }
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[prefThemeMode] = mode.name }
    }

    suspend fun setUseGpsLocation(useGps: Boolean) {
        context.dataStore.edit { it[prefUseGps] = useGps }
    }

    suspend fun setSelectedLocation(name: String, lat: Double, lon: Double) {
        context.dataStore.edit {
            it[prefCityName] = name
            it[prefLatitude] = lat
            it[prefLongitude] = lon
            it[prefUseGps] = false
        }
    }
}
