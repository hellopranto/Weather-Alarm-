package com.example.location

data class LocationItem(
    val id: String,
    val nameBn: String,
    val nameEn: String,
    val hierarchyBn: String,
    val typeLabel: String, // জেলা, উপজেলা, শহর, ইউনিয়ন, গ্রাম/এলাকা
    val latitude: Double,
    val longitude: Double
)
