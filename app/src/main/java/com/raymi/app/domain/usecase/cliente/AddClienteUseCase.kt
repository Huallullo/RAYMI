package com.raymi.app.domain.usecase.cliente

import com.raymi.app.domain.model.Cliente
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.ClienteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Caso de uso para agregar un nuevo cliente
 * Incluye validaciones de negocio antes de agregar
 */
class AddClienteUseCase @Inject constructor(
    private val clienteRepository: ClienteRepository
) {
    /**
     * Ejecuta la adición de un cliente con validaciones
     * @param cliente Cliente a agregar
     * @return Flow con el resultado de la operación
     */
    operator fun invoke(cliente: Cliente): Flow<Resource<String>> = flow {
        // Validar datos del cliente
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

        // Si pasa las validaciones, agregar el cliente
        clienteRepository.addCliente(cliente).collect { result ->
            emit(result)
        }
    }
}
