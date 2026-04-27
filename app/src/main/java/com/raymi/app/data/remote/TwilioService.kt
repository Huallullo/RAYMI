package com.raymi.app.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.raymi.app.domain.model.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Servicio para envío de mensajes WhatsApp/SMS usando intents de Android (gratuito)
 */
@Singleton
class TwilioService @Inject constructor(
    private val context: Context
) {

    /**
     * Envía un mensaje de WhatsApp usando intent
     */
    suspend fun enviarWhatsApp(to: String, message: String): Resource<String> {
        return withContext(Dispatchers.Main) {
            try {
                // Lista de paquetes de WhatsApp posibles
                val whatsappPackages = listOf(
                    "com.whatsapp",
                    "com.whatsapp.w4b",
                    "com.whatsapp.w4b.alpha",
                    "com.whatsapp.w4b.beta"
                )

                // Buscar el primer paquete de WhatsApp disponible
                var selectedPackage: String? = null
                for (packageName in whatsappPackages) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, message)
                        setPackage(packageName)
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        selectedPackage = packageName
                        break
                    }
                }

                if (selectedPackage != null) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, message)
                        setPackage(selectedPackage)
                    }
                    context.startActivity(intent)
                    Resource.Success("Mensaje preparado para envío en WhatsApp")
                } else {
                    Resource.Error("WhatsApp no está instalado en el dispositivo")
                }
            } catch (e: Exception) {
                Resource.Error("Error al preparar mensaje para WhatsApp: ${e.message}")
            }
        }
    }

    /**
     * Envía un mensaje SMS usando intent
     */
    suspend fun enviarSMS(to: String, message: String): Resource<String> {
        return withContext(Dispatchers.Main) {
            try {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = android.net.Uri.parse("smsto:$to")
                    putExtra("sms_body", message)
                }

                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    Resource.Success("Mensaje SMS preparado para envío")
                } else {
                    Resource.Error("No hay aplicación de SMS disponible")
                }
            } catch (e: Exception) {
                Resource.Error("Error al preparar mensaje SMS: ${e.message}")
            }
        }
    }

    /**
     * Envía recordatorio de alquiler vencido
     */
    suspend fun enviarRecordatorioVencido(
        telefonoCliente: String,
        nombreCliente: String,
        vestuarioNombre: String,
        diasVencido: Int
    ): Resource<String> {
        val mensaje = """
            Hola $nombreCliente,

            Le recordamos que el alquiler del vestuario "$vestuarioNombre" está vencido por $diasVencido días.

            Por favor, devuelva el vestuario lo antes posible para evitar cargos adicionales.

            Atentamente,
            RAYMI - Alquiler de Vestuarios
        """.trimIndent()

        // Intentar WhatsApp primero, si falla usar SMS
        val whatsappResult = enviarWhatsApp(telefonoCliente, mensaje)
        return if (whatsappResult is Resource.Success) {
            whatsappResult
        } else {
            enviarSMS(telefonoCliente, mensaje)
        }
    }

    /**
     * Comparte archivo PDF por WhatsApp
     */
    suspend fun compartirPdfPorWhatsApp(pdfUri: Uri, mensaje: String = ""): Resource<String> {
        return withContext(Dispatchers.Main) {
            try {
                // Lista de paquetes de WhatsApp posibles
                val whatsappPackages = listOf(
                    "com.whatsapp",
                    "com.whatsapp.w4b",
                    "com.whatsapp.w4b.alpha",
                    "com.whatsapp.w4b.beta"
                )

                // Buscar el primer paquete de WhatsApp disponible
                var selectedPackage: String? = null
                for (packageName in whatsappPackages) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, pdfUri)
                        putExtra(Intent.EXTRA_TEXT, mensaje)
                        setPackage(packageName)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        selectedPackage = packageName
                        break
                    }
                }

                if (selectedPackage != null) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, pdfUri)
                        putExtra(Intent.EXTRA_TEXT, mensaje)
                        setPackage(selectedPackage)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    Resource.Success("PDF preparado para compartir en WhatsApp")
                } else {
                    Resource.Error("WhatsApp no está instalado")
                }
            } catch (e: Exception) {
                Resource.Error("Error al compartir PDF: ${e.message}")
            }
        }
    }
}
