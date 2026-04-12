// ========== GetVestuarioByIdUseCase.kt ==========
package com.raymi.app.domain.usecase.vestuario

import com.raymi.app.domain.repository.VestuarioRepository
import javax.inject.Inject

class GetVestuarioByIdUseCase @Inject constructor(
    private val vestuarioRepository: VestuarioRepository
) {
    suspend operator fun invoke(vestuarioId: String) =
        vestuarioRepository.getVestuarioById(vestuarioId)
}
