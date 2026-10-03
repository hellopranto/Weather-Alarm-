package com.example.location

import android.location.Location

interface LocationTracker {
    suspend fun getCurrentLocation(): Location?
    fun hasLocationPermission(): Boolean
}
