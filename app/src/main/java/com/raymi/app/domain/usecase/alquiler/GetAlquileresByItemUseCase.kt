package com.raymi.app.domain.usecase.alquiler

import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.AlquilerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Caso de uso para obtener el historial de alquileres de un ítem específico.
 */
class GetAlquileresByItemUseCase @Inject constructor(
    private val repository: AlquilerRepository
) {
    suspend operator fun invoke(workspaceId: String, itemId: String): Flow<Resource<List<Alquiler>>> {
        return repository.getAlquileresByItem(workspaceId, itemId)
    }
}
