package com.raymi.app.domain.usecase.alquiler

import com.raymi.app.domain.model.Pago
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.AlquilerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Obtiene el historial de pagos de un alquiler.
 */
class GetPagosUseCase @Inject constructor(
    private val alquilerRepository: AlquilerRepository
) {
    suspend operator fun invoke(workspaceId: String, alquilerId: String): Flow<Resource<List<Pago>>> =
        alquilerRepository.getPagos(workspaceId, alquilerId)
}
