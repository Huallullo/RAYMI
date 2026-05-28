package com.raymi.app.domain.usecase.pdf

import android.net.Uri
import com.raymi.app.domain.model.Item
import com.raymi.app.domain.model.Resource
import com.raymi.app.data.remote.PdfService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GenerarPdfInventarioUseCase @Inject constructor(
    private val pdfService: PdfService
) {
    suspend fun generarPdf(items: List<Item>, negocioNombre: String): Flow<Resource<Uri>> = flow {
        emit(Resource.Loading())
        emit(pdfService.generarPdfInventario(items, negocioNombre))
    }
}
