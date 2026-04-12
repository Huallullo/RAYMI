package com.raymi.app.domain.usecase.alquiler

import com.raymi.app.domain.repository.AlquilerRepository
import javax.inject.Inject

/**
 * Caso de uso para obtener la lista de alquileres
 * Maneja la lógica para recuperar todos los alquileres
 */
class GetAlquileresUseCase @Inject constructor(
    private val alquilerRepository: AlquilerRepository
) {
    /**
     * Ejecuta la obtención de alquileres
     * @return Flow con la lista de alquileres
     */
    suspend operator fun invoke() = alquilerRepository.getAlquileres()
}