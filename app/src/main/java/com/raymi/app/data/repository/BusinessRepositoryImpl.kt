package com.raymi.app.data.repository

import com.raymi.app.core.utils.Constants.COLLECTION_NEGOCIOS
import com.raymi.app.data.remote.FirebaseDataSource
import com.raymi.app.domain.repository.BusinessRepository
import javax.inject.Inject

class BusinessRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseDataSource
) : BusinessRepository {

    override suspend fun getBusinessConfig(negocioId: String): Map<String, Any>? {
        return dataSource.getDocument(COLLECTION_NEGOCIOS, negocioId)
    }

    override suspend fun updateBusinessConfig(negocioId: String, config: Map<String, Any>) {
        dataSource.updateDocument(COLLECTION_NEGOCIOS, negocioId, config)
    }
}
