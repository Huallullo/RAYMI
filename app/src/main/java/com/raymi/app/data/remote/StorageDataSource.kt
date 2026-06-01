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
    private val imageOptimizer: ImageOptimizer
) {
    /**
     * Sube un archivo a una ruta específica.
     * Si es una imagen, la comprime automáticamente antes de subirla.
     */
    suspend fun uploadFile(path: String, uri: Uri): String {
        val ref = storage.reference.child(path)
        
        // Optimización: Comprimir imagen antes de subir
        val optimizedImage = imageOptimizer.optimizeImage(uri)
        
        return if (optimizedImage != null) {
            // Subir ByteArray optimizado
            ref.putBytes(optimizedImage).await()
            ref.downloadUrl.await().toString()
        } else {
            // Fallback: Subir archivo original si falla la optimización
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        }
    }

    /**
     * Elimina un archivo en una ruta específica.
     */
    suspend fun deleteFile(path: String) {
        try {
            storage.reference.child(path).delete().await()
        } catch (_: Exception) {
            // Ignorar si el archivo no existe (evita crashes al borrar)
        }
    }

    /**
     * Obtiene el nombre del archivo desde una URL de Storage para poder borrarlo.
     */
    fun getPathFromUrl(url: String): String? {
        return try {
            val decodedUrl = java.net.URLDecoder.decode(url, "UTF-8")
            decodedUrl.substringAfter("/o/").substringBefore("?alt=media")
        } catch (_: Exception) {
            null
        }
    }
}
