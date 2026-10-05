package com.example

import com.example.data.local.TemperatureUnit
import com.example.data.model.CurrentWeatherModel
import com.example.data.model.LocationModel
import com.example.data.model.UnifiedWeatherResponse
import com.example.data.remote.ApiClient
import com.example.location.BangladeshLocations
import com.example.ui.home.formatTemp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun bangladeshLocations_searchDhaka() {
        val results = BangladeshLocations.search("Dhaka")
        assertTrue(results.isNotEmpty())
        assertEquals("Dhaka", results[0].nameEn)
        assertEquals("ঢাকা", results[0].nameBn)
    }

    @Test
    fun bangladeshLocations_searchChattogram() {
        val results = BangladeshLocations.search("Chattogram")
        assertTrue(results.isNotEmpty())
        assertEquals("Chattogram", results[0].nameEn)
    }

    @Test
    fun temperatureFormatting_celsiusAndFahrenheit() {
        val tempC = 30.0
        assertEquals("30°C", formatTemp(tempC, TemperatureUnit.CELSIUS))
        assertEquals("86°F", formatTemp(tempC, TemperatureUnit.FAHRENHEIT))
    }

    @Test
    fun moshiSerialization_unifiedWeatherResponse() {
        val adapter = ApiClient.moshi.adapter(UnifiedWeatherResponse::class.java)
        val dummy = UnifiedWeatherResponse(
            location = LocationModel("Dhaka", "Dhaka", "Bangladesh", 23.8103, 90.4125),
            current = CurrentWeatherModel(
                temperature = 31.0,
                feelsLike = 35.0,
                humidity = 75,
                windSpeed = 12.0
            )
        )
        val json = adapter.toJson(dummy)
        assertNotNull(json)
        val parsed = adapter.fromJson(json)
        assertNotNull(parsed)
        assertEquals("Dhaka", parsed?.location?.name)
        assertEquals(31.0, parsed?.current?.temperature ?: 0.0, 0.01)
    }

    @Test
    fun banglaUtils_convertsDigitsAndFormatsMetrics() {
        assertEquals("৩০°C", com.example.util.BanglaUtils.formatTemp(30.0))
        assertEquals("৬৩%", com.example.util.BanglaUtils.formatHumidity(63))
        assertEquals("১০১০ hPa", com.example.util.BanglaUtils.formatPressure(1010))
        assertEquals("৩.১ কিমি/ঘণ্টা", com.example.util.BanglaUtils.formatWind(3.1))
        assertEquals("০ মিমি", com.example.util.BanglaUtils.formatRainfall(0.0))
        assertEquals("৬ কিমি", com.example.util.BanglaUtils.formatVisibility(6000))
        assertEquals("২", com.example.util.BanglaUtils.formatUvIndex(2.0))
        assertEquals("প্রধানত মেঘলা", com.example.util.BanglaUtils.mapCondition("Mostly Cloudy", 802))
    }
}

