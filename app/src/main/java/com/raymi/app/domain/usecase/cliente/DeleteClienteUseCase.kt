package com.raymi.app.domain.usecase.cliente

import com.raymi.app.domain.repository.ClienteRepository
import javax.inject.Inject

/**
 * Caso de uso para eliminar un cliente
 * Maneja la lógica de eliminación de clientes
 */
class DeleteClienteUseCase @Inject constructor(
    private val clienteRepository: ClienteRepository
) {
    /**
     * Ejecuta la eliminación de un cliente
     * @param clienteId ID del cliente a eliminar
     * @return Flow con el resultado de la operación
     */
    suspend operator fun invoke(clienteId: String) =
        clienteRepository.deleteCliente(clienteId)
}