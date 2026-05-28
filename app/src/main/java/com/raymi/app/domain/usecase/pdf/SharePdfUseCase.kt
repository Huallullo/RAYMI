package com.raymi.app.domain.usecase.pdf

import android.content.Context
import android.content.Intent
import android.net.Uri
import javax.inject.Inject

class SharePdfUseCase @Inject constructor(
    private val context: Context
) {
    operator fun invoke(uri: Uri, title: String = "Comprobante RAYMI"): com.raymi.app.domain.model.Resource<Unit> {
        return try {
            if (uri.toString().isBlank()) {
                return com.raymi.app.domain.model.Resource.Error("El enlace del archivo está vacío")
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TITLE, title)
                putExtra(Intent.EXTRA_TEXT, "Hola, te comparto el comprobante de alquiler generado por RAYMI.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = Intent.createChooser(shareIntent, "Enviar por:")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            com.raymi.app.domain.model.Resource.Success(Unit)
        } catch (e: android.content.ActivityNotFoundException) {
            com.raymi.app.domain.model.Resource.Error("No se encontró ninguna aplicación para compartir PDFs.")
        } catch (e: Exception) {
            com.raymi.app.domain.model.Resource.Error("Error al intentar compartir: ${e.message}")
        }
    }
}
