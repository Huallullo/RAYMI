// ========== UpdateAlquilerUseCase.kt ==========
package com.raymi.app.domain.usecase.alquiler

import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.AlquilerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class UpdateAlquilerUseCase @Inject constructor(
    private val alquilerRepository: AlquilerRepository
) {
    operator fun invoke(alquiler: Alquiler): Flow<Resource<Unit>> = flow {
        // Validaciones
        if (alquiler.id.isBlank()) {
            emit(Resource.Error("ID de alquiler inválido"))
            return@flow
        }

        if (alquiler.precioTotal <= 0) {
            emit(Resource.Error("El precio debe ser mayor a 0"))
            return@flow
        }

        if (alquiler.adelanto < 0) {
            emit(Resource.Error("El adelanto no puede ser negativo"))
            return@flow
        }

        if (alquiler.adelanto > alquiler.precioTotal) {
            emit(Resource.Error("El adelanto no puede ser mayor al precio total"))
            return@flow
        }

        // Actualizar
        alquilerRepository.updateAlquiler(alquiler).collect { result ->
            emit(result)
        }
    }
}
