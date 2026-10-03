package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.data.local.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = WeatherPrimaryDark,
    onPrimary = WeatherOnPrimaryDark,
    primaryContainer = WeatherPrimaryContainerDark,
    onPrimaryContainer = WeatherOnPrimaryContainerDark,
    secondary = WeatherSecondaryDark,
    onSecondary = WeatherOnSecondaryDark,
    secondaryContainer = WeatherSecondaryContainerDark,
    onSecondaryContainer = WeatherOnSecondaryContainerDark,
    tertiary = WeatherTertiaryDark,
    onTertiary = WeatherOnTertiaryDark,
    tertiaryContainer = WeatherTertiaryContainerDark,
    onTertiaryContainer = WeatherOnTertiaryContainerDark,
    background = WeatherBackgroundDark,
    onBackground = WeatherOnBackgroundDark,
    surface = WeatherSurfaceDark,
    onSurface = WeatherOnSurfaceDark,
    surfaceVariant = WeatherSurfaceVariantDark,
    onSurfaceVariant = WeatherOnSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = WeatherPrimaryLight,
    onPrimary = WeatherOnPrimaryLight,
    primaryContainer = WeatherPrimaryContainerLight,
    onPrimaryContainer = WeatherOnPrimaryContainerLight,
    secondary = WeatherSecondaryLight,
    onSecondary = WeatherOnSecondaryLight,
    secondaryContainer = WeatherSecondaryContainerLight,
    onSecondaryContainer = WeatherOnSecondaryContainerLight,
    tertiary = WeatherTertiaryLight,
    onTertiary = WeatherOnTertiaryLight,
    tertiaryContainer = WeatherTertiaryContainerLight,
    onTertiaryContainer = WeatherOnTertiaryContainerLight,
    background = WeatherBackgroundLight,
    onBackground = WeatherOnBackgroundLight,
    surface = WeatherSurfaceLight,
    onSurface = WeatherOnSurfaceLight,
    surfaceVariant = WeatherSurfaceVariantLight,
    onSurfaceVariant = WeatherOnSurfaceVariantLight
)

@Composable
fun MyApplicationTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false, // Weather app should showcase curated atmospheric blues
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
