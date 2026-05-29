package com.raymi.app.data.model.dto

import com.google.firebase.Timestamp
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.AlquilerItem
import com.raymi.app.domain.model.EstadoAlquiler

/**
 * DTO para Alquileres.
 */
data class AlquilerDto(
    val id: String = "",
    val workspaceId: String = "",
    val clienteId: String = "",
    val clienteNombre: String = "",
    val clienteDni: String = "",
    val clienteTelefono: String = "",
    val itemId: String = "",
    val itemNombre: String = "",
    val itemCodigo: String = "",
    val cantidad: Int = 1,
    val items: List<Map<String, Any>> = emptyList(),
    val fechaInicio: Timestamp = Timestamp.now(),
    val fechaFinPrevista: Timestamp = Timestamp.now(),
    val fechaDevolucion: Timestamp? = null,
    val precioUnitario: Double = 0.0,
    val precioTotal: Double = 0.0,
    val adelanto: Double = 0.0,
    val saldo: Double = 0.0,
    val garantia: Double = 0.0,
    val penalidad: Double = 0.0,
    val estado: String = "ACTIVO",
    val observaciones: String = "",
    val garantiaDevuelta: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    fun toDomain(): Alquiler = Alquiler(
        id = id,
        workspaceId = workspaceId,
        clienteId = clienteId,
        clienteNombre = clienteNombre,
        clienteDni = clienteDni,
        clienteTelefono = clienteTelefono,
        itemId = itemId,
        itemNombre = itemNombre,
        itemCodigo = itemCodigo,
        cantidad = cantidad,
        items = items.map { 
            AlquilerItem(
                itemId = it["itemId"] as? String ?: "",
                itemNombre = it["itemNombre"] as? String ?: "",
                itemCodigo = it["itemCodigo"] as? String ?: "",
                cantidad = (it["cantidad"] as? Number)?.toInt() ?: 0,
                precioUnitario = (it["precioUnitario"] as? Number)?.toDouble() ?: 0.0,
                subtotal = (it["subtotal"] as? Number)?.toDouble() ?: 0.0
            )
        },
        fechaInicio = fechaInicio,
        fechaFinPrevista = fechaFinPrevista,
        fechaDevolucion = fechaDevolucion,
        precioUnitario = precioUnitario,
        precioTotal = precioTotal,
        adelanto = adelanto,
        saldo = saldo,
        garantia = garantia,
        penalidad = penalidad,
        estado = try { EstadoAlquiler.valueOf(estado) } catch (_: Exception) { EstadoAlquiler.ACTIVO },
        observaciones = observaciones,
        garantiaDevuelta = garantiaDevuelta,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(domain: Alquiler): AlquilerDto = AlquilerDto(
            id = domain.id,
            workspaceId = domain.workspaceId,
            clienteId = domain.clienteId,
            clienteNombre = domain.clienteNombre,
            clienteDni = domain.clienteDni,
            clienteTelefono = domain.clienteTelefono,
            itemId = domain.itemId,
            itemNombre = domain.itemNombre,
            itemCodigo = domain.itemCodigo,
            cantidad = domain.cantidad,
            items = domain.items.map { 
                mapOf(
                    "itemId" to it.itemId,
                    "itemNombre" to it.itemNombre,
                    "itemCodigo" to it.itemCodigo,
                    "cantidad" to it.cantidad,
                    "precioUnitario" to it.precioUnitario,
                    "subtotal" to it.subtotal
                )
            },
            fechaInicio = domain.fechaInicio,
            fechaFinPrevista = domain.fechaFinPrevista,
            fechaDevolucion = domain.fechaDevolucion,
            precioUnitario = domain.precioUnitario,
            precioTotal = domain.precioTotal,
            adelanto = domain.adelanto,
            saldo = domain.saldo,
            garantia = domain.garantia,
            penalidad = domain.penalidad,
            estado = domain.estado.name,
            observaciones = domain.observaciones,
            garantiaDevuelta = domain.garantiaDevuelta,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )

        fun fromMap(id: String, map: Map<String, Any>): AlquilerDto = AlquilerDto(
            id = id,
            workspaceId = (map["workspaceId"] as? String) ?: (map["negocioId"] as? String) ?: "",
            clienteId = map["clienteId"] as? String ?: "",
            clienteNombre = map["clienteNombre"] as? String ?: "",
            clienteDni = map["clienteDni"] as? String ?: "",
            clienteTelefono = map["clienteTelefono"] as? String ?: "",
            itemId = (map["itemId"] as? String) ?: (map["vestuarioId"] as? String) ?: "",
            itemNombre = (map["itemNombre"] as? String) ?: (map["vestuarioNombre"] as? String) ?: "",
            itemCodigo = (map["itemCodigo"] as? String) ?: (map["vestuarioCodigo"] as? String) ?: "",
            cantidad = (map["cantidad"] as? Number)?.toInt() ?: 1,
            items = (map["items"] as? List<Map<String, Any>>) ?: emptyList(),
            fechaInicio = map["fechaInicio"] as? Timestamp ?: Timestamp.now(),
            fechaFinPrevista = map["fechaFinPrevista"] as? Timestamp ?: Timestamp.now(),
            fechaDevolucion = map["fechaDevolucion"] as? Timestamp,
            precioUnitario = (map["precioUnitario"] as? Number)?.toDouble() ?: 0.0,
            precioTotal = (map["precioTotal"] as? Number)?.toDouble() ?: 0.0,
            adelanto = (map["adelanto"] as? Number)?.toDouble() ?: 0.0,
            saldo = (map["saldo"] as? Number)?.toDouble() ?: 0.0,
            garantia = (map["garantia"] as? Number)?.toDouble() ?: 0.0,
            penalidad = (map["penalidad"] as? Number)?.toDouble() ?: 0.0,
            estado = map["estado"] as? String ?: "ACTIVO",
            observaciones = map["observaciones"] as? String ?: "",
            garantiaDevuelta = map["garantiaDevuelta"] as? Boolean ?: false,
            createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now(),
            updatedAt = map["updatedAt"] as? Timestamp ?: Timestamp.now()
        )
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "workspaceId" to workspaceId,
        "negocioId" to workspaceId,
        "clienteId" to clienteId,
        "clienteNombre" to clienteNombre,
        "clienteDni" to clienteDni,
        "clienteTelefono" to clienteTelefono,
        "itemId" to itemId,
        "itemNombre" to itemNombre,
        "itemCodigo" to itemCodigo,
        "cantidad" to cantidad,
        "items" to items,
        "fechaInicio" to fechaInicio,
        "fechaFinPrevista" to fechaFinPrevista,
        "fechaDevolucion" to fechaDevolucion,
        "precioUnitario" to precioUnitario,
        "precioTotal" to precioTotal,
        "adelanto" to adelanto,
        "saldo" to saldo,
        "garantia" to garantia,
        "penalidad" to penalidad,
        "estado" to estado,
        "observaciones" to observaciones,
        "garantiaDevuelta" to garantiaDevuelta,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )
}
