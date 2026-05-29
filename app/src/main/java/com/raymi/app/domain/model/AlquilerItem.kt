package com.raymi.app.domain.model

/**
 * Representa un producto individual dentro de un contrato de alquiler.
 */
data class AlquilerItem(
    val itemId: String,
    val itemNombre: String,
    val itemCodigo: String,
    val cantidad: Int,
    val precioUnitario: Double,
    val subtotal: Double
)
