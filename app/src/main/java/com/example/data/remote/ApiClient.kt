package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    const val DEFAULT_BACKEND_URL = "https://backend-blond-five-79.vercel.app/"

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private fun getBackendBaseUrl(): String {
        return try {
            val url = BuildConfig.BACKEND_BASE_URL
            if (url.isNotBlank() && url.startsWith("http")) {
                if (url.endsWith("/")) url else "$url/"
            } else {
                DEFAULT_BACKEND_URL
            }
        } catch (_: Exception) {
            DEFAULT_BACKEND_URL
        }
    }

    val weatherApi: WeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl(getBackendBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(WeatherApi::class.java)
    }

    val bmdApi: BmdApi by lazy {
        Retrofit.Builder()
            .baseUrl(getBackendBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(BmdApi::class.java)
    }

    val radarApi: RadarApi by lazy {
        Retrofit.Builder()
            .baseUrl(getBackendBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(RadarApi::class.java)
    }
}
