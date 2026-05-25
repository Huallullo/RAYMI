package com.raymi.app.domain.usecase.pdf

import android.net.Uri
import com.raymi.app.data.remote.PdfService
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.Workspace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Caso de uso para generar PDF de alquiler personalizado por negocio.
 */
class GenerarPdfAlquilerUseCase @Inject constructor(
    private val pdfService: PdfService
) {
    /**
     * Genera PDF del detalle del alquiler.
     * @param alquiler Datos del contrato.
     * @param workspace Información del negocio para el encabezado.
     */
    fun generarPdf(alquiler: Alquiler, workspace: Workspace?): Flow<Resource<Uri>> = flow {
        try {
            emit(Resource.Loading())
            val result = pdfService.generarComprobanteAlquiler(alquiler, workspace)
            emit(result)
        } catch (e: Exception) {
            emit(Resource.Error("Falla técnica al crear el PDF"))
        }
    }
}
