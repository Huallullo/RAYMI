package com.raymi.app.domain.usecase.cliente

import com.raymi.app.domain.repository.ClienteRepository
import javax.inject.Inject

class GetClienteByIdUseCase @Inject constructor(
    private val clienteRepository: ClienteRepository
) {
    suspend operator fun invoke(id: String) = clienteRepository.getClienteById(id)
}
