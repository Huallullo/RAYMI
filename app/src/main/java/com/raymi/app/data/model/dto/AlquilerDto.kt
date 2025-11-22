package com.raymi.app.data.model.dto

import com.google.firebase.Timestamp
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.EstadoAlquiler

/**
 * DTO para transferencia de datos de Alquiler con Firebase
 */
data class AlquilerDto(
    val id: String = "",
    val clienteId: String = "",
    val clienteNombre: String = "",
    val vestuarioId: String = "",
    val vestuarioNombre: String = "",
    val vestuarioCodigo: String = "",
    val fechaInicio: Timestamp = Timestamp.now(),
    val fechaFinPrevista: Timestamp = Timestamp.now(),
    val fechaDevolucion: Timestamp? = null,
    val precioTotal: Double = 0.0,
    val adelanto: Double = 0.0,
    val saldo: Double = 0.0,
    val estado: String = "ACTIVO",
    val observaciones: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    /**
     * Convierte DTO a modelo de dominio
     */
    fun toDomain(): Alquiler {
        return Alquiler(
            id = id,
            clienteId = clienteId,
            clienteNombre = clienteNombre,
            vestuarioId = vestuarioId,
            vestuarioNombre = vestuarioNombre,
            vestuarioCodigo = vestuarioCodigo,
            fechaInicio = fechaInicio,
            fechaFinPrevista = fechaFinPrevista,
            fechaDevolucion = fechaDevolucion,
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
        /**
         * Convierte modelo de dominio a DTO
         */
        fun fromDomain(alquiler: Alquiler): AlquilerDto {
            return AlquilerDto(
                id = alquiler.id,
                clienteId = alquiler.clienteId,
                clienteNombre = alquiler.clienteNombre,
                vestuarioId = alquiler.vestuarioId,
                vestuarioNombre = alquiler.vestuarioNombre,
                vestuarioCodigo = alquiler.vestuarioCodigo,
                fechaInicio = alquiler.fechaInicio,
                fechaFinPrevista = alquiler.fechaFinPrevista,
                fechaDevolucion = alquiler.fechaDevolucion,
                precioTotal = alquiler.precioTotal,
                adelanto = alquiler.adelanto,
                saldo = alquiler.saldo,
                estado = alquiler.estado.name,
                observaciones = alquiler.observaciones,
                createdAt = alquiler.createdAt,
                updatedAt = alquiler.updatedAt
            )
        }

        /**
         * Crea DTO desde un Map de Firebase
         */
        fun fromMap(id: String, map: Map<String, Any>): AlquilerDto {
            return AlquilerDto(
                id = id,
                clienteId = map["clienteId"] as? String ?: "",
                clienteNombre = map["clienteNombre"] as? String ?: "",
                vestuarioId = map["vestuarioId"] as? String ?: "",
                vestuarioNombre = map["vestuarioNombre"] as? String ?: "",
                vestuarioCodigo = map["vestuarioCodigo"] as? String ?: "",
                fechaInicio = map["fechaInicio"] as? Timestamp ?: Timestamp.now(),
                fechaFinPrevista = map["fechaFinPrevista"] as? Timestamp ?: Timestamp.now(),
                fechaDevolucion = map["fechaDevolucion"] as? Timestamp,
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

    /**
     * Convierte DTO a Map para Firebase
     */
    fun toMap(): Map<String, Any> {
        val map = hashMapOf(
            "clienteId" to clienteId,
            "clienteNombre" to clienteNombre,
            "vestuarioId" to vestuarioId,
            "vestuarioNombre" to vestuarioNombre,
            "vestuarioCodigo" to vestuarioCodigo,
            "fechaInicio" to fechaInicio,
            "fechaFinPrevista" to fechaFinPrevista,
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