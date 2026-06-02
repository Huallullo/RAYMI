package com.raymi.app.core.utils

import kotlinx.coroutines.CancellationException

/**
 * Mapeador de errores de Firebase a mensajes amigables para el usuario.
 */
object FirebaseErrorMapper {
    fun mapError(e: Exception): String = when {
        e.message?.contains("PERMISSION_DENIED") == true -> "Sin permisos para esta operación"
        e.message?.contains("UNAVAILABLE") == true -> "Sin conexión. Verifica tu internet"
        e.message?.contains("NOT_FOUND") == true -> "Registro no encontrado"
        e.message?.contains("ALREADY_EXISTS") == true -> "Este registro ya existe"
        e is CancellationException -> throw e
        else -> "Error inesperado. Intenta de nuevo"
    }
}
