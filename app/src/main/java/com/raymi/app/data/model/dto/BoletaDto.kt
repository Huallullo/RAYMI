package com.raymi.app.data.model.dto

import com.google.firebase.Timestamp
import com.raymi.app.domain.model.Boleta
import com.raymi.app.domain.model.BoletaItem
import com.raymi.app.domain.model.EstadoBoleta

data class BoletaDto(
    val id: String = "",
    val alquilerId: String = "",
    val workspaceId: String = "",
    val clienteId: String = "",
    val clienteNombre: String = "",
    val clienteDni: String = "",
    val items: List<BoletaItemDto> = emptyList(),
    val total: Double = 0.0,
    val estado: String = "BORRADOR",
    val numeroBoleta: Int = 0,
    val serieNumeracion: String = "B001",
    val fechaEmision: Timestamp = Timestamp.now()
) {
    fun toDomain(): Boleta = Boleta(
        id = id,
        alquilerId = alquilerId,
        workspaceId = workspaceId,
        clienteId = clienteId,
        clienteNombre = clienteNombre,
        clienteDni = clienteDni,
        items = items.map { it.toDomain() },
        total = total,
        estado = try { EstadoBoleta.valueOf(estado) } catch (_: Exception) { EstadoBoleta.BORRADOR },
        numeroBoleta = numeroBoleta,
        serieNumeracion = serieNumeracion,
        fechaEmision = fechaEmision
    )
}

data class BoletaItemDto(
    val itemNombre: String = "",
    val itemCodigo: String = "",
    val cantidad: Int = 1,
    val subtotal: Double = 0.0
) {
    fun toDomain(): BoletaItem = BoletaItem(
        itemNombre = itemNombre,
        itemCodigo = itemCodigo,
        cantidad = cantidad,
        subtotal = subtotal
    )
}
