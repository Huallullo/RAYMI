// ========== RegistrarDevolucionUseCase.kt ==========
package com.raymi.app.domain.usecase.alquiler

import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.AlquilerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Caso de uso para registrar la devolución física de un ítem.
 * Garantiza que el ítem vuelva a estar disponible y el contrato se cierre.
 */
class RegistrarDevolucionUseCase @Inject constructor(
    private val alquilerRepository: AlquilerRepository
) {
    operator fun invoke(alquilerId: String): Flow<Resource<Unit>> = flow {
        if (alquilerId.isBlank()) {
            emit(Resource.Error("ID de alquiler no proporcionado"))
            return@flow
        }
        
        alquilerRepository.registrarDevolucion(alquilerId).collect { result ->
            emit(result)
        }
    }
}
