package com.raymi.app.domain.usecase.notifications

import android.net.Uri
import com.raymi.app.data.remote.CommunicationService
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.Workspace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Caso de Uso Maestro de Notificaciones (Estrategia de Fidelización).
 * Implementa el TICKET VIP para WhatsApp.
 */
class EnviarMensajeUseCase @Inject constructor(
    private val commsService: CommunicationService
) {
    /**
     * Envía el TICKET VIP de bienvenida y confirmación de contrato.
     * Soporta múltiples monedas y diseño optimizado para WhatsApp.
     */
    fun enviarConfirmacionAlquiler(
        alquiler: Alquiler,
        workspace: Workspace
    ): Flow<Resource<String>> = flow {
        if (alquiler.clienteTelefono.isBlank()) {
            emit(Resource.Error("El cliente no tiene teléfono registrado"))
            return@flow
        }
        
        val currency = workspace.moneda.ifBlank { "PEN" }
        val currencySymbol = if (currency == "USD") "$" else "S/."
        
        val mapsPart = if (workspace.googleMapsUrl.isNotBlank()) {
            "\n📍 *¿Cómo llegar?*\n${workspace.googleMapsUrl}\n"
        } else ""

        val itemsList = if (alquiler.items.isNotEmpty()) {
            "\n📦 *DETALLE:*\n" + alquiler.items.joinToString("\n") { "• ${it.cantidad}x ${it.itemNombre}" }
        } else "\n📦 *PRODUCTO:* ${alquiler.itemNombre}"

        val mensaje = """
            ✨ *¡CONFIRMACIÓN DE ALQUILER!* ✨
            ━━━━━━━━━━━━━━━━━━
            🏢 *NEGOCIO:* ${workspace.nombreComercial.ifBlank { workspace.nombre }}
            📝 *CLIENTE:* ${alquiler.clienteNombre}
            $itemsList
            
            📅 *ENTREGA:* ${alquiler.fechaInicioFormatted}
            🔄 *DEVOLUCIÓN:* ${alquiler.fechaFinFormatted}
            
            💰 *RESUMEN ECONÓMICO:*
            • Total: $currencySymbol ${String.format("%.2f", alquiler.precioTotal)}
            • Garantía: $currencySymbol ${String.format("%.2f", alquiler.garantia)}
            • Adelanto: $currencySymbol ${String.format("%.2f", alquiler.adelanto)}
            
            💸 *SALDO PENDIENTE:* 
            👉 *$currencySymbol ${String.format("%.2f", alquiler.saldo)}*
            ━━━━━━━━━━━━━━━━━━
            $mapsPart
            ⚠️ *Recuerda traer tu DNI original para recoger el producto.*
            
            ¡Gracias por confiar en nosotros! 🚀
        """.trimIndent()
        
        emit(Resource.Loading())
        emit(commsService.enviarWhatsApp(alquiler.clienteTelefono, mensaje))
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
