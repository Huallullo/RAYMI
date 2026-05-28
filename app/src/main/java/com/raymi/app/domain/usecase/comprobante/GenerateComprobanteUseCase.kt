package com.raymi.app.domain.usecase.comprobante

import com.raymi.app.domain.model.*
import com.raymi.app.domain.repository.ComprobanteRepository
import com.raymi.app.data.remote.PdfService
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class GenerateComprobanteUseCase @Inject constructor(
    private val repository: ComprobanteRepository,
    private val pdfService: PdfService
) {
    suspend operator fun invoke(
        comprobante: Comprobante,
        alquiler: Alquiler,
        workspace: Workspace
    ): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            // 1. Obtener número correlativo atómico
            val numberResult = repository.getNextNumber(workspace.id, comprobante.tipo).first { it !is Resource.Loading }
            if (numberResult !is Resource.Success) {
                emit(Resource.Error(numberResult.message ?: "Error al asignar número"))
                return@flow
            }
            
            val numero = numberResult.data ?: 1
            val comprobanteConNumero = comprobante.copy(numero = numero)

            // 2. Generar PDF
            val pdfResult = pdfService.generarPdfComprobante(comprobanteConNumero, alquiler, workspace)
            if (pdfResult !is Resource.Success) {
                emit(Resource.Error(pdfResult.message ?: "Error al generar PDF"))
                return@flow
            }

            // 3. Guardar en base de datos
            val finalComprobante = comprobanteConNumero.copy(pdfUrl = pdfResult.data.toString())
            repository.saveComprobante(finalComprobante).collect { result ->
                emit(result)
            }
        } catch (e: Exception) {
            emit(Resource.Error("Fallo técnico: ${e.message}"))
        }
    }
}
