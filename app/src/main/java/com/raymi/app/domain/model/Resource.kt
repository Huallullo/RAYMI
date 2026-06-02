package com.raymi.app.domain.model

/**
 * Clase sealed para manejar estados de las operaciones
 * Success: Operación exitosa con datos
 * Error: Error con mensaje
 * Loading: Estado de carga
 */
sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null,
    val cursor: Any? = null // OPTIMIZACIÓN: Soporte para cursores de paginación Firestore
) {
    class Success<T>(data: T, cursor: Any? = null) : Resource<T>(data, cursor = cursor)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
    class Loading<T>(data: T? = null) : Resource<T>(data)
}
