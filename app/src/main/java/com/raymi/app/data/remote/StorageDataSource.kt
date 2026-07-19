package com.raymi.app.data.remote

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.raymi.app.core.utils.ImageOptimizer
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fuente de datos para la gestión de archivos en Firebase Storage.
 * Optimizado para reducir costos mediante compresión WebP y redimensionado.
 */
@Singleton
class StorageDataSource @Inject constructor(
    private val storage: FirebaseStorage,
    private val imageOptimizer: ImageOptimizer,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) {
    /**
     * Sube un archivo a una ruta específica.
     * Optimizado para Spark Plan: codifica la imagen optimizada a Base64
     * y la retorna como un data URL de tipo "data:image/webp;base64,...".
     */
    suspend fun uploadFile(path: String, uri: Uri): String {
        // [B-05] Verificar tamaño antes de procesar
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            if (pfd.statSize > 10 * 1024 * 1024L) { // 10MB límite
                throw IllegalArgumentException("La imagen es demasiado grande. Máximo 10MB permitido.")
            }
        }

        // Definir dimensiones óptimas para no sobrepasar el límite de Firestore (1MB)
        val (maxWidth, maxHeight, quality) = when {
            path.contains("logo.webp") -> Triple(200, 200, 60)
            path.contains("_face.webp") -> Triple(300, 300, 50)
            path.contains("clientes/") -> Triple(500, 500, 50)
            path.contains("items/") -> Triple(400, 400, 60)
            else -> Triple(400, 400, 60)
        }
        
        // Optimización: Comprimir imagen localmente
        val optimizedImage = imageOptimizer.optimizeImage(uri, maxWidth, maxHeight, quality)
        val bytes = optimizedImage ?: context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("No se pudo procesar la imagen")

        val base64String = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        return "data:image/webp;base64,$base64String"
    }

    /**
     * Elimina un archivo en una ruta específica.
     * No-op para Base64 en Firestore, ya que el string se elimina del documento directamente.
     */
    suspend fun deleteFile(path: String) {
        // No-op
    }

    /**
     * Obtiene el nombre del archivo desde una URL de Storage para poder borrarlo.
     * Retorna null para URLs en formato data URI Base64.
     */
    fun getPathFromUrl(url: String): String? {
        if (url.startsWith("data:")) return null
        return try {
            FirebaseStorage.getInstance().getReferenceFromUrl(url).path
        } catch (_: Exception) {
            null
        }
    }
}
