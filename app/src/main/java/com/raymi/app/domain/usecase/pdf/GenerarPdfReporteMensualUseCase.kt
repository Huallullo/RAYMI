package com.raymi.app.domain.usecase.pdf

import android.net.Uri
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.Pago
import com.raymi.app.domain.model.Resource
import com.raymi.app.data.remote.PdfService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GenerarPdfReporteMensualUseCase @Inject constructor(
    private val pdfService: PdfService
) {
    fun generarPdf(alquileres: List<Alquiler>, pagos: List<Pago>, mes: Int, anio: Int): Flow<Resource<Uri>> = flow {
        emit(Resource.Loading())
        // Usamos la lógica de auditoría real que requiere pagos
        emit(pdfService.generarPdfResumenFinanciero(alquileres, pagos, anio))
    }
}
