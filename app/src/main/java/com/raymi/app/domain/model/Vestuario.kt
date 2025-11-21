package com.raymi.app.domain.model

import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.util.Locale

data class Vestuario(
    val id: String = "",
    val codigo: String = "",
    val danza: String = "",
    val departamento: String = "",
    val descripcion: String = "",
    val talla: String = "",
    val precio: Double = 0.0,
    val estado: EstadoVestuario = EstadoVestuario.DISPONIBLE,
    val imagenUrl: String = "",
    val createdAt: Timestamp = Timestamp.now()
) {
    val precioFormateado: String
        get() = NumberFormat.getCurrencyInstance(Locale("es", "PE")).format(precio)
}

enum class EstadoVestuario {
    DISPONIBLE,
    ALQUILADO,
    MANTENIMIENTO,
    NO_DISPONIBLE
}