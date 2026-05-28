package com.raymi.app.domain.usecase.workspace

import com.raymi.app.data.remote.StatsDataSource
import com.raymi.app.domain.model.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Caso de uso para obtener estadísticas del negocio.
 * Optimizado para leer de un único documento de metadatos.
 */
class GetWorkspaceStatsUseCase @Inject constructor(
    private val statsDataSource: StatsDataSource
) {
    suspend operator fun invoke(workspaceId: String): Flow<Resource<Map<String, Any>>> = flow {
        emit(Resource.Loading())
        try {
            val stats = statsDataSource.getStats(workspaceId)
            if (stats != null) {
                emit(Resource.Success(stats))
            } else {
                emit(Resource.Success<Map<String, Any>>(emptyMap()))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Error al cargar estadísticas: ${e.message}"))
        }
    }
}
