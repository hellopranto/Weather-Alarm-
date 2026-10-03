package com.example.location

data class BangladeshCity(
    val nameEn: String,
    val nameBn: String,
    val divisionEn: String,
    val divisionBn: String,
    val latitude: Double,
    val longitude: Double,
    val isMajorDivision: Boolean = false
)

object BangladeshLocations {
    val defaultLocation = BangladeshCity(
        nameEn = "Dhaka",
        nameBn = "ঢাকা",
        divisionEn = "Dhaka",
        divisionBn = "ঢাকা",
        latitude = 23.8103,
        longitude = 90.4125,
        isMajorDivision = true
    )

    val allCities = listOf(
        // 8 Administrative Divisions
        BangladeshCity("Dhaka", "ঢাকা", "Dhaka", "ঢাকা", 23.8103, 90.4125, true),
        BangladeshCity("Chattogram", "চট্টগ্রাম", "Chattogram", "চট্টগ্রাম", 22.3569, 91.7832, true),
        BangladeshCity("Sylhet", "সিলেট", "Sylhet", "সিলেট", 24.8949, 91.8687, true),
        BangladeshCity("Rajshahi", "রাজশাহী", "Rajshahi", "রাজশাহী", 24.3745, 88.6042, true),
        BangladeshCity("Khulna", "খুলনা", "Khulna", "খুলনা", 22.8456, 89.5403, true),
        BangladeshCity("Barishal", "বরিশাল", "Barishal", "বরিশাল", 22.7010, 90.3535, true),
        BangladeshCity("Rangpur", "রংপুর", "Rangpur", "রংপুর", 25.7439, 89.2752, true),
        BangladeshCity("Mymensingh", "ময়মনসিংহ", "Mymensingh", "ময়মনসিংহ", 24.7471, 90.4203, true),

        // Key Meteorological & Coastal Districts
        BangladeshCity("Cox's Bazar", "কক্সবাজার", "Chattogram", "চট্টগ্রাম", 21.4272, 92.0058),
        BangladeshCity("Bogura", "বগুড়া", "Rajshahi", "রাজশাহী", 24.8465, 89.3778),
        BangladeshCity("Cumilla", "কুমিল্লা", "Chattogram", "চট্টগ্রাম", 23.4607, 91.1809),
        BangladeshCity("Gazipur", "গাজীপুর", "Dhaka", "ঢাকা", 24.0023, 90.4264),
        BangladeshCity("Narayanganj", "নারায়ণগঞ্জ", "Dhaka", "ঢাকা", 23.6238, 90.5000),
        BangladeshCity("Jessore", "যশোর", "Khulna", "খুলনা", 23.1664, 89.2081),
        BangladeshCity("Dinajpur", "দিনাজপুর", "Rangpur", "রংপুর", 25.6217, 88.6354),
        BangladeshCity("Tangail", "টাঙ্গাইল", "Dhaka", "ঢাকা", 24.2513, 89.9167),
        BangladeshCity("Faridpur", "ফরিদপুর", "Dhaka", "ঢাকা", 23.6071, 89.8429),
        BangladeshCity("Pabna", "পাবনা", "Rajshahi", "রাজশাহী", 24.0064, 89.2484),
        BangladeshCity("Kushtia", "কুষ্টিয়া", "Khulna", "খুলনা", 23.9013, 89.1205),
        BangladeshCity("Feni", "ফেনী", "Chattogram", "চট্টগ্রাম", 23.0187, 91.3966),
        BangladeshCity("Noakhali", "নোয়াখালী", "Chattogram", "চট্টগ্রাম", 22.8696, 91.0994),
        BangladeshCity("Brahmanbaria", "ব্রাহ্মণবাড়িয়া", "Chattogram", "চট্টগ্রাম", 23.9571, 91.1119),
        BangladeshCity("Sreemangal", "শ্রীমঙ্গল", "Sylhet", "সিলেট", 24.3065, 91.7296),
        BangladeshCity("Khepupara", "খেপুপাড়া", "Barishal", "বরিশাল", 21.9833, 90.2333),
        BangladeshCity("Mongla", "মোংলা", "Khulna", "খুলনা", 22.4833, 89.6000),
        BangladeshCity("Bhola", "ভোলা", "Barishal", "বরিশাল", 22.6859, 90.6481),
        BangladeshCity("Jamalpur", "জামালপুর", "Mymensingh", "ময়মনসিংহ", 24.9375, 89.9378),
        BangladeshCity("Sirajganj", "সিরাজগঞ্জ", "Rajshahi", "রাজশাহী", 24.4534, 89.7008),
        BangladeshCity("Patuakhali", "পটুয়াখালী", "Barishal", "বরিশাল", 22.3596, 90.3299),
        BangladeshCity("Netrokona", "নেত্রকোণা", "Mymensingh", "ময়মনসিংহ", 24.8703, 90.7279),
        BangladeshCity("Sunamganj", "সুনামগঞ্জ", "Sylhet", "সিলেট", 25.0658, 91.3950),
        BangladeshCity("Bandarban", "বান্দরবান", "Chattogram", "চট্টগ্রাম", 22.1953, 92.2184),
        BangladeshCity("Rangamati", "রাঙ্গামাটি", "Chattogram", "চট্টগ্রাম", 22.6533, 92.1753),
        BangladeshCity("Khagrachhari", "খাগড়াছড়ি", "Chattogram", "চট্টগ্রাম", 23.1193, 91.9847)
    )

    fun search(query: String): List<BangladeshCity> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return allCities
        return allCities.filter {
            it.nameEn.lowercase().contains(q) ||
            it.nameBn.contains(q) ||
            it.divisionEn.lowercase().contains(q) ||
            it.divisionBn.contains(q)
        }
    }
}
