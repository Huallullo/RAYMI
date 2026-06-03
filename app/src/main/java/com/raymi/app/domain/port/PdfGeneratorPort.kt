package com.raymi.app.domain.port

import android.net.Uri
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.Workspace
import com.raymi.app.domain.model.Pago

/**
 * Puerto de dominio para la generación de documentos PDF.
 * [B-08] Desacoplamiento de la capa de datos.
 */
interface PdfGeneratorPort {
    suspend fun generarComprobanteAlquiler(
        alquiler: Alquiler, 
        workspace: Workspace?, 
        pagos: List<Pago>
    ): Resource<Uri>
}
