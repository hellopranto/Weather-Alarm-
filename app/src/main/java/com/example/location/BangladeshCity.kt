package com.example.location

data class BangladeshCity(
    val nameEn: String,
    val nameBn: String,
    val districtEn: String,
    val districtBn: String,
    val latitude: Double,
    val longitude: Double,
    val isDivision: Boolean = false
)
