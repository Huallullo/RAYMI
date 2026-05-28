package com.raymi.app.data.remote

import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.Comprobante
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.Workspace
import com.raymi.app.domain.repository.InvoiceProvider
import javax.inject.Inject

class LocalPdfInvoiceProvider @Inject constructor(
    private val pdfService: PdfService
) : InvoiceProvider {
    
    override val name: String = "Generador Local (Contingencia)"

    override suspend fun emitir(comprobante: Comprobante, alquiler: Alquiler, workspace: Workspace): Resource<String> {
        val result = pdfService.generarPdfComprobante(comprobante, alquiler, workspace)
        return when (result) {
            is Resource.Success -> Resource.Success(result.data.toString())
            is Resource.Error -> Resource.Error(result.message ?: "Error en generación local")
            is Resource.Loading -> Resource.Loading()
        }
    }
}
