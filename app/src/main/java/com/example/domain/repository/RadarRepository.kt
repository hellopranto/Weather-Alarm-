package com.example.domain.repository

import com.example.data.model.RainViewerResponse
import kotlinx.coroutines.flow.Flow

interface RadarRepository {
    fun getRadarMetadata(): Flow<Resource<RainViewerResponse>>
    fun getTileUrl(
        host: String,
        path: String,
        z: Int,
        x: Int,
        y: Int,
        colorScheme: Int = 2,
        smooth: Boolean = true
    ): String
}
