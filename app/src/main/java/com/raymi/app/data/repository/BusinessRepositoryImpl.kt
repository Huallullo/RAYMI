package com.raymi.app.data.repository

import com.raymi.app.data.remote.FirebaseDataSource
import com.raymi.app.domain.repository.BusinessRepository
import javax.inject.Inject

class BusinessRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseDataSource
) : BusinessRepository {

    override suspend fun getBusinessConfig(negocioId: String): Map<String, Any>? {
        return dataSource.getBusinessDocument("negocios", negocioId)
    }

    override suspend fun updateBusinessConfig(negocioId: String, config: Map<String, Any>) {
        dataSource.updateBusinessDocument("negocios", negocioId, config)
    }
}