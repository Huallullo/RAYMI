package com.raymi.app.data.model.dto

import com.google.firebase.Timestamp
import com.raymi.app.domain.model.EstadoVestuario
import com.raymi.app.domain.model.Vestuario

/**
 * DTO para transferencia de datos de Vestuario con Firebase
 */
data class VestuarioDto(
    val id: String = "",
    val codigo: String = "",
    val danza: String = "",
    val departamento: String = "",
    val descripcion: String = "",
    val talla: String = "",
    val precio: Double = 0.0,
    val estado: String = "DISPONIBLE",
    val imagenUrl: String = "",
    val createdAt: Timestamp = Timestamp.now()
) {
    /**
     * Convierte DTO a modelo de dominio
     */
    fun toDomain(): Vestuario {
        return Vestuario(
            id = id,
            codigo = codigo,
            danza = danza,
            departamento = departamento,
            descripcion = descripcion,
            talla = talla,
            precio = precio,
            estado = EstadoVestuario.valueOf(estado),
            imagenUrl = imagenUrl,
            createdAt = createdAt
        )
    }

    companion object {
        /**
         * Convierte modelo de dominio a DTO
         */
        fun fromDomain(vestuario: Vestuario): VestuarioDto {
            return VestuarioDto(
                id = vestuario.id,
                codigo = vestuario.codigo,
                danza = vestuario.danza,
                departamento = vestuario.departamento,
                descripcion = vestuario.descripcion,
                talla = vestuario.talla,
                precio = vestuario.precio,
                estado = vestuario.estado.name,
                imagenUrl = vestuario.imagenUrl,
                createdAt = vestuario.createdAt
            )
        }

        /**
         * Crea DTO desde un Map de Firebase
         */
        fun fromMap(id: String, map: Map<String, Any>): VestuarioDto {
            return VestuarioDto(
                id = id,
                codigo = map["codigo"] as? String ?: "",
                danza = map["danza"] as? String ?: "",
                departamento = map["departamento"] as? String ?: "",
                descripcion = map["descripcion"] as? String ?: "",
                talla = map["talla"] as? String ?: "",
                precio = (map["precio"] as? Number)?.toDouble() ?: 0.0,
                estado = map["estado"] as? String ?: "DISPONIBLE",
                imagenUrl = map["imagenUrl"] as? String ?: "",
                createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now()
            )
        }
        private fun buildSearchTerms(vararg fields: String): List<String> {
            val baseTokens = fields.toList()
                .flatMap { it.trim().lowercase().split("\\s+".toRegex()) }
                .filter { it.isNotBlank() }

            val prefixes = mutableSetOf<String>()
            baseTokens.forEach { token ->
                val clean = token.take(30)
                for (i in 1..clean.length) {
                    prefixes.add(clean.substring(0, i))
                }
            }
            return prefixes.toList()
        }
    }

    /**
     * Convierte DTO a Map para Firebase
     */
    fun toMap(): Map<String, Any> {
        return hashMapOf(
            "codigo" to codigo,
            "danza" to danza,
            "departamento" to departamento,
            "descripcion" to descripcion,
            "talla" to talla,
            "precio" to precio,
            "estado" to estado,
            "imagenUrl" to imagenUrl,
            "createdAt" to createdAt,
            "searchTerms" to buildSearchTerms(codigo, danza, departamento, descripcion)
        )
    }
}
