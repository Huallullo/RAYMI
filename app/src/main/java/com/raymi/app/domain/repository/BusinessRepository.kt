package com.raymi.app.domain.repository

interface BusinessRepository {
    suspend fun getBusinessConfig(negocioId: String): Map<String, Any>?
    suspend fun updateBusinessConfig(negocioId: String, config: Map<String, Any>)
}