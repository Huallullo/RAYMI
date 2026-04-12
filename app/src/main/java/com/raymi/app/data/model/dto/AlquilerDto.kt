package com.raymi.app.data.model.dto

import com.google.firebase.Timestamp
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.EstadoAlquiler

data class AlquilerDto(
    val id: String = "",
    val clienteId: String = "",
    val clienteNombre: String = "",
    val vestuarioId: String = "",
    val vestuarioNombre: String = "",
    val vestuarioCodigo: String = "",
    val cantidad: Int = 1,  // ✅ NUEVO
    val fechaInicio: Timestamp = Timestamp.now(),
    val fechaFinPrevista: Timestamp = Timestamp.now(),
    val fechaDevolucion: Timestamp? = null,
    val precioUnitario: Double = 0.0,  // ✅ NUEVO
    val precioTotal: Double = 0.0,
    val adelanto: Double = 0.0,
    val saldo: Double = 0.0,
    val estado: String = "ACTIVO",
    val observaciones: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    fun toDomain(): Alquiler {
        return Alquiler(
            id = id,
            clienteId = clienteId,
            clienteNombre = clienteNombre,
            vestuarioId = vestuarioId,
            vestuarioNombre = vestuarioNombre,
            vestuarioCodigo = vestuarioCodigo,
            cantidad = cantidad,  // ✅
            fechaInicio = fechaInicio,
            fechaFinPrevista = fechaFinPrevista,
            fechaDevolucion = fechaDevolucion,
            precioUnitario = precioUnitario,  // ✅
            precioTotal = precioTotal,
            adelanto = adelanto,
            saldo = saldo,
            estado = EstadoAlquiler.valueOf(estado),
            observaciones = observaciones,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(alquiler: Alquiler): AlquilerDto {
            return AlquilerDto(
                id = alquiler.id,
                clienteId = alquiler.clienteId,
                clienteNombre = alquiler.clienteNombre,
                vestuarioId = alquiler.vestuarioId,
                vestuarioNombre = alquiler.vestuarioNombre,
                vestuarioCodigo = alquiler.vestuarioCodigo,
                cantidad = alquiler.cantidad,  // ✅
                fechaInicio = alquiler.fechaInicio,
                fechaFinPrevista = alquiler.fechaFinPrevista,
                fechaDevolucion = alquiler.fechaDevolucion,
                precioUnitario = alquiler.precioUnitario,  // ✅
                precioTotal = alquiler.precioTotal,
                adelanto = alquiler.adelanto,
                saldo = alquiler.saldo,
                estado = alquiler.estado.name,
                observaciones = alquiler.observaciones,
                createdAt = alquiler.createdAt,
                updatedAt = alquiler.updatedAt
            )
        }

        fun fromMap(id: String, map: Map<String, Any>): AlquilerDto {
            return AlquilerDto(
                id = id,
                clienteId = map["clienteId"] as? String ?: "",
                clienteNombre = map["clienteNombre"] as? String ?: "",
                vestuarioId = map["vestuarioId"] as? String ?: "",
                vestuarioNombre = map["vestuarioNombre"] as? String ?: "",
                vestuarioCodigo = map["vestuarioCodigo"] as? String ?: "",
                cantidad = (map["cantidad"] as? Number)?.toInt() ?: 1,  // ✅
                fechaInicio = map["fechaInicio"] as? Timestamp ?: Timestamp.now(),
                fechaFinPrevista = map["fechaFinPrevista"] as? Timestamp ?: Timestamp.now(),
                fechaDevolucion = map["fechaDevolucion"] as? Timestamp,
                precioUnitario = (map["precioUnitario"] as? Number)?.toDouble() ?: 0.0,  // ✅
                precioTotal = (map["precioTotal"] as? Number)?.toDouble() ?: 0.0,
                adelanto = (map["adelanto"] as? Number)?.toDouble() ?: 0.0,
                saldo = (map["saldo"] as? Number)?.toDouble() ?: 0.0,
                estado = map["estado"] as? String ?: "ACTIVO",
                observaciones = map["observaciones"] as? String ?: "",
                createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now(),
                updatedAt = map["updatedAt"] as? Timestamp ?: Timestamp.now()
            )
        }
    }

    fun toMap(): Map<String, Any> {
        val map = hashMapOf(
            "clienteId" to clienteId,
            "clienteNombre" to clienteNombre,
            "vestuarioId" to vestuarioId,
            "vestuarioNombre" to vestuarioNombre,
            "vestuarioCodigo" to vestuarioCodigo,
            "cantidad" to cantidad,  // ✅
            "fechaInicio" to fechaInicio,
            "fechaFinPrevista" to fechaFinPrevista,
            "precioUnitario" to precioUnitario,  // ✅
            "precioTotal" to precioTotal,
            "adelanto" to adelanto,
            "saldo" to saldo,
            "estado" to estado,
            "observaciones" to observaciones,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )

        fechaDevolucion?.let {
            map["fechaDevolucion"] = it
        }

        return map
    }
}