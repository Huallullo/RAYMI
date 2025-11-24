// ========== UpdateVestuarioUseCase.kt ==========
package com.raymi.app.domain.usecase.vestuario

import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.Vestuario
import com.raymi.app.domain.repository.VestuarioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class UpdateVestuarioUseCase @Inject constructor(
    private val vestuarioRepository: VestuarioRepository
) {
    suspend operator fun invoke(vestuario: Vestuario): Flow<Resource<Unit>> = flow {
        // Validaciones
        if (vestuario.id.isBlank()) {
            emit(Resource.Error("ID de vestuario inválido"))
            return@flow
        }

        if (vestuario.codigo.isBlank()) {
            emit(Resource.Error("El código es requerido"))
            return@flow
        }

        if (vestuario.danza.isBlank()) {
            emit(Resource.Error("El nombre de la danza es requerido"))
            return@flow
        }

        if (vestuario.precio <= 0) {
            emit(Resource.Error("El precio debe ser mayor a 0"))
            return@flow
        }

        // Actualizar
        vestuarioRepository.updateVestuario(vestuario).collect { result ->
            emit(result)
        }
    }
}