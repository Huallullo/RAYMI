package com.raymi.app.data.remote

import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.Comprobante
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.Workspace
import com.raymi.app.domain.repository.InvoiceProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FallbackInvoiceService @Inject constructor(
    private val nubefactProvider: NubefactInvoiceProvider,
    private val apiperuProvider: ApiperuInvoiceProvider,
    private val miapiProvider: MiapiInvoiceProvider,
    private val localProvider: LocalPdfInvoiceProvider
) : com.raymi.app.domain.port.InvoiceGeneratorPort {
    override suspend fun emitirConFallback(
        comprobante: Comprobante,
        alquiler: Alquiler,
        workspace: Workspace
    ): Resource<String> {
        val providers = listOf(nubefactProvider, apiperuProvider, miapiProvider, localProvider)
        var lastError = "No hay proveedores disponibles"

        for (provider in providers) {
            val res = provider.emitir(comprobante, alquiler, workspace)
            if (res is Resource.Success) {
                android.util.Log.d("RAYMI_BILLING", "Comprobante emitido con éxito usando: ${provider.name}")
                return res
            }
            if (res is Resource.Error) {
                lastError = res.message ?: lastError
                android.util.Log.w("RAYMI_BILLING", "Falla con proveedor ${provider.name}: $lastError")
            }
        }

        return Resource.Error("❌ Error crítico: Fallaron todos los sistemas de facturación. Último error: $lastError")
    }

    override suspend fun emitirSoloLocal(
        comprobante: Comprobante,
        alquiler: Alquiler,
        workspace: Workspace
    ): Resource<String> {
        return localProvider.emitir(comprobante, alquiler, workspace)
    }
}
