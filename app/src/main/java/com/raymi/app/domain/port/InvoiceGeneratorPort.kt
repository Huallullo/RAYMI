package com.raymi.app.domain.port

import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.Comprobante
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.Workspace

/**
 * Puerto de dominio para la emisión de comprobantes electrónicos con lógica de fallback.
 * [B-08] Desacoplamiento de la capa de datos.
 */
interface InvoiceGeneratorPort {
    suspend fun emitirConFallback(
        comprobante: Comprobante,
        alquiler: Alquiler,
        workspace: Workspace
    ): Resource<String>

    suspend fun emitirSoloLocal(
        comprobante: Comprobante,
        alquiler: Alquiler,
        workspace: Workspace
    ): Resource<String>
}
