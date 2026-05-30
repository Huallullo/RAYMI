package com.raymi.app.domain.usecase.pdf

import android.content.Context
import android.content.Intent
import android.net.Uri
import javax.inject.Inject

class ViewPdfUseCase @Inject constructor(
    private val context: Context
) {
    operator fun invoke(uri: Uri): com.raymi.app.domain.model.Resource<Unit> {
        return try {
            if (uri.toString().isBlank()) {
                return com.raymi.app.domain.model.Resource.Error("El enlace del archivo está vacío")
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                if (uri.scheme == "https" || uri.scheme == "http") {
                    setDataAndType(uri, "text/html") // Open URL in browser
                } else {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(intent)
            com.raymi.app.domain.model.Resource.Success(Unit)
        } catch (e: android.content.ActivityNotFoundException) {
            com.raymi.app.domain.model.Resource.Error("No se encontró una aplicación para abrir este archivo.")
        } catch (e: Exception) {
            com.raymi.app.domain.model.Resource.Error("Error al abrir: ${e.message}")
        }
    }
}
