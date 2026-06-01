package com.raymi.app.domain.usecase.cliente

import com.raymi.app.domain.repository.ClienteRepository
import javax.inject.Inject

class GetClientesOnceUseCase @Inject constructor(
    private val repository: ClienteRepository
) {
    suspend operator fun invoke(workspaceId: String) = repository.getClientesOnce(workspaceId)
}
