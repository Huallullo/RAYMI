package com.raymi.app.core.utils

/**
 * Constantes globales de la aplicación
 */
object Constants {

    // ========== COLECCIONES FIREBASE ==========
    const val COLLECTION_CLIENTES = "clientes"
    const val COLLECTION_VESTUARIOS = "vestuarios"
    const val COLLECTION_ALQUILERES = "alquileres"

    // ========== VALIDACIONES ==========
    const val DNI_LENGTH = 8
    const val MIN_PASSWORD_LENGTH = 6
    const val MIN_PHONE_LENGTH = 9

    // ========== ESTADOS VESTUARIO ==========
    const val ESTADO_DISPONIBLE = "DISPONIBLE"
    const val ESTADO_ALQUILADO = "ALQUILADO"
    const val ESTADO_MANTENIMIENTO = "MANTENIMIENTO"
    const val ESTADO_NO_DISPONIBLE = "NO_DISPONIBLE"

    // ========== ESTADOS ALQUILER ==========
    const val ESTADO_ACTIVO = "ACTIVO"
    const val ESTADO_DEVUELTO = "DEVUELTO"
    const val ESTADO_VENCIDO = "VENCIDO"
    const val ESTADO_CANCELADO = "CANCELADO"

    // ========== DEPARTAMENTOS DEL PERÚ ==========
    val DEPARTAMENTOS_PERU = listOf(
        "Amazonas", "Áncash", "Apurímac", "Arequipa", "Ayacucho",
        "Cajamarca", "Callao", "Cusco", "Huancavelica", "Huánuco",
        "Ica", "Junín", "La Libertad", "Lambayeque", "Lima",
        "Loreto", "Madre de Dios", "Moquegua", "Pasco", "Piura",
        "Puno", "San Martín", "Tacna", "Tumbes", "Ucayali"
    )

    // ========== TALLAS ==========
    val TALLAS = listOf(
        "XS", "S", "M", "L", "XL", "XXL",
        "Niño 2-4", "Niño 4-6", "Niño 6-8", "Niño 8-10",
        "Único"
    )

    // ========== FORMATO DE FECHA ==========
    const val DATE_FORMAT = "dd/MM/yyyy"
    const val DATE_TIME_FORMAT = "dd/MM/yyyy HH:mm"

    // ========== MONEDA ==========
    const val CURRENCY_SYMBOL = "S/."
    const val CURRENCY_CODE = "PEN"

    // ========== MENSAJES ==========
    object Messages {
        const val ERROR_NETWORK = "Error de conexión. Verifica tu internet"
        const val ERROR_GENERIC = "Ocurrió un error inesperado"
        const val SUCCESS_SAVE = "Guardado correctamente"
        const val SUCCESS_UPDATE = "Actualizado correctamente"
        const val SUCCESS_DELETE = "Eliminado correctamente"
        const val CONFIRM_DELETE = "¿Estás seguro de eliminar?"
    }

    // ========== LÍMITES ==========
    const val MAX_SEARCH_RESULTS = 50
    const val DEBOUNCE_TIME = 300L // milisegundos
}