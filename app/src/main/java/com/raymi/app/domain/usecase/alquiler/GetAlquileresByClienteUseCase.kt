package com.raymi.app.domain.usecase.alquiler

import com.raymi.app.domain.repository.AlquilerRepository
import javax.inject.Inject

class GetAlquileresByClienteUseCase @Inject constructor(
    private val repository: AlquilerRepository
) {
    suspend operator fun invoke(workspaceId: String, clienteId: String) = 
        repository.getAlquileresByCliente(workspaceId, clienteId)
}
