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
            // 1. Obtener número correlativo atómico y guardar registro inicial (GENERANDO)
            val numberResult = repository.getNextNumber(workspace.id, comprobanteInput.tipo).first { it !is Resource.Loading }
            if (numberResult !is Resource.Success) {
                emit(Resource.Error(numberResult.message ?: "Error al asignar número"))
                return@flow
            }
            
            val numero = numberResult.data ?: 1
            val comprobanteConNumero = comprobanteInput.copy(
                numero = numero,
                estado = EstadoComprobante.GENERANDO
            )

            // Guardar registro inicial para "reservar" el número en DB
            val saveResult = repository.saveComprobante(comprobanteConNumero).first { it !is Resource.Loading }
            if (saveResult !is Resource.Success) {
                emit(Resource.Error("No se pudo reservar el número de comprobante."))
                return@flow
            }
            val comprobanteId = saveResult.data!!

            // 2. Lógica de Emisión Inteligente
            val apiResult = if (comprobanteConNumero.tipo == TipoComprobante.TICKET) {
                // LOS TICKETS SON GRATIS: Usamos generación local directamente
                billingService.emitirSoloLocal(comprobanteConNumero, alquiler, workspace)
            } else {
                // Boletas y Facturas: Usan la cascada de APIs con Fallback (Nubefact -> ApiPeru -> MiApi)
                billingService.emitirConFallback(comprobanteConNumero, alquiler, workspace)
            }
            
            if (apiResult !is Resource.Success) {
                // Si fallan todos los providers, marcamos error en DB
                repository.saveComprobante(comprobanteConNumero.copy(id = comprobanteId, estado = EstadoComprobante.ERROR_GENERACION)).first { it !is Resource.Loading }
                emit(Resource.Error(apiResult.message ?: "Error al procesar el comprobante electrónico."))
                return@flow
            }

            val finalPdfUriStr = apiResult.data!!

            // 3. Actualizar a GENERADO con el URI final
            val finalComprobante = comprobanteConNumero.copy(
                id = comprobanteId,
                pdfUrl = finalPdfUriStr,
                estado = EstadoComprobante.GENERADO
            )
            
            repository.saveComprobante(finalComprobante).collect { updateResult ->
                if (updateResult is Resource.Success) {
                    emit(Resource.Success(GeneratedComprobanteResult(
                        comprobanteId = comprobanteId,
                        pdfUri = finalPdfUriStr.toUri(),
                        correlativo = finalComprobante.correlativoCompleto
                    )))
                } else if (updateResult is Resource.Error) {
                    emit(Resource.Error(updateResult.message ?: "Error al finalizar el registro del comprobante"))
                }
            }
        } catch (e: Exception) {
            emit(Resource.Error("Fallo técnico crítico: ${e.message}"))
        }
    }
}
