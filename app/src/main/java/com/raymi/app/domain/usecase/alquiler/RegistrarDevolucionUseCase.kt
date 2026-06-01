// ========== RegistrarDevolucionUseCase.kt ==========
package com.raymi.app.domain.usecase.alquiler

import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.AlquilerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Caso de uso para registrar la devolución física de un ítem.
 * Garantiza que el ítem vuelva a estar disponible y el contrato se cierre.
 * REGLA SENIOR: No permite devolución si hay saldo pendiente.
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
        if (alquilerId.isBlank()) {
            emit(Resource.Error("ID de alquiler no proporcionado"))
            return@flow
        }
        
        // 1. Validar saldo antes de proceder
        val alquilerResult = alquilerRepository.getAlquilerById(alquilerId).first { it !is Resource.Loading }
        if (alquilerResult is Resource.Success) {
            val alquiler = alquilerResult.data
            if (alquiler != null && alquiler.saldo > 0.01) {
                emit(Resource.Error("No se puede registrar devolución: Existe un saldo pendiente de ${alquiler.saldoFormateado}. Primero liquide la deuda."))
                return@flow
            }
            
            // Validar que no devuelva más de lo que tiene alquilado
            if (alquiler != null && unidadesARetornar > alquiler.cantidad) {
                emit(Resource.Error("No puedes devolver más unidades ($unidadesARetornar) de las alquiladas (${alquiler.cantidad})"))
                return@flow
            }
        }

        // 2. Ejecutar devolución
        alquilerRepository.registrarDevolucion(alquilerId, penalidad, observaciones, montoGarantiaRetenida, unidadesARetornar).collect { result ->
            emit(result)
        }
    }
}
