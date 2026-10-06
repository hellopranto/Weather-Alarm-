package com.example.location

import android.content.Context
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

interface LocationService {
    suspend fun searchLocations(query: String): List<LocationItem>
    suspend fun reverseGeocode(lat: Double, lon: Double): String?
}

class LocationServiceImpl(private val context: Context) : LocationService {

    companion object {
        val DEFAULT_SUGGESTIONS = listOf(
            LocationItem("dhaka", "ঢাকা সদর", "Dhaka Sadar", "ঢাকা বিভাগ", "জেলা", 23.8103, 90.4125),
            LocationItem("ctg", "চট্টগ্রাম সদর", "Chattogram Sadar", "চট্টগ্রাম বিভাগ", "জেলা", 22.3569, 91.7832),
            LocationItem("sylhet", "সিলেট সদর", "Sylhet Sadar", "সিলেট বিভাগ", "জেলা", 24.8949, 91.8687),
            LocationItem("rajshahi", "রাজশাহী সদর", "Rajshahi Sadar", "রাজশাহী বিভাগ", "জেলা", 24.3745, 88.6042),
            LocationItem("khulna", "খুলনা সদর", "Khulna Sadar", "খুলনা বিভাগ", "জেলা", 22.8456, 89.5403),
            LocationItem("barishal", "বরিশাল সদর", "Barishal Sadar", "বরিশাল বিভাগ", "জেলা", 22.7010, 90.3535),
            LocationItem("rangpur", "রংপুর সদর", "Rangpur Sadar", "রংপুর বিভাগ", "জেলা", 25.7439, 89.2752),
            LocationItem("mymensingh", "ময়মনসিংহ সদর", "Mymensingh Sadar", "ময়মনসিংহ বিভাগ", "জেলা", 24.7471, 90.4203),
            LocationItem("cox", "কক্সবাজার সদর", "Cox's Bazar", "চট্টগ্রাম বিভাগ", "উপজেলা", 21.4272, 92.0058),
            LocationItem("kurigram", "কুড়িগ্রাম সদর", "Kurigram Sadar", "রংপুর বিভাগ", "উপজেলা", 25.8054, 89.6362),
            LocationItem("nageshwari", "নাগেশ্বরী উপজেলা", "Nageshwari", "কুড়িগ্রাম জেলা", "উপজেলা", 25.9622, 89.6894),
            LocationItem("bogra", "বগুড়া সদর", "Bogura", "রাজশাহী বিভাগ", "উপজেলা", 24.8465, 89.3778)
        )
    }

    override suspend fun searchLocations(query: String): List<LocationItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext DEFAULT_SUGGESTIONS

        val cleanQuery = query.trim()
        val matchedStatic = DEFAULT_SUGGESTIONS.filter {
            it.nameBn.contains(cleanQuery, ignoreCase = true) ||
            it.nameEn.contains(cleanQuery, ignoreCase = true) ||
            it.parentBn.contains(cleanQuery, ignoreCase = true)
        }

        // Try geocoder if available
        val geocoded = try {
            val geocoder = Geocoder(context, Locale("bn", "BD"))
            val addresses = geocoder.getFromLocationName("$cleanQuery, Bangladesh", 5)
            addresses?.mapIndexed { index, addr ->
                val name = addr.subLocality ?: addr.locality ?: addr.subAdminArea ?: cleanQuery
                val parent = addr.adminArea ?: "বাংলাদেশ"
                LocationItem(
                    id = "geo_${addr.latitude}_${addr.longitude}_$index",
                    nameBn = name,
                    nameEn = cleanQuery,
                    parentBn = parent,
                    typeBn = "স্থান",
                    latitude = addr.latitude,
                    longitude = addr.longitude
                )
            }.orEmpty()
        } catch (_: Exception) {
            emptyList()
        }

        val combined = (matchedStatic + geocoded).distinctBy { "${it.latitude}_${it.longitude}" }
        combined.ifEmpty { matchedStatic }
    }

    override suspend fun reverseGeocode(lat: Double, lon: Double): String? = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale("bn", "BD"))
            val list = geocoder.getFromLocation(lat, lon, 1)
            val first = list?.firstOrNull() ?: return@withContext null
            val upazila = first.subLocality ?: first.subAdminArea
            val district = first.locality ?: first.adminArea
            when {
                upazila != null && district != null -> "$upazila, $district"
                district != null -> district
                else -> first.featureName
            }
        } catch (_: Exception) {
            null
        }
    }
}
