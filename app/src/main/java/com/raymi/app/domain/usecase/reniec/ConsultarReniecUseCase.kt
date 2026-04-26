package com.raymi.app.domain.usecase.reniec

import com.raymi.app.data.remote.ReniecData
import com.raymi.app.data.remote.ReniecService
import com.raymi.app.domain.model.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Caso de uso para consultar datos de RENIEC por DNI
 */
class ConsultarReniecUseCase @Inject constructor(
    private val reniecService: ReniecService
) {
    operator fun invoke(dni: String): Flow<Resource<ReniecData>> = flow {
        try {
            emit(Resource.Loading())
            val result = reniecService.consultarPorDni(dni)
            emit(result)
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error desconocido"))
        }
    }
}
