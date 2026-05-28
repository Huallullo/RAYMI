package com.raymi.app.domain.repository

import com.raymi.app.domain.model.EmpresaData
import com.raymi.app.domain.model.PersonaData
import com.raymi.app.domain.model.Resource

interface ExternalLookupRepository {
    suspend fun buscarDni(dni: String): Resource<PersonaData>
    suspend fun buscarRuc(ruc: String): Resource<EmpresaData>
}
