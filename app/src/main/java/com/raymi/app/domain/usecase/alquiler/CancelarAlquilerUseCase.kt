package com.raymi.app.domain.usecase.alquiler

import com.raymi.app.domain.model.EstadoAlquiler
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.AlquilerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CancelarAlquilerUseCase @Inject constructor(
    private val alquilerRepository: AlquilerRepository
) {
    operator fun invoke(alquilerId: String, motivo: String): Flow<Resource<Unit>> = flow {
        if (alquilerId.isBlank()) {
            emit(Resource.Error("ID de alquiler no válido"))
            return@flow
        }

        // 1. Validar estado actual
        val result = alquilerRepository.getAlquilerById(alquilerId).first { it !is Resource.Loading }
        if (result is Resource.Success) {
            val alquiler = result.data
            if (alquiler != null && alquiler.estado != EstadoAlquiler.ACTIVO && alquiler.estado != EstadoAlquiler.RESERVADO) {
                emit(Resource.Error("No se puede cancelar un alquiler en estado ${alquiler.estado}"))
                return@flow
            }
        }

        // 2. Ejecutar cancelación
        alquilerRepository.cancelarAlquiler(alquilerId, motivo).collect { emit(it) }
    }
}
