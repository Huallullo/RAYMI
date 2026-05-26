package com.raymi.app.domain.usecase.alquiler

import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.DomainError
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.AlquilerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Caso de uso para crear un nuevo alquiler
 * Incluye validaciones de negocio antes de crear
 */
class CreateAlquilerUseCase @Inject constructor(
    private val alquilerRepository: AlquilerRepository
) {
    /**
     * Ejecuta la creación de un alquiler con validaciones
     * @param alquiler Alquiler a crear
     * @return Flow con el resultado de la operación
     */
    operator fun invoke(alquiler: Alquiler): Flow<Resource<String>> = flow {
        // Validar datos del alquiler
        if (alquiler.clienteId.isBlank()) {
            emit(Resource.Error("Debe seleccionar un cliente"))
            return@flow
        }

        if (alquiler.itemId.isBlank()) {
            emit(Resource.Error("Debe seleccionar un producto"))
            return@flow
        }

        if (alquiler.precioTotal <= 0) {
            emit(Resource.Error("El precio debe ser mayor a 0"))
            return@flow
        }

        if (alquiler.adelanto < 0) {
            emit(Resource.Error(DomainError.NegativeBalance.message))
            return@flow
        }

        if (alquiler.adelanto > alquiler.precioTotal) {
            emit(Resource.Error("El adelanto no puede ser mayor al precio total"))
            return@flow
        }

        // Validar fechas
        if (alquiler.fechaFinPrevista.seconds <= alquiler.fechaInicio.seconds) {
            emit(Resource.Error(DomainError.InvalidDateRange.message))
            return@flow
        }

        // Si pasa las validaciones, crear el alquiler
        alquilerRepository.createAlquiler(alquiler).collect { result ->
            emit(result)
        }
    }
}
