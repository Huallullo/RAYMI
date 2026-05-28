package com.raymi.app.domain.repository

import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.Comprobante
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.Workspace

interface InvoiceProvider {
    val name: String
    suspend fun emitir(comprobante: Comprobante, alquiler: Alquiler, workspace: Workspace): Resource<String>
}
