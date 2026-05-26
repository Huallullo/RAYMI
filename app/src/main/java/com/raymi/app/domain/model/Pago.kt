package com.raymi.app.domain.model

import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.util.Locale

/**
 * Representa un abono o pago realizado a un alquiler.
 */
data class Pago(
    val id: String = "",
    val alquilerId: String = "",
    val monto: Double = 0.0,
    val metodoPago: MetodoPago = MetodoPago.EFECTIVO,
    val referencia: String = "", // Número de operación, link de Yape, etc.
    val fecha: Timestamp = Timestamp.now()
) {
    val montoFormateado: String
        get() = NumberFormat.getCurrencyInstance(Locale("es", "PE")).format(monto)
}

enum class MetodoPago {
    EFECTIVO,
    YAPE,
    PLIN,
    TRANSFERENCIA,
    TARJETA
}
