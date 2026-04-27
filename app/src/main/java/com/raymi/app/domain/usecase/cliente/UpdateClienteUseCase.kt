// ========== UpdateClienteUseCase.kt ==========
package com.raymi.app.domain.usecase.cliente

import com.raymi.app.domain.model.Cliente
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.ClienteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class UpdateClienteUseCase @Inject constructor(
    private val clienteRepository: ClienteRepository
) {
    suspend operator fun invoke(cliente: Cliente): Flow<Resource<Unit>> = flow {
        // Validaciones
        if (cliente.id.isBlank()) {
            emit(Resource.Error("ID de cliente inválido"))
            return@flow
        }

        if (cliente.dni.length != 8) {
            emit(Resource.Error("El DNI debe tener 8 dígitos"))
            return@flow
        }

        if (cliente.nombre.isBlank()) {
            emit(Resource.Error("El nombre es requerido"))
            return@flow
        }

        if (cliente.apellidos.isBlank()) {
            emit(Resource.Error("Los apellidos son requeridos"))
            return@flow
        }

        if (cliente.telefono.isBlank()) {
            emit(Resource.Error("El teléfono es requerido"))
            return@flow
        }

        // Actualizar
        clienteRepository.updateCliente(cliente).collect { result ->
            emit(result)
        }
    }
}
