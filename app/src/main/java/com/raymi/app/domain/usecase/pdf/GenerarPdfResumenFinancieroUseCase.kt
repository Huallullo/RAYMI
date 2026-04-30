package com.raymi.app.domain.usecase.pdf

import android.net.Uri
import com.raymi.app.data.remote.PdfService
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GenerarPdfResumenFinancieroUseCase @Inject constructor(
    private val pdfService: PdfService
) {
    fun generarPdf(alquileres: List<Alquiler>, year: Int): Flow<Resource<Uri>> = flow {
        emit(Resource.Loading())
        emit(pdfService.generarPdfResumenFinanciero(alquileres, year))
    }
}