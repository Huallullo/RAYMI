package com.raymi.app.data.remote

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fuente de datos para la gestión de archivos en Firebase Storage.
 */
@Singleton
class StorageDataSource @Inject constructor(
    private val storage: FirebaseStorage
) {
    /**
     * Sube un archivo a una ruta específica.
     */
    suspend fun uploadFile(path: String, uri: Uri): String {
        val ref = storage.reference.child(path)
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    /**
     * Elimina un archivo en una ruta específica.
     */
    suspend fun deleteFile(path: String) {
        storage.reference.child(path).delete().await()
    }
}
