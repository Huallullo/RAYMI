package com.raymi.app.domain.usecase.vestuario

import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.Vestuario
import com.raymi.app.domain.repository.VestuarioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Caso de uso para agregar un nuevo vestuario
 * Incluye validaciones de negocio antes de agregar
 */
class AddVestuarioUseCase @Inject constructor(
    private val vestuarioRepository: VestuarioRepository
) {
    /**
     * Ejecuta la adición de un vestuario con validaciones
     * @param vestuario Vestuario a agregar
     * @return Flow con el resultado de la operación
     */
    suspend operator fun invoke(vestuario: Vestuario): Flow<Resource<String>> = flow {
        // Validar datos del vestuario
        if (vestuario.codigo.isBlank()) {
            emit(Resource.Error("El código es requerido"))
            return@flow
        }

        if (vestuario.danza.isBlank()) {
            emit(Resource.Error("El nombre de la danza es requerido"))
            return@flow
        }

        if (vestuario.departamento.isBlank()) {
            emit(Resource.Error("El departamento es requerido"))
            return@flow
        }

        if (vestuario.talla.isBlank()) {
            emit(Resource.Error("La talla es requerida"))
            return@flow
        }

        if (vestuario.precio <= 0) {
            emit(Resource.Error("El precio debe ser mayor a 0"))
            return@flow
        }

        // Si pasa las validaciones, agregar el vestuario
        vestuarioRepository.addVestuario(vestuario).collect { result ->
            emit(result)
        }
    }
}
