package com.raymi.app.domain.usecase.pdf

import android.net.Uri
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.Workspace
import com.raymi.app.domain.port.PdfGeneratorPort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Caso de uso para generar PDF de alquiler personalizado por negocio.
 * [B-08] Desacoplado de la capa de datos.
 */
class GenerarPdfAlquilerUseCase @Inject constructor(
    private val pdfGenerator: PdfGeneratorPort
) {
    /**
     * Genera PDF del detalle del alquiler.
     * @param alquiler Datos del contrato.
     * @param workspace Información del negocio para el encabezado.
     */
    fun generarPdf(alquiler: Alquiler, workspace: Workspace?, pagos: List<com.raymi.app.domain.model.Pago> = emptyList()): Flow<Resource<Uri>> = flow {
        try {
            emit(Resource.Loading())
            val result = pdfGenerator.generarComprobanteAlquiler(alquiler, workspace, pagos)
            emit(result)
        } catch (e: Exception) {
            emit(Resource.Error("Falla técnica al crear el PDF"))
        }
    }
}
