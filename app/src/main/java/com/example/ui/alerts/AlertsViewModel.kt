package com.example.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.WeatherAlertModel
import com.example.domain.repository.BmdWeatherRepository
import com.example.domain.repository.Resource
import com.example.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AlertsUiState(
    val isLoading: Boolean = true,
    val alerts: List<WeatherAlertModel> = emptyList(),
    val error: String? = null
)

class AlertsViewModel(
    private val weatherRepository: WeatherRepository,
    private val bmdRepository: BmdWeatherRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    init {
        loadAlerts()
    }

    fun loadAlerts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val prefs = preferencesRepository.userPreferencesFlow.first()
            val lat = if (prefs.selectedLatitude != 0.0) prefs.selectedLatitude else 23.8103
            val lon = if (prefs.selectedLongitude != 0.0) prefs.selectedLongitude else 90.4125

            val accumulatedAlerts = mutableListOf<WeatherAlertModel>()

            // 1. Fetch local warnings from unified weather API
            try {
                weatherRepository.getWeather(lat, lon, prefs.selectedCityName).collect { res ->
                    if (res is Resource.Success) {
                        val localList = if (res.data.warnings.isNotEmpty()) res.data.warnings else res.data.alerts
                        accumulatedAlerts.addAll(localList)
                    }
                }
            } catch (_: Exception) {}

            // 2. Fetch nationwide BMD warnings bulletin directly
            try {
                bmdRepository.getWeatherWarnings().collect { bmdRes ->
                    if (bmdRes is Resource.Success) {
                        val bmdAlerts = bmdRes.data.map { w ->
                            val sigTitle = if (w.signalNumber != null) {
                                "নদীবন্দর ${toBanglaNum(w.signalNumber)} নম্বর সতর্ক সংকেত"
                            } else {
                                translateWarningTitle(w.title)
                            }
                            WeatherAlertModel(
                                id = w.id,
                                title = w.title,
                                titleBn = sigTitle,
                                description = w.description,
                                descriptionBn = translateWarningDescription(w.description),
                                severity = w.severity,
                                startTime = w.issuedAt,
                                endTime = w.validUntil,
                                source = "বাংলাদেশ আবহাওয়া অধিদপ্তর (BMD বুলেটিন)",
                                signalNumber = w.signalNumber,
                                regions = w.affectedRegions.map { translateRegion(it) }
                            )
                        }
                        accumulatedAlerts.addAll(bmdAlerts)
                    }
                }
            } catch (_: Exception) {}

            val distinct = accumulatedAlerts.distinctBy { it.id.ifBlank { it.title } }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                alerts = distinct,
                error = null
            )
        }
    }

    companion object {
        fun toBanglaNum(num: Int): String {
            val en = "0123456789"
            val bn = "০১২৩৪৫৬৭৮৯"
            return num.toString().map { ch ->
                val idx = en.indexOf(ch)
                if (idx != -1) bn[idx] else ch
            }.joinToString("")
        }

        fun translateRegion(reg: String): String {
            return when (reg.lowercase()) {
                "sylhet" -> "সিলেট"
                "mymensingh" -> "ময়মনসিংহ"
                "chattogram", "chittagong" -> "চট্টগ্রাম"
                "dhaka" -> "ঢাকা"
                "barishal", "barisal" -> "বরিশাল"
                "khulna" -> "খুলনা"
                "rajshahi" -> "রাজশাহী"
                "rangpur" -> "রংপুর"
                "cox's bazar", "coxs bazar" -> "কক্সবাজার"
                else -> reg
            }
        }

        fun translateWarningTitle(title: String): String {
            return when {
                title.contains("Cautionary Signal No. 1", ignoreCase = true) -> "নদীবন্দর ১ নম্বর সতর্ক সংকেত"
                title.contains("Cautionary Signal No. 2", ignoreCase = true) -> "নদীবন্দর ২ নম্বর সতর্ক সংকেত"
                title.contains("Cautionary Signal No. 3", ignoreCase = true) -> "নদীবন্দর ৩ নম্বর সতর্ক সংকেত"
                title.contains("Signal No. 4", ignoreCase = true) -> "নদীবন্দর ৪ নম্বর হুঁশিয়ারি সংকেত"
                title.contains("Signal No. 10", ignoreCase = true) -> "মহাবিপদ সংকেত ১০ নম্বর"
                title.contains("Norwester", ignoreCase = true) || title.contains("Kalbaishakhi", ignoreCase = true) -> "কালবৈশাখী ঝড়ের পূর্বাভাস"
                else -> title
            }
        }

        fun translateWarningDescription(desc: String): String {
            return when {
                desc.contains("River ports are advised to hoist cautionary signal number one", ignoreCase = true) ->
                    "সিলেট, ময়মনসিংহ এবং চট্টগ্রাম অঞ্চলের উপর দিয়ে অস্থায়ীভাবে দমকা অথবা ঝোড়ো হাওয়াসহ বৃষ্টি বা বজ্রসহ বৃষ্টিপাত হতে পারে। নদী বন্দরসমূহকে ১ নম্বর সতর্ক সংকেত দেখাতে বলা হয়েছে।"
                else -> desc
            }
        }
    }
}
