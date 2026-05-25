package com.raymi.app.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.raymi.app.domain.model.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Servicio de Comunicaciones Premium (SaaS).
 * Permite el envío gratuito de mensajes por WhatsApp y SMS mediante Intents.
 * Incluye plantillas profesionales para fidelización de clientes.
 */
@Singleton
class CommunicationService @Inject constructor(
    private val context: Context
) {

    /**
     * Prepara y abre WhatsApp con un mensaje predefinido.
     */
    suspend fun enviarWhatsApp(telefono: String, mensaje: String): Resource<String> {
        return withContext(Dispatchers.Main) {
            try {
                // Limpiar el teléfono de caracteres no numéricos
                val cleanNumber = telefono.replace(Regex("[^0-9]"), "")
                // Asegurar código de país (Perú por defecto si tiene 9 dígitos)
                val finalNumber = if (cleanNumber.length == 9) "51$cleanNumber" else cleanNumber
                
                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$finalNumber&text=${Uri.encode(mensaje)}")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                
                context.startActivity(intent)
                Resource.Success("WhatsApp abierto con éxito")
            } catch (e: Exception) {
                Resource.Error("Falla al abrir WhatsApp: ${e.message}")
            }
        }
    }

    /**
     * Comparte un archivo PDF (como una boleta) por WhatsApp.
     */
    suspend fun compartirPdfPorWhatsApp(pdfUri: Uri, mensaje: String = ""): Resource<String> {
        return withContext(Dispatchers.Main) {
            try {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, pdfUri)
                    putExtra(Intent.EXTRA_TEXT, mensaje)
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Resource.Success("Comprobante enviado a WhatsApp")
            } catch (e: Exception) {
                // Si falla por paquete específico, intentar envío genérico
                try {
                    val genericIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, pdfUri)
                        putExtra(Intent.EXTRA_TEXT, mensaje)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(genericIntent)
                    Resource.Success("Enviado mediante selector de sistema")
                } catch (ex: Exception) {
                    Resource.Error("Error crítico al compartir documento")
                }
            }
        }
    }
}
