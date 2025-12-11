package com.raymi.app.domain.model

import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Alquiler(
    val id: String = "",
    val clienteId: String = "",
    val clienteNombre: String = "",
    val vestuarioId: String = "",
    val vestuarioNombre: String = "",
    val vestuarioCodigo: String = "",
    val cantidad: Int = 1,  // ✅ NUEVO CAMPO
    val fechaInicio: Timestamp = Timestamp.now(),
    val fechaFinPrevista: Timestamp = Timestamp.now(),
    val fechaDevolucion: Timestamp? = null,
    val precioUnitario: Double = 0.0,  // ✅ NUEVO - Precio por unidad
    val precioTotal: Double = 0.0,
    val adelanto: Double = 0.0,
    val saldo: Double = 0.0,
    val estado: EstadoAlquiler = EstadoAlquiler.ACTIVO,
    val observaciones: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    val diasAlquilados: Int
        get() {
            val fin = fechaDevolucion ?: Timestamp.now()
            val diff = fin.seconds - fechaInicio.seconds
            return (diff / 86400).toInt() + 1
        }

    val diasRestantes: Int
        get() {
            if (estado != EstadoAlquiler.ACTIVO) return 0
            val diff = fechaFinPrevista.seconds - Timestamp.now().seconds
            return (diff / 86400).toInt()
        }

    val estaVencido: Boolean
        get() = estado == EstadoAlquiler.ACTIVO && diasRestantes < 0

    val fechaInicioFormatted: String
        get() = formatDate(fechaInicio.toDate())

    val fechaFinFormatted: String
        get() = formatDate(fechaFinPrevista.toDate())

    val precioFormateado: String
        get() = NumberFormat.getCurrencyInstance(Locale("es", "PE")).format(precioTotal)

    val precioUnitarioFormateado: String  // ✅ NUEVO
        get() = NumberFormat.getCurrencyInstance(Locale("es", "PE")).format(precioUnitario)

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