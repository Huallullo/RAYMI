package com.raymi.app.domain.usecase.alquiler

import com.raymi.app.domain.repository.AlquilerRepository
import javax.inject.Inject

class GetAlquileresOnceUseCase @Inject constructor(
    private val repository: AlquilerRepository
) {
    suspend operator fun invoke(workspaceId: String) = repository.getAlquileresOnce(workspaceId)
}
