package com.raymi.app.domain.usecase.pdf

import android.net.Uri
import com.raymi.app.data.remote.PdfService
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Caso de uso para generar PDF de alquiler
 */
class GenerarPdfAlquilerUseCase @Inject constructor(
    private val pdfService: PdfService
) {
    /**
     * Genera PDF del detalle del alquiler
     */
    fun generarPdf(alquiler: Alquiler): Flow<Resource<Uri>> = flow {
        try {
            emit(Resource.Loading())
            val result = pdfService.generarPdfAlquiler(alquiler)
            emit(result)
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error desconocido"))
        }
    }
}
