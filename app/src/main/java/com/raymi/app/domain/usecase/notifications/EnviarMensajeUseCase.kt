package com.raymi.app.domain.usecase.notifications

import com.raymi.app.data.remote.TwilioService
import com.raymi.app.domain.model.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Caso de uso para enviar mensajes WhatsApp/SMS
 */
class EnviarMensajeUseCase @Inject constructor(
    private val twilioService: TwilioService
) {
    /**
     * Envía mensaje de WhatsApp
     */
    fun enviarWhatsApp(to: String, message: String): Flow<Resource<String>> = flow {
        try {
            emit(Resource.Loading())
            val result = twilioService.enviarWhatsApp(to, message)
            emit(result)
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error desconocido"))
        }
    }

    /**
     * Envía mensaje SMS
     */
    fun enviarSMS(to: String, message: String): Flow<Resource<String>> = flow {
        try {
            emit(Resource.Loading())
            val result = twilioService.enviarSMS(to, message)
            emit(result)
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error desconocido"))
        }
    }

    /**
     * Envía recordatorio de alquiler vencido
     */
    fun enviarRecordatorioVencido(
        telefonoCliente: String,
        nombreCliente: String,
        vestuarioNombre: String,
        diasVencido: Int
    ): Flow<Resource<String>> = flow {
        try {
            emit(Resource.Loading())
            val result = twilioService.enviarRecordatorioVencido(
                telefonoCliente,
                nombreCliente,
                vestuarioNombre,
                diasVencido
            )
            emit(result)
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error desconocido"))
        }
    }

    /**
     * Comparte PDF por WhatsApp
     */
    fun compartirPdfPorWhatsApp(pdfFile: java.io.File, mensaje: String = ""): Flow<Resource<String>> = flow {
        try {
            emit(Resource.Loading())
            val result = twilioService.compartirPdfPorWhatsApp(pdfFile, mensaje)
            emit(result)
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error desconocido"))
        }
    }
}
