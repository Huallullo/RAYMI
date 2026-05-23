package com.raymi.app.domain.usecase.business

import com.raymi.app.domain.repository.BusinessRepository
import javax.inject.Inject

class CheckBusinessConfigUseCase @Inject constructor(
    private val businessRepository: BusinessRepository
) {
    suspend operator fun invoke(negocioId: String): Boolean {
        val config = businessRepository.getBusinessConfig(negocioId) ?: return false
        // Verificar si la configuración esencial está presente
        val rubro = config["rubro"] as? String
        val tipoActivo = config["tipoActivo"] as? String
        return !rubro.isNullOrBlank() && !tipoActivo.isNullOrBlank()
    }
}