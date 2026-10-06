package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.remote.ApiClient
import com.example.data.repository.BmdWeatherRepositoryImpl
import com.example.data.repository.RadarRepositoryImpl
import com.example.data.repository.WeatherRepositoryImpl
import com.example.domain.repository.BmdWeatherRepository
import com.example.domain.repository.RadarRepository
import com.example.domain.repository.WeatherRepository
import com.example.location.DefaultLocationTracker
import com.example.location.LocationTracker
import com.google.android.gms.location.LocationServices

class WeatherApplication : Application() {

    lateinit var weatherRepository: WeatherRepository
        private set

    lateinit var bmdRepository: BmdWeatherRepository
        private set

    lateinit var radarRepository: RadarRepository
        private set

    lateinit var userPreferencesRepository: UserPreferencesRepository
        private set

    lateinit var locationTracker: LocationTracker
        private set

    override fun onCreate() {
        super.onCreate()

        val database = AppDatabase.getInstance(this)
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        userPreferencesRepository = UserPreferencesRepository(this)
        locationTracker = DefaultLocationTracker(fusedLocationClient, this)

        weatherRepository = WeatherRepositoryImpl(
            weatherApi = ApiClient.weatherApi,
            weatherDao = database.weatherDao(),
            moshi = ApiClient.moshi
        )

        bmdRepository = BmdWeatherRepositoryImpl(
            bmdApi = ApiClient.bmdApi
        )

        radarRepository = RadarRepositoryImpl(
            radarApi = ApiClient.radarApi
        )
    }
}
