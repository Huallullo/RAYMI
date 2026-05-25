package com.raymi.app.data.model.dto

import com.google.firebase.Timestamp
import com.raymi.app.domain.model.Cliente

/**
 * DTO para transferencia de datos de Cliente con Firebase
 */
data class ClienteDto(
    val id: String = "",
    val workspaceId: String = "",
    val dni: String = "",
    val nombre: String = "",
    val apellidos: String = "",
    val telefono: String = "",
    val email: String = "",
    val direccion: String = "",
    val createdAt: Timestamp = Timestamp.now()
) {
    /**
     * Convierte DTO a modelo de dominio
     */
    fun toDomain(): Cliente {
        return Cliente(
            id = id,
            workspaceId = workspaceId,
            dni = dni,
            nombre = nombre,
            apellidos = apellidos,
            telefono = telefono,
            email = email,
            direccion = direccion,
            createdAt = createdAt
        )
    }

    companion object {
        /**
         * Convierte modelo de dominio a DTO
         */
        fun fromDomain(cliente: Cliente): ClienteDto {
            return ClienteDto(
                id = cliente.id,
                workspaceId = cliente.workspaceId,
                dni = cliente.dni,
                nombre = cliente.nombre,
                apellidos = cliente.apellidos,
                telefono = cliente.telefono,
                email = cliente.email,
                direccion = cliente.direccion,
                createdAt = cliente.createdAt
            )
        }

        /**
         * Crea DTO desde un Map de Firebase
         */
        fun fromMap(id: String, map: Map<String, Any>): ClienteDto {
            return ClienteDto(
                id = id,
                workspaceId = (map["workspaceId"] as? String) ?: (map["negocioId"] as? String) ?: "",
                dni = map["dni"] as? String ?: "",
                nombre = map["nombre"] as? String ?: "",
                apellidos = map["apellidos"] as? String ?: "",
                telefono = map["telefono"] as? String ?: "",
                email = map["email"] as? String ?: "",
                direccion = map["direccion"] as? String ?: "",
                createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now()
            )
        }
        private fun buildSearchTerms(
            dni: String,
            nombre: String,
            apellidos: String
        ): List<String> {
            val baseTokens = listOf(dni, nombre, apellidos)
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
            "workspaceId" to workspaceId,
            "negocioId" to workspaceId,
            "dni" to dni,
            "nombre" to nombre,
            "apellidos" to apellidos,
            "telefono" to telefono,
            "email" to email,
            "direccion" to direccion,
            "createdAt" to createdAt,
            "searchTerms" to buildSearchTerms(dni, nombre, apellidos)
        )
    }
}
