package com.raymi.app.domain.usecase.alquiler

import com.raymi.app.domain.model.Pago
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.AlquilerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Caso de uso para registrar un nuevo abono a un alquiler.
 */
class AddPagoUseCase @Inject constructor(
    private val alquilerRepository: AlquilerRepository
) {
    operator fun invoke(workspaceId: String, alquilerId: String, pago: Pago): Flow<Resource<Unit>> = flow {
        if (pago.monto <= 0) {
            emit(Resource.Error("El monto del pago debe ser mayor a 0"))
            return@flow
        }

        // Validar que el pago no exceda el saldo actual
        val alquilerResult = alquilerRepository.getAlquilerById(alquilerId).first()
        if (alquilerResult is Resource.Success) {
            val alquiler = alquilerResult.data
            if (alquiler != null && pago.monto > (alquiler.saldo + 0.01)) {
                emit(Resource.Error("El monto excede el saldo pendiente (S/. ${alquiler.saldo})"))
                return@flow
            }
        }

        alquilerRepository.addPago(workspaceId, alquilerId, pago).collect { result ->
            emit(result)
        }
    }
}
