// ========== RegistrarDevolucionUseCase.kt ==========
package com.raymi.app.domain.usecase.alquiler

import com.raymi.app.domain.model.EstadoAlquiler
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.AlquilerRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * Caso de uso para registrar la devolución física de un ítem.
 * [A-12] Verificación de estado e integridad del contrato.
 */
class RegistrarDevolucionUseCase @Inject constructor(
    private val alquilerRepository: AlquilerRepository
) {
    operator fun invoke(
        alquilerId: String,
        penalidad: Double = 0.0,
        observaciones: String = "",
        montoGarantiaRetenida: Double = 0.0,
        unidadesARetornar: Int = 0
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        if (alquilerId.isBlank()) {
            emit(Resource.Error("ID de alquiler no proporcionado"))
            return@flow
        }
        
        // 1. Validar estado y saldo antes de proceder
        val alquilerResult = alquilerRepository.getAlquilerById(alquilerId).first { it !is Resource.Loading }
        if (alquilerResult is Resource.Success) {
            val alquiler = alquilerResult.data ?: run {
                emit(Resource.Error("Contrato no encontrado"))
                return@flow
            }

            // ✅ [A-12] Bloquear si ya está cerrado
            if (alquiler.estado == EstadoAlquiler.DEVUELTO || alquiler.estado == EstadoAlquiler.CANCELADO) {
                emit(Resource.Error("Este contrato ya está cerrado (${alquiler.estado.name.lowercase()})"))
                return@flow
            }

            if (alquiler.saldo > 0.01) {
                emit(Resource.Error("No se puede registrar devolución: Existe un saldo pendiente de ${alquiler.saldoFormateado}. Primero liquide la deuda."))
                return@flow
            }
            
            // Validar que no devuelva más de lo que tiene alquilado
            if (unidadesARetornar > alquiler.cantidad) {
                emit(Resource.Error("No puedes devolver más unidades ($unidadesARetornar) de las alquiladas (${alquiler.cantidad})"))
                return@flow
            }
        }

        // 2. Ejecutar devolución
        emitAll(alquilerRepository.registrarDevolucion(alquilerId, penalidad, observaciones, montoGarantiaRetenida, unidadesARetornar))
    }
}
