package com.raymi.app.domain.usecase.notifications

import android.net.Uri
import com.raymi.app.data.remote.CommunicationService
import com.raymi.app.domain.model.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Caso de Uso Maestro de Notificaciones (Estrategia de Fidelización).
 * Implementa plantillas de texto persuasivas y profesionales para el negocio.
 */
class EnviarMensajeUseCase @Inject constructor(
    private val commsService: CommunicationService
) {
    /**
     * Envía mensaje de bienvenida y confirmación de contrato.
     */
    fun enviarConfirmacionAlquiler(
        telefono: String,
        cliente: String,
        item: String,
        fechaDevolucion: String,
        monto: String,
        negocio: String
    ): Flow<Resource<String>> = flow {
        val mensaje = """
            *¡Hola $cliente!* 👋
            
            Gracias por elegir a *$negocio*. Confirmamos tu alquiler:
            
            📦 *Producto:* $item
            📅 *Fecha de Devolución:* $fechaDevolucion
            💰 *Total:* $monto
            
            ¡Estamos para servirte! Por favor, conserva este mensaje como tu ticket digital. 
        """.trimIndent()
        
        emit(Resource.Loading())
        emit(commsService.enviarWhatsApp(telefono, mensaje))
    }

    /**
     * Envía un recordatorio de devolución próxima o vencida.
     */
    fun enviarRecordatorioDevolucion(
        telefono: String,
        cliente: String,
        item: String,
        esVencido: Boolean
    ): Flow<Resource<String>> = flow {
        val saludo = if (esVencido) "⚠️ *RECORDATORIO DE DEVOLUCIÓN*" else "🔔 *AVISO DE DEVOLUCIÓN*"
        val mensaje = """
            $saludo
            
            Estimado(a) *$cliente*, te recordamos que el periodo de alquiler de tu *$item* ha finalizado o está por concluir. 
            
            Te esperamos en nuestro local para la recepción del equipo. ¡Muchas gracias!
        """.trimIndent()
        
        emit(Resource.Loading())
        emit(commsService.enviarWhatsApp(telefono, mensaje))
    }

    /**
     * Comparte el PDF oficial de forma directa.
     */
    fun compartirBoletaDigital(pdfUri: Uri, mensaje: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        emit(commsService.compartirPdfPorWhatsApp(pdfUri, mensaje))
    }
}
