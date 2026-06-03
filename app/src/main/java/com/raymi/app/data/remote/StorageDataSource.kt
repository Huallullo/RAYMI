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
     * Si es una imagen, la comprime automáticamente antes de subirla.
     */
    suspend fun uploadFile(path: String, uri: Uri): String {
        // [B-05] Verificar tamaño antes de procesar
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            if (pfd.statSize > 10 * 1024 * 1024L) { // 10MB límite
                throw IllegalArgumentException("La imagen es demasiado grande. Máximo 10MB permitido.")
            }
        }

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
     * [M-09] Uso de API oficial para mayor robustez.
     */
    fun getPathFromUrl(url: String): String? {
        return try {
            FirebaseStorage.getInstance().getReferenceFromUrl(url).path
        } catch (_: Exception) {
            null
        }
    }
}
