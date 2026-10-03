package com.example.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.TemperatureUnit
import com.example.data.local.ThemeMode
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.WindUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    fun setLanguage(langCode: String) {
        viewModelScope.launch {
            preferencesRepository.setLanguage(langCode)
        }
    }

    fun setTemperatureUnit(unit: TemperatureUnit) {
        viewModelScope.launch {
            preferencesRepository.setTemperatureUnit(unit)
        }
    }

    fun setWindUnit(unit: WindUnit) {
        viewModelScope.launch {
            preferencesRepository.setWindUnit(unit)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setUseGpsLocation(useGps: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setUseGpsLocation(useGps)
        }
    }
}
