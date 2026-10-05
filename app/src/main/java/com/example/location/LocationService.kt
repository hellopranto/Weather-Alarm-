package com.example.location

import android.content.Context
import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

interface LocationService {
    fun hasLocationPermission(): Boolean
    suspend fun getCurrentLocation(): Location?
    suspend fun searchLocations(query: String): List<LocationItem>
    suspend fun reverseGeocode(lat: Double, lon: Double): String?
}

class LocationServiceImpl(
    private val context: Context,
    private val locationTracker: LocationTracker = DefaultLocationTracker(context)
) : LocationService {

    override fun hasLocationPermission(): Boolean {
        return locationTracker.hasLocationPermission()
    }

    override suspend fun getCurrentLocation(): Location? {
        return locationTracker.getCurrentLocation()
    }

    override suspend fun reverseGeocode(lat: Double, lon: Double): String? = withContext(Dispatchers.IO) {
        try {
            val urlString = "https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lon&format=json&accept-language=bn,en"
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "WeatherAlertBangladesh/2.0")
            conn.connectTimeout = 3500
            conn.readTimeout = 3500
            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = org.json.JSONObject(responseText)
                if (obj.has("display_name")) {
                    return@withContext obj.getString("display_name")
                }
            }
        } catch (_: Exception) {}
        return@withContext null
    }

    override suspend fun searchLocations(query: String): List<LocationItem> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty()) {
            return@withContext DEFAULT_SUGGESTIONS
        }

        // 1. Instant local index search
        val localMatches = BANGLADESH_ADMIN_DATA.filter { item ->
            item.nameBn.contains(q, ignoreCase = true) ||
            item.nameEn.contains(q, ignoreCase = true) ||
            item.hierarchyBn.contains(q, ignoreCase = true)
        }

        // If local matches are rich (>= 5), return them immediately
        if (localMatches.size >= 5) {
            return@withContext localMatches.take(15)
        }

        // 2. Query OpenStreetMap Nominatim for any specific union, village, or area in Bangladesh
        val dynamicResults = mutableListOf<LocationItem>()
        try {
            val encodedQuery = URLEncoder.encode(q, "UTF-8")
            val urlString = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&countrycodes=bd&format=json&accept-language=bn,en&limit=8"
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "WeatherAlertBangladesh/2.0")
            conn.connectTimeout = 3500
            conn.readTimeout = 3500

            if (conn.responseCode == 200) {
                val jsonString = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(jsonString)

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val rawName = obj.optString("name", "")
                    val displayName = obj.optString("display_name", "")
                    val lat = obj.optDouble("lat", 0.0)
                    val lon = obj.optDouble("lon", 0.0)
                    val type = obj.optString("type", "")

                    val typeLabel = when {
                        type.contains("city") || type.contains("town") -> "শহর"
                        type.contains("district") || type.contains("county") -> "উপজেলা/জেলা"
                        type.contains("village") -> "গ্রাম/এলাকা"
                        type.contains("suburb") || type.contains("neighbourhood") -> "এলাকা"
                        else -> "স্থান"
                    }

                    val firstPart = displayName.substringBefore(",").trim()
                    val title = if (rawName.isNotBlank()) rawName else firstPart

                    if (lat != 0.0 && lon != 0.0) {
                        dynamicResults.add(
                            LocationItem(
                                id = "osm_${obj.optString("osm_id", i.toString())}",
                                nameBn = title,
                                nameEn = title,
                                hierarchyBn = displayName,
                                typeLabel = typeLabel,
                                latitude = lat,
                                longitude = lon
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        // Combine local and dynamic without duplicates
        val combined = (localMatches + dynamicResults).distinctBy { "${it.latitude}_${it.longitude}" }
        return@withContext if (combined.isNotEmpty()) combined else localMatches
    }

    companion object {
        val DEFAULT_SUGGESTIONS = listOf(
            LocationItem("nag_1", "নাগেশ্বরী শহর", "Nageshwari Town", "নাগেশ্বরী, কুড়িগ্রাম, রংপুর", "শহর", 25.9667, 89.6833),
            LocationItem("kuri_sadar", "কুড়িগ্রাম সদর", "Kurigram Sadar", "কুড়িগ্রাম, রংপুর", "উপজেলা", 25.8050, 89.6360),
            LocationItem("dhaka_sadar", "ঢাকা সদর", "Dhaka Sadar", "ঢাকা বিভাগ", "জেলা", 23.8103, 90.4125),
            LocationItem("ctg_patenga", "চট্টগ্রাম (পতেঙ্গা)", "Chattogram", "চট্টগ্রাম বিভাগ", "জেলা", 22.3569, 91.7832),
            LocationItem("cox_bazar", "কক্সবাজার শহর", "Cox's Bazar", "কক্সবাজার, চট্টগ্রাম", "শহর", 21.4272, 92.0058),
            LocationItem("sylhet_sadar", "সিলেট সদর", "Sylhet Sadar", "সিলেট বিভাগ", "জেলা", 24.8949, 91.8687),
            LocationItem("srimangal", "শ্রীমঙ্গল", "Sreemangal", "মৌলভীবাজার, সিলেট", "উপজেলা", 24.3065, 91.7296),
            LocationItem("rangpur_sadar", "রংপুর সদর", "Rangpur Sadar", "রংপুর বিভাগ", "জেলা", 25.7439, 89.2752),
            LocationItem("rajshahi_sadar", "রাজশাহী সদর", "Rajshahi Sadar", "রাজশাহী বিভাগ", "জেলা", 24.3745, 88.6042),
            LocationItem("khulna_sadar", "খুলনা সদর", "Khulna Sadar", "খুলনা বিভাগ", "জেলা", 22.8456, 89.5403),
            LocationItem("barishal_sadar", "বরিশাল সদর", "Barishal Sadar", "বরিশাল বিভাগ", "জেলা", 22.7010, 90.3535)
        )

        val BANGLADESH_ADMIN_DATA = listOf(
            // Kurigram Upazilas & Towns
            LocationItem("loc_nag", "নাগেশ্বরী শহর", "Nageshwari Town", "নাগেশ্বরী, কুড়িগ্রাম, রংপুর", "শহর", 25.9667, 89.6833),
            LocationItem("loc_nag_up", "নাগেশ্বরী উপজেলা", "Nageshwari Upazila", "কুড়িগ্রাম, রংপুর", "উপজেলা", 25.9750, 89.7000),
            LocationItem("loc_kurigram", "কুড়িগ্রাম সদর", "Kurigram Sadar", "কুড়িগ্রাম, রংপুর", "উপজেলা", 25.8050, 89.6360),
            LocationItem("loc_bhurungamari", "ভুরুঙ্গামারী", "Bhurungamari", "কুড়িগ্রাম, রংপুর", "উপজেলা", 26.1333, 89.6833),
            LocationItem("loc_ulipur", "উলিপুর", "Ulipur", "কুড়িগ্রাম, রংপুর", "উপজেলা", 25.6667, 89.6333),
            LocationItem("loc_chilmari", "চিলমারী", "Chilmari", "কুড়িগ্রাম, রংপুর", "উপজেলা", 25.5500, 89.6833),
            LocationItem("loc_rajarhat", "রাজারহাট", "Rajarhat", "কুড়িগ্রাম, রংপুর", "উপজেলা", 25.8000, 89.5500),
            LocationItem("loc_rowmari", "রৌমারী", "Rowmari", "কুড়িগ্রাম, রংপুর", "উপজেলা", 25.5667, 89.8500),
            LocationItem("loc_char_rajibpur", "চর রাজীবপুর", "Char Rajibpur", "কুড়িগ্রাম, রংপুর", "উপজেলা", 25.4000, 89.7833),
            LocationItem("loc_phulbari", "ফুলবাড়ী", "Phulbari", "কুড়িগ্রাম, রংপুর", "উপজেলা", 25.9500, 89.5667),

            // 64 Districts
            LocationItem("dist_dhaka", "ঢাকা", "Dhaka", "ঢাকা বিভাগ", "জেলা", 23.8103, 90.4125),
            LocationItem("dist_ctg", "চট্টগ্রাম", "Chattogram", "চট্টগ্রাম বিভাগ", "জেলা", 22.3569, 91.7832),
            LocationItem("dist_cox", "কক্সবাজার", "Cox's Bazar", "চট্টগ্রাম বিভাগ", "জেলা", 21.4272, 92.0058),
            LocationItem("dist_cumilla", "কুমিল্লা", "Cumilla", "চট্টগ্রাম বিভাগ", "জেলা", 23.4607, 91.1809),
            LocationItem("dist_feni", "ফেনী", "Feni", "চট্টগ্রাম বিভাগ", "জেলা", 23.0159, 91.3976),
            LocationItem("dist_noakhali", "নোয়াখালী", "Noakhali", "চট্টগ্রাম বিভাগ", "জেলা", 22.8696, 91.0994),
            LocationItem("dist_chandpur", "চাঁদপুর", "Chandpur", "চট্টগ্রাম বিভাগ", "জেলা", 23.2333, 90.6667),
            LocationItem("dist_lakshmipur", "লক্ষ্মীপুর", "Lakshmipur", "চট্টগ্রাম বিভাগ", "জেলা", 22.9425, 90.8412),
            LocationItem("dist_brahmanbaria", "ব্রাহ্মণবাড়িয়া", "Brahmanbaria", "চট্টগ্রাম বিভাগ", "জেলা", 23.9571, 91.1119),
            LocationItem("dist_rangamati", "রাঙ্গামাটি", "Rangamati", "চট্টগ্রাম বিভাগ", "জেলা", 22.6533, 92.1753),
            LocationItem("dist_khagrachhari", "খাগড়াছড়ি", "Khagrachhari", "চট্টগ্রাম বিভাগ", "জেলা", 23.1193, 91.9847),
            LocationItem("dist_bandarban", "বান্দরবান", "Bandarban", "চট্টগ্রাম বিভাগ", "জেলা", 22.1953, 92.2184),

            LocationItem("dist_sylhet", "সিলেট", "Sylhet", "সিলেট বিভাগ", "জেলা", 24.8949, 91.8687),
            LocationItem("dist_moulvibazar", "মৌলভীবাজার", "Moulvibazar", "সিলেট বিভাগ", "জেলা", 24.4829, 91.7774),
            LocationItem("dist_sreemangal", "শ্রীমঙ্গল", "Sreemangal", "মৌলভীবাজার, সিলেট", "উপজেলা", 24.3065, 91.7296),
            LocationItem("dist_habiganj", "হবিগঞ্জ", "Habiganj", "সিলেট বিভাগ", "জেলা", 24.3749, 91.4155),
            LocationItem("dist_sunamganj", "সুনামগঞ্জ", "Sunamganj", "সিলেট বিভাগ", "জেলা", 25.0658, 91.3950),

            LocationItem("dist_rajshahi", "রাজশাহী", "Rajshahi", "রাজশাহী বিভাগ", "জেলা", 24.3745, 88.6042),
            LocationItem("dist_bogura", "বগুড়া", "Bogura", "রাজশাহী বিভাগ", "জেলা", 24.8465, 89.3778),
            LocationItem("dist_pabna", "পাবনা", "Pabna", "রাজশাহী বিভাগ", "জেলা", 24.0064, 89.2372),
            LocationItem("dist_sirajganj", "সিরাজগঞ্জ", "Sirajganj", "রাজশাহী বিভাগ", "জেলা", 24.4534, 89.7007),
            LocationItem("dist_naogaon", "নওগাঁ", "Naogaon", "রাজশাহী বিভাগ", "জেলা", 24.7937, 88.9318),
            LocationItem("dist_natore", "নাটোর", "Natore", "রাজশাহী বিভাগ", "জেলা", 24.4206, 89.0003),
            LocationItem("dist_chapai", "চাঁপাইনবাবগঞ্জ", "Chapai Nawabganj", "রাজশাহী বিভাগ", "জেলা", 24.5965, 88.2775),
            LocationItem("dist_joypurhat", "জয়পুরহাট", "Joypurhat", "রাজশাহী বিভাগ", "জেলা", 25.1015, 89.0277),

            LocationItem("dist_rangpur", "রংপুর", "Rangpur", "রংপুর বিভাগ", "জেলা", 25.7439, 89.2752),
            LocationItem("dist_dinajpur", "দিনাজপুর", "Dinajpur", "রংপুর বিভাগ", "জেলা", 25.6279, 88.6332),
            LocationItem("dist_gaibandha", "গাইবান্ধা", "Gaibandha", "রংপুর বিভাগ", "জেলা", 25.3288, 89.5406),
            LocationItem("dist_nilphamari", "নীলফামারী", "Nilphamari", "রংপুর বিভাগ", "জেলা", 25.9318, 88.8560),
            LocationItem("dist_syedpur", "সৈয়দপুর", "Syedpur", "নীলফামারী, রংপুর", "শহর", 25.7778, 88.8917),
            LocationItem("dist_panchagarh", "পঞ্চগড়", "Panchagarh", "রংপুর বিভাগ", "জেলা", 26.3411, 88.5541),
            LocationItem("dist_tetulia", "তেঁতুলিয়া", "Tetulia", "পঞ্চগড়, রংপুর", "উপজেলা", 26.4950, 88.3470),
            LocationItem("dist_thakurgaon", "ঠাকুরগাঁও", "Thakurgaon", "রংপুর বিভাগ", "জেলা", 26.0337, 88.4617),
            LocationItem("dist_lalmonirhat", "লালমনিরহাট", "Lalmonirhat", "রংপুর বিভাগ", "জেলা", 25.9923, 89.2847),

            LocationItem("dist_khulna", "খুলনা", "Khulna", "খুলনা বিভাগ", "জেলা", 22.8456, 89.5403),
            LocationItem("dist_jashore", "যশোর", "Jashore", "খুলনা বিভাগ", "জেলা", 23.1664, 89.2182),
            LocationItem("dist_satkhira", "সাতক্ষীরা", "Satkhira", "খুলনা বিভাগ", "জেলা", 22.7185, 89.0705),
            LocationItem("dist_kushtia", "কুষ্টিয়া", "Kushtia", "খুলনা বিভাগ", "জেলা", 23.9013, 89.1206),
            LocationItem("dist_jhenaidah", "ঝিনাইদহ", "Jhenaidah", "খুলনা বিভাগ", "জেলা", 23.5448, 89.1539),
            LocationItem("dist_bagerhat", "বাগেরহাট", "Bagerhat", "খুলনা বিভাগ", "জেলা", 22.6516, 89.7859),
            LocationItem("dist_mongla", "মোংলা", "Mongla", "বাগেরহাট, খুলনা", "উপজেলা/বন্দর", 22.4833, 89.6000),
            LocationItem("dist_chuadanga", "চুয়াডাঙ্গা", "Chuadanga", "খুলনা বিভাগ", "জেলা", 23.6402, 88.8418),
            LocationItem("dist_magura", "মাগুরা", "Magura", "খুলনা বিভাগ", "জেলা", 23.4873, 89.4199),
            LocationItem("dist_meherpur", "মেহেরপুর", "Meherpur", "খুলনা বিভাগ", "জেলা", 23.7622, 88.6318),
            LocationItem("dist_narail", "নড়াইল", "Narail", "খুলনা বিভাগ", "জেলা", 23.1725, 89.5127),

            LocationItem("dist_barishal", "বরিশাল", "Barishal", "বরিশাল বিভাগ", "জেলা", 22.7010, 90.3535),
            LocationItem("dist_patuakhali", "পটুয়াখালী", "Patuakhali", "বরিশাল বিভাগ", "জেলা", 22.3596, 90.3299),
            LocationItem("dist_bhola", "ভোলা", "Bhola", "বরিশাল বিভাগ", "জেলা", 22.6859, 90.6481),
            LocationItem("dist_pirojpur", "পিরোজপুর", "Pirojpur", "বরিশাল বিভাগ", "জেলা", 22.5841, 89.9720),
            LocationItem("dist_barguna", "বরগুনা", "Barguna", "বরিশাল বিভাগ", "জেলা", 22.0953, 90.1121),
            LocationItem("dist_khepupara", "খেপুপাড়া (কলাপাড়া)", "Khepupara", "পটুয়াখালী, বরিশাল", "উপজেলা", 21.9833, 90.2333),
            LocationItem("dist_kuakata", "কুয়াকাটা", "Kuakata", "পটুয়াখালী, বরিশাল", "শহর/সৈকত", 21.8167, 90.1167),
            LocationItem("dist_jhalokati", "ঝালকাঠি", "Jhalokati", "বরিশাল বিভাগ", "জেলা", 22.6406, 90.1987),

            LocationItem("dist_mymensingh", "ময়মনসিংহ", "Mymensingh", "ময়মনসিংহ বিভাগ", "জেলা", 24.7471, 90.4203),
            LocationItem("dist_jamalpur", "জামালপুর", "Jamalpur", "ময়মনসিংহ বিভাগ", "জেলা", 24.9375, 89.9378),
            LocationItem("dist_netrokona", "নেত্রকোণা", "Netrokona", "ময়মনসিংহ বিভাগ", "জেলা", 24.8709, 90.7279),
            LocationItem("dist_sherpur", "শেরপুর", "Sherpur", "ময়মনসিংহ বিভাগ", "জেলা", 25.0205, 90.0153),

            LocationItem("dist_gazipur", "গাজীপুর", "Gazipur", "ঢাকা বিভাগ", "জেলা", 23.9999, 90.4203),
            LocationItem("dist_narayanganj", "নারায়ণগঞ্জ", "Narayanganj", "ঢাকা বিভাগ", "জেলা", 23.6238, 90.5000),
            LocationItem("dist_tangail", "টাঙ্গাইল", "Tangail", "ঢাকা বিভাগ", "জেলা", 24.2513, 89.9167),
            LocationItem("dist_kishoreganj", "কিশোরগঞ্জ", "Kishoreganj", "ঢাকা বিভাগ", "জেলা", 24.4449, 90.7766),
            LocationItem("dist_narsingdi", "নরসিংদী", "Narsingdi", "ঢাকা বিভাগ", "জেলা", 23.9322, 90.7154),
            LocationItem("dist_manikganj", "মানিকগঞ্জ", "Manikganj", "ঢাকা বিভাগ", "জেলা", 23.8644, 90.0047),
            LocationItem("dist_munshiganj", "মুন্সীগঞ্জ", "Munshiganj", "ঢাকা বিভাগ", "জেলা", 23.5422, 90.5305),
            LocationItem("dist_faridpur", "ফরিদপুর", "Faridpur", "ঢাকা বিভাগ", "জেলা", 23.6070, 89.8429),
            LocationItem("dist_gopalganj", "গোপালগঞ্জ", "Gopalganj", "ঢাকা বিভাগ", "জেলা", 23.0051, 89.8266),
            LocationItem("dist_madaripur", "মাদারীপুর", "Madaripur", "ঢাকা বিভাগ", "জেলা", 23.1641, 90.1897),
            LocationItem("dist_rajbari", "রাজবাড়ী", "Rajbari", "ঢাকা বিভাগ", "জেলা", 23.7574, 89.6445),
            LocationItem("dist_shariatpur", "শরীয়তপুর", "Shariatpur", "ঢাকা বিভাগ", "জেলা", 23.2423, 90.4348)
        )
    }
}
