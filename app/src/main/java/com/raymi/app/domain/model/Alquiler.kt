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
 * Soporta estados profesionales, garantias y penalidades.
 */
data class Alquiler(
    val id: String = "",
    val workspaceId: String = "",
    val clienteId: String = "",
    val clienteNombre: String = "",
    val clienteDni: String = "",
    val clienteTelefono: String = "",
    val clienteEmail: String = "",
    val itemId: String = "",
    val itemNombre: String = "",
    val itemCodigo: String = "",
    val cantidad: Int = 1,
    val fechaInicio: Timestamp = Timestamp.now(),
    val fechaFinPrevista: Timestamp = Timestamp.now(),
    val fechaDevolucion: Timestamp? = null,
    val precioUnitario: Double = 0.0,
    val precioTotal: Double = 0.0,
    val adelanto: Double = 0.0,
    val saldo: Double = 0.0,
    val garantia: Double = 0.0,            // Depósito de seguridad
    val penalidad: Double = 0.0,           // Por retraso o daño
    val estado: EstadoAlquiler = EstadoAlquiler.ACTIVO,
    val observaciones: String = "",
    val boletaId: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    val totalConPenalidad: Double get() = precioTotal + penalidad

    val saldoPendienteReal: Double get() = (totalConPenalidad - adelanto).coerceAtLeast(0.0)

    val diasAlquilados: Int
        get() {
            val fin = fechaDevolucion ?: Timestamp.now()
            val diffMillis = fin.toDate().time - fechaInicio.toDate().time
            return (TimeUnit.MILLISECONDS.toDays(diffMillis) + 1).toInt()
        }

    val diasRestantes: Int
        get() {
            if (estado != EstadoAlquiler.ACTIVO && estado != EstadoAlquiler.RESERVADO) return 0
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
        get() = (estado == EstadoAlquiler.ACTIVO || estado == EstadoAlquiler.RESERVADO) && diasRestantes < 0

    val fechaInicioFormatted: String get() = formatDate(fechaInicio.toDate())
    val fechaFinFormatted: String get() = formatDate(fechaFinPrevista.toDate())
    
    val precioFormateado: String get() = formatCurrency(precioTotal)
    val adelantoFormateado: String get() = formatCurrency(adelanto)
    val saldoFormateado: String get() = formatCurrency(saldoPendienteReal)
    val garantiaFormateada: String get() = formatCurrency(garantia)
    val penalidadFormateada: String get() = formatCurrency(penalidad)

    private fun formatDate(date: Date): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)

    private fun formatCurrency(amount: Double): String =
        NumberFormat.getCurrencyInstance(Locale("es", "PE")).format(amount)
}

enum class EstadoAlquiler {
    RESERVADO,   // El item está separado pero el alquiler no ha iniciado
    ACTIVO,      // El cliente tiene el item
    VENCIDO,     // Pasó la fecha de devolución y sigue activo
    DEVUELTO,    // Item reintegrado al stock
    CANCELADO    // Alquiler anulado antes o durante
}
