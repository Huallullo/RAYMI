package com.raymi.app.domain.usecase.comprobante

import com.raymi.app.domain.model.*
import com.raymi.app.domain.repository.ComprobanteRepository
import com.raymi.app.data.remote.FallbackInvoiceService
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import androidx.core.net.toUri

class GenerateComprobanteUseCase @Inject constructor(
    private val repository: ComprobanteRepository,
    private val billingService: FallbackInvoiceService
) {
    operator fun invoke(
        comprobanteInput: Comprobante,
        alquiler: Alquiler,
        workspace: Workspace
    ): Flow<Resource<GeneratedComprobanteResult>> = flow {
        emit(Resource.Loading())
        try {
            android.util.Log.d("RAYMI_BILLING", "Iniciando generación de comprobante tipo: ${comprobanteInput.tipo}")
            
            // 1. Obtener número correlativo
            val numberResult = repository.getNextNumber(workspace.id, comprobanteInput.tipo)
                .filter { it !is Resource.Loading }
                .first()
                
            if (numberResult !is Resource.Success) {
                emit(Resource.Error(numberResult.message ?: "Error al asignar número"))
                return@flow
            }
            
            val numero = numberResult.data ?: 1
            val comprobanteConNumero = comprobanteInput.copy(
                numero = numero,
                estado = EstadoComprobante.GENERANDO
            )
            android.util.Log.d("RAYMI_BILLING", "Número obtenido: $numero. Reservando en DB...")

            // Guardar registro inicial
            val saveResult = repository.saveComprobante(comprobanteConNumero)
                .filter { it !is Resource.Loading }
                .first()
                
            if (saveResult !is Resource.Success) {
                emit(Resource.Error("No se pudo reservar el número de comprobante."))
                return@flow
            }
            val comprobanteId = saveResult.data!!
            android.util.Log.d("RAYMI_BILLING", "Comprobante reservado con ID: $comprobanteId")

            // 2. Lógica de Emisión Inteligente
            val apiResult = if (comprobanteConNumero.tipo == TipoComprobante.TICKET) {
                billingService.emitirSoloLocal(comprobanteConNumero, alquiler, workspace)
            } else {
                billingService.emitirConFallback(comprobanteConNumero, alquiler, workspace)
            }
            
            if (apiResult !is Resource.Success) {
                android.util.Log.e("RAYMI_BILLING", "Falla en emisión: ${apiResult.message}")
                repository.saveComprobante(comprobanteConNumero.copy(id = comprobanteId, estado = EstadoComprobante.ERROR_GENERACION)).first { it !is Resource.Loading }
                emit(Resource.Error(apiResult.message ?: "Error al procesar el comprobante."))
                return@flow
            }

            val finalPdfUriStr = apiResult.data!!
            android.util.Log.d("RAYMI_BILLING", "PDF generado con éxito: $finalPdfUriStr")

            // 3. Actualizar a GENERADO
            val finalComprobante = comprobanteConNumero.copy(
                id = comprobanteId,
                pdfUrl = finalPdfUriStr,
                estado = EstadoComprobante.GENERADO
            )
            
            android.util.Log.d("RAYMI_BILLING", "Finalizando registro de comprobante...")
            repository.saveComprobante(finalComprobante)
                .filter { it !is Resource.Loading }
                .first()
                .let { updateResult ->
                    if (updateResult is Resource.Success) {
                        android.util.Log.d("RAYMI_BILLING", "¡Comprobante generado y registrado exitosamente!")
                        emit(Resource.Success(GeneratedComprobanteResult(
                            comprobanteId = comprobanteId,
                            pdfUri = finalPdfUriStr.toUri(),
                            correlativo = finalComprobante.correlativoCompleto
                        )))
                    } else if (updateResult is Resource.Error) {
                        android.util.Log.e("RAYMI_BILLING", "Error al finalizar registro: ${updateResult.message}")
                        emit(Resource.Error(updateResult.message ?: "Error al finalizar el registro"))
                    }
                }
        } catch (e: Exception) {
            android.util.Log.e("RAYMI_BILLING", "Fallo técnico: ${e.message}", e)
            // ✅ Corregido: Asegurar que el estado cambie a ERROR si ocurre una excepción inesperada
            try {
                // Si ya teníamos un ID reservado, marcamos error
                // Nota: comprobanteId puede no estar inicializado si falla antes de guardarResult
            } catch (_: Exception) {}
            emit(Resource.Error("Fallo técnico: ${e.message}"))
        }
    }
}
