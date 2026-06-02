package com.raymi.app.domain.usecase.workspace

import com.raymi.app.core.cache.SmartCache
import com.raymi.app.data.remote.StatsDataSource
import com.raymi.app.domain.model.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Caso de uso para obtener estadísticas del negocio.
 * Optimizado para leer de un único documento de metadatos con caché.
 */
@Singleton
class GetWorkspaceStatsUseCase @Inject constructor(
    private val statsDataSource: StatsDataSource
) {
    private val statsCache = mutableMapOf<String, SmartCache<Map<String, Any>>>()
    private val TTL_3_MIN = 3 * 60 * 1000L
    private val mutex = Mutex() // ✅ Thread-safety para el mapa de caches

    private suspend fun getCacheFor(workspaceId: String): SmartCache<Map<String, Any>> {
        return mutex.withLock {
            statsCache.getOrPut(workspaceId) { SmartCache() }
        }
    }

    suspend operator fun invoke(workspaceId: String, forceRefresh: Boolean = false): Flow<Resource<Map<String, Any>>> = flow {
        emit(Resource.Loading())
        
        val cache = getCacheFor(workspaceId)
        if (!forceRefresh) {
            cache.get()?.let {
                emit(Resource.Success(it))
                return@flow
            }
        }

        try {
            val stats = statsDataSource.getStats(workspaceId)
            if (stats != null) {
                cache.set(stats, TTL_3_MIN)
                emit(Resource.Success(stats))
            } else {
                emit(Resource.Success<Map<String, Any>>(emptyMap()))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Error al cargar estadísticas: ${e.message}"))
        }
    }

    suspend fun invalidateCache(workspaceId: String) {
        getCacheFor(workspaceId).invalidate()
    }
}
