package com.raymi.app.domain.usecase.vestuario

import com.raymi.app.domain.repository.VestuarioRepository
import javax.inject.Inject

/**
 * Caso de uso para obtener la lista de vestuarios
 * Maneja la lógica para recuperar todos los vestuarios
 */
class GetVestuariosUseCase @Inject constructor(
    private val vestuarioRepository: VestuarioRepository
) {
    /**
     * Ejecuta la obtención de vestuarios
     * @return Flow con la lista de vestuarios
     */
    suspend operator fun invoke() = vestuarioRepository.getVestuarios()
}
