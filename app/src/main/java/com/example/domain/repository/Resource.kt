package com.example.domain.repository

sealed class Resource<out T> {
    data class Success<out T>(val data: T, val isOfflineCached: Boolean = false) : Resource<T>()
    data class Error(val message: String, val cachedData: Any? = null) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()
}
