package com.raymi.app.data.remote

import com.google.firebase.storage.FirebaseStorage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fuente de datos para la gestión de archivos en Firebase Storage.
 */
@Singleton
class StorageDataSource @Inject constructor(
    @Suppress("unused") private val storage: FirebaseStorage
) {
    // Funciones de subida reservadas para futura implementación de fotos de items
}
