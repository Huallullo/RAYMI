package com.raymi.app.domain.repository

import com.raymi.app.domain.model.EmpresaData
import com.raymi.app.domain.model.Resource

interface RucLookupProvider {
    suspend fun buscar(ruc: String): Resource<EmpresaData>
}
