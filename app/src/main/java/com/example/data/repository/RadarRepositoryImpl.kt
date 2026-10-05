package com.example.data.repository

import com.example.data.model.RadarFrameItem
import com.example.data.model.RadarSeries
import com.example.data.model.RainViewerResponse
import com.example.data.remote.RadarApi
import com.example.domain.repository.RadarRepository
import com.example.domain.repository.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class RadarRepositoryImpl(
    private val radarApi: RadarApi
) : RadarRepository {

    override fun getRadarMetadata(): Flow<Resource<RainViewerResponse>> = flow {
        emit(Resource.Loading)
        try {
            // 1. Fetch live radar data from real backend endpoint
            val backendResponse = radarApi.getBackendRadarMaps()
            val frames = backendResponse.allPastFrames
            if (frames.isNotEmpty()) {
                val normalized = backendResponse.copy(
                    radar = RadarSeries(past = frames, nowcast = backendResponse.nowcastList.orEmpty())
                )
                emit(Resource.Success(normalized))
                return@flow
            }
        } catch (_: Exception) {}

        try {
            // 2. Fallback to upstream RainViewer public API directly
            val publicResponse = radarApi.getRainViewerPublicMaps()
            val frames = publicResponse.allPastFrames
            if (frames.isNotEmpty()) {
                val normalized = publicResponse.copy(
                    radar = RadarSeries(past = frames, nowcast = publicResponse.radar?.nowcast.orEmpty())
                )
                emit(Resource.Success(normalized))
                return@flow
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "ডপলার রাডার তথ্য পাওয়া যায়নি।"))
            return@flow
        }

        emit(Resource.Error("ডপলার রাডার তথ্য পাওয়া যায়নি।"))
    }.flowOn(Dispatchers.IO)

    override fun getTileUrl(
        host: String,
        path: String,
        z: Int,
        x: Int,
        y: Int,
        colorScheme: Int,
        smooth: Boolean
    ): String {
        val s = if (smooth) 1 else 0
        val normalizedHost = if (host.endsWith("/")) host.removeSuffix("/") else host
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        return "$normalizedHost$normalizedPath/256/$z/$x/$y/$colorScheme/${s}_1.png"
    }
}
