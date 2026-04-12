package com.raymi.app.core.utils

import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Funciones de extensión útiles para la aplicación
 */

// ========== EXTENSIONES DE STRING ==========

/**
 * Valida si un string es un DNI válido (8 dígitos)
 */
fun String.isValidDni(): Boolean {
    return this.length == Constants.DNI_LENGTH && this.all { it.isDigit() }
}

/**
 * Valida si un string es un email válido
 */
fun String.isValidEmail(): Boolean {
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    return this.matches(emailRegex.toRegex())
}

/**
 * Valida si un string es un teléfono válido (9 dígitos)
 */
fun String.isValidPhone(): Boolean {
    return this.length >= Constants.MIN_PHONE_LENGTH && this.all { it.isDigit() }
}

/**
 * Capitaliza la primera letra de cada palabra
 */
fun String.capitalizeWords(): String {
    return this.split(" ")
        .joinToString(" ") { word ->
            word.lowercase().replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
        }
}

/**
 * Obtiene las iniciales de un nombre completo
 */
fun String.getInitials(): String {
    val words = this.trim().split(" ").filter { it.isNotBlank() }
    return when {
        words.isEmpty() -> ""
        words.size == 1 -> words[0].take(2).uppercase()
        else -> "${words[0].first()}${words[1].first()}".uppercase()
    }
}

// ========== EXTENSIONES DE DOUBLE ==========

/**
 * Formatea un Double como moneda peruana
 */
fun Double.toSoles(): String {
    val format = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
    return format.format(this)
}

/**
 * Formatea un Double a 2 decimales
 */
fun Double.toDecimalString(): String {
    return String.format(Locale.getDefault(), "%.2f", this)
}

// ========== EXTENSIONES DE DATE ==========

/**
 * Formatea una Date a string con formato personalizado
 */
fun Date.formatTo(pattern: String = Constants.DATE_FORMAT): String {
    val formatter = SimpleDateFormat(pattern, Locale.getDefault())
    return formatter.format(this)
}

/**
 * Convierte Date a Timestamp de Firebase
 */
fun Date.toTimestamp(): Timestamp {
    return Timestamp(this)
}

// ========== EXTENSIONES DE TIMESTAMP ==========

/**
 * Formatea un Timestamp de Firebase a string legible
 */
fun Timestamp.formatTo(pattern: String = Constants.DATE_FORMAT): String {
    val date = this.toDate()
    val formatter = SimpleDateFormat(pattern, Locale.getDefault())
    return formatter.format(date)
}

/**
 * Obtiene la diferencia en días entre un Timestamp y ahora
 */
fun Timestamp.daysUntil(other: Timestamp = Timestamp.now()): Int {
    val diff = other.seconds - this.seconds
    return (diff / 86400).toInt()
}

/**
 * Obtiene la diferencia en días desde un Timestamp hasta ahora
 */
fun Timestamp.daysSince(): Int {
    val now = Timestamp.now()
    val diff = now.seconds - this.seconds
    return (diff / 86400).toInt()
}

/**
 * Verifica si un Timestamp está en el pasado
 */
fun Timestamp.isPast(): Boolean {
    return this.seconds < Timestamp.now().seconds
}

/**
 * Verifica si un Timestamp está en el futuro
 */
fun Timestamp.isFuture(): Boolean {
    return this.seconds > Timestamp.now().seconds
}

// ========== EXTENSIONES DE INT ==========

/**
 * Convierte días a milisegundos
 */
fun Int.daysToMillis(): Long {
    return this.toLong() * 24 * 60 * 60 * 1000
}

/**
 * Formatea un número con separadores de miles
 */
fun Int.formatWithThousands(): String {
    return String.format(Locale.getDefault(), "%,d", this)
}

// ========== EXTENSIONES DE BOOLEAN ==========

/**
 * Convierte Boolean a texto Sí/No
 */
fun Boolean.toYesNo(): String {
    return if (this) "Sí" else "No"
}