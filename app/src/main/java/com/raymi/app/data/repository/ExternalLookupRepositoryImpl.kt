package com.raymi.app.data.repository

import com.raymi.app.data.remote.ExternalLookupService
import com.raymi.app.domain.model.EmpresaData
import com.raymi.app.domain.model.PersonaData
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.ExternalLookupRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExternalLookupRepositoryImpl @Inject constructor(
    private val service: ExternalLookupService
) : ExternalLookupRepository {
    override suspend fun buscarDni(dni: String): Resource<PersonaData> = service.buscarDni(dni)
    override suspend fun buscarRuc(ruc: String): Resource<EmpresaData> = service.buscarRuc(ruc)
}
