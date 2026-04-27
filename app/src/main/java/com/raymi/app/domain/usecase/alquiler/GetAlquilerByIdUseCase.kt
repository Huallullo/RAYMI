// ========== GetAlquilerByIdUseCase.kt ==========
package com.raymi.app.domain.usecase.alquiler

import com.raymi.app.domain.repository.AlquilerRepository
import javax.inject.Inject

class GetAlquilerByIdUseCase @Inject constructor(
    private val alquilerRepository: AlquilerRepository
) {
    suspend operator fun invoke(alquilerId: String) =
        alquilerRepository.getAlquilerById(alquilerId)
}
