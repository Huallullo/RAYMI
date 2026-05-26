package com.raymi.app.data.remote

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fuente de datos para la gestión de archivos en Firebase Storage.
 * Implementa aislamiento por negocio (SaaS).
 */
@Singleton
class StorageDataSource @Inject constructor(
    private val storage: FirebaseStorage
) {
    /**
     * Sube una imagen de producto a una ruta privada del negocio.
     * Ruta: negocios/{workspaceId}/items/{itemId}/{filename}
     */
    suspend fun uploadItemImage(
        workspaceId: String,
        itemId: String,
        imageUri: Uri,
        filename: String = "main.jpg"
    ): String {
        val path = "negocios/$workspaceId/items/$itemId/$filename"
        val ref = storage.reference.child(path)
        
        ref.putFile(imageUri).await()
        return ref.downloadUrl.await().toString()
    }

    /**
     * Sube el logo del negocio.
     * Ruta: negocios/{workspaceId}/branding/logo.jpg
     */
    suspend fun uploadBusinessLogo(workspaceId: String, imageUri: Uri): String {
        val path = "negocios/$workspaceId/branding/logo.jpg"
        val ref = storage.reference.child(path)
        
        ref.putFile(imageUri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun deleteFile(path: String) {
        storage.reference.child(path).delete().await()
    }
}
