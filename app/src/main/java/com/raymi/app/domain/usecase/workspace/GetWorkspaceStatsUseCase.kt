package com.raymi.app.domain.usecase.workspace

import com.raymi.app.data.remote.FirebaseDataSource
import com.raymi.app.domain.model.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Caso de uso para obtener las estadísticas consolidadas de un negocio.
 * Ahorra costos al leer 1 solo documento en lugar de miles.
 */
class GetWorkspaceStatsUseCase @Inject constructor(
    private val dataSource: FirebaseDataSource
) {
    suspend operator fun invoke(workspaceId: String): Flow<Resource<Map<String, Any>>> = flow {
        emit(Resource.Loading())
        try {
            val stats = dataSource.getStats(workspaceId)
            if (stats != null) {
                emit(Resource.Success(stats))
            } else {
                emit(Resource.Error("No se encontraron estadísticas"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Falla al conectar con el servidor"))
        }
    }
}
