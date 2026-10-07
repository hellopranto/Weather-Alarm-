package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val AnekBanglaFontFamily = FontFamily(
    Font(R.font.anek_bangla, FontWeight.Light),
    Font(R.font.anek_bangla, FontWeight.Normal),
    Font(R.font.anek_bangla, FontWeight.Medium),
    Font(R.font.anek_bangla, FontWeight.SemiBold),
    Font(R.font.anek_bangla, FontWeight.Bold),
    Font(R.font.anek_bangla, FontWeight.ExtraBold)
)

val Typography = Typography(
    // Temperature and Primary Numbers ("700")
    displayLarge = TextStyle(
        fontFamily = AnekBanglaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp
    ),
    displayMedium = TextStyle(
        fontFamily = AnekBanglaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp
    ),
    displaySmall = TextStyle(
        fontFamily = AnekBanglaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    // Main Headings & Critical Alerts ("600–700")
    headlineLarge = TextStyle(
        fontFamily = AnekBanglaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AnekBanglaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = AnekBanglaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    // Section Titles and Main Headings ("600–700")
    titleLarge = TextStyle(
        fontFamily = AnekBanglaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    // Location Names & Card Titles ("500–600")
    titleMedium = TextStyle(
        fontFamily = AnekBanglaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = AnekBanglaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    // Weather Information & Dynamic Backend Text ("400–500")
    bodyLarge = TextStyle(
        fontFamily = AnekBanglaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    // Forecast Information & Metric Values ("400–500")
    bodyMedium = TextStyle(
        fontFamily = AnekBanglaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    // Small Secondary Text & Timestamps ("300–400")
    bodySmall = TextStyle(
        fontFamily = AnekBanglaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    // Buttons & Interactive Elements ("500–600")
    labelLarge = TextStyle(
        fontFamily = AnekBanglaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    // Filter Chips and Tab Labels ("500–600")
    labelMedium = TextStyle(
        fontFamily = AnekBanglaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    // Secondary Badges & Micro Text ("300–400")
    labelSmall = TextStyle(
        fontFamily = AnekBanglaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
)
