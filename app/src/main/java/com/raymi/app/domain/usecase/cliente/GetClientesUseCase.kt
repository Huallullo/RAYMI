package com.raymi.app.domain.usecase.cliente

import com.raymi.app.domain.repository.ClienteRepository
import javax.inject.Inject

/**
 * Caso de uso para obtener la lista de clientes
 * Maneja la lógica para recuperar todos los clientes
 */
class GetClientesUseCase @Inject constructor(
    private val clienteRepository: ClienteRepository
) {
    /**
     * Ejecuta la obtención de clientes
     * @return Flow con la lista de clientes
     */
    suspend operator fun invoke() = clienteRepository.getClientes()
}