package com.raymi.app.domain.model

import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Representa un Alquiler Genérico (SaaS).
 * Reemplaza referencias a "Vestuario" por "Item" para soportar cualquier rubro.
 */
data class Alquiler(
    val id: String = "",
    val workspaceId: String = "",              // A qué negocio pertenece
    val clienteId: String = "",
    val clienteNombre: String = "",
    val clienteDni: String = "",               // DNI para mercado PERÚ
    val clienteTelefono: String = "",          // Para notificaciones WhatsApp
    val clienteEmail: String = "",
    val itemId: String = "",                   // ID del producto (antes vestuarioId)
    val itemNombre: String = "",               // Nombre del producto alquilado
    val itemCodigo: String = "",               // SKU del producto
    val cantidad: Int = 1,
    val fechaInicio: Timestamp = Timestamp.now(),
    val fechaFinPrevista: Timestamp = Timestamp.now(),
    val fechaDevolucion: Timestamp? = null,
    val precioUnitario: Double = 0.0,
    val precioTotal: Double = 0.0,
    val adelanto: Double = 0.0,
    val saldo: Double = 0.0,
    val estado: EstadoAlquiler = EstadoAlquiler.ACTIVO,
    val observaciones: String = "",
    val boletaId: String? = null,              // Referencia a facturación electrónica
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    val diasAlquilados: Int
        get() {
            val fin = fechaDevolucion ?: Timestamp.now()
            val diffMillis = fin.toDate().time - fechaInicio.toDate().time
            return (TimeUnit.MILLISECONDS.toDays(diffMillis) + 1).toInt()
        }

    /**
     * Calcula cuántos días quedan para la devolución.
     */
    val diasRestantes: Int
        get() {
            if (estado != EstadoAlquiler.ACTIVO) return 0
            val hoy = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.time

            val fin = Calendar.getInstance().apply {
                time = fechaFinPrevista.toDate()
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.time

            val diffMillis = fin.time - hoy.time
            return TimeUnit.MILLISECONDS.toDays(diffMillis).toInt()
        }

    val estaVencido: Boolean
        get() = estado == EstadoAlquiler.ACTIVO && diasRestantes < 0

    val fechaInicioFormatted: String
        get() = formatDate(fechaInicio.toDate())

    val fechaFinFormatted: String
        get() = formatDate(fechaFinPrevista.toDate())

    val precioFormateado: String
        get() = NumberFormat.getCurrencyInstance(Locale("es", "PE")).format(precioTotal)

    val adelantoFormateado: String
        get() = NumberFormat.getCurrencyInstance(Locale("es", "PE")).format(adelanto)

    val saldoFormateado: String
        get() = NumberFormat.getCurrencyInstance(Locale("es", "PE")).format(saldo)

    private fun formatDate(date: Date): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
}

enum class EstadoAlquiler {
    ACTIVO,
    DEVUELTO,
    VENCIDO,
    CANCELADO
}
