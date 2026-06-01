package com.raymi.app.data.model.dto

import com.google.firebase.Timestamp
import com.raymi.app.domain.model.Cliente
import com.raymi.app.domain.model.EstadoCliente

/**
 * DTO para transferencia de datos de Cliente con Firebase.
 * Incluye campos para respaldo de identidad (Fotos DNI y Rostro).
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
    val whatsapp: String = "",
    val notas: String = "",
    val estado: String = "ACTIVO",
    val totalGastado: Double = 0.0,
    val deudaTotal: Double = 0.0,
    val ultimoAlquiler: Timestamp? = null,
    val fotoDniFrontUrl: String? = null,
    val fotoDniBackUrl: String? = null,
    val fotoRostroUrl: String? = null,
    val createdAt: Timestamp = Timestamp.now()
) {
    fun toDomain(): Cliente = Cliente(
        id = id,
        workspaceId = workspaceId,
        dni = dni,
        nombre = nombre,
        apellidos = apellidos,
        telefono = telefono,
        email = email,
        direccion = direccion,
        whatsapp = whatsapp,
        notas = notas,
        estado = try { EstadoCliente.valueOf(estado) } catch (_: Exception) { EstadoCliente.ACTIVO },
        totalGastado = totalGastado,
        deudaTotal = deudaTotal,
        ultimoAlquiler = ultimoAlquiler,
        fotoDniFrontUrl = fotoDniFrontUrl,
        fotoDniBackUrl = fotoDniBackUrl,
        fotoRostroUrl = fotoRostroUrl,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(cliente: Cliente): ClienteDto = ClienteDto(
            id = cliente.id,
            workspaceId = cliente.workspaceId,
            dni = cliente.dni,
            nombre = cliente.nombre,
            apellidos = cliente.apellidos,
            telefono = cliente.telefono,
            email = cliente.email,
            direccion = cliente.direccion,
            whatsapp = cliente.whatsapp,
            notas = cliente.notas,
            estado = cliente.estado.name,
            totalGastado = cliente.totalGastado,
            deudaTotal = cliente.deudaTotal,
            ultimoAlquiler = cliente.ultimoAlquiler,
            fotoDniFrontUrl = cliente.fotoDniFrontUrl,
            fotoDniBackUrl = cliente.fotoDniBackUrl,
            fotoRostroUrl = cliente.fotoRostroUrl,
            createdAt = cliente.createdAt
        )

        fun fromMap(id: String, map: Map<String, Any>): ClienteDto = ClienteDto(
            id = id,
            workspaceId = (map["workspaceId"] as? String) ?: (map["negocioId"] as? String) ?: "",
            dni = map["dni"] as? String ?: "",
            nombre = map["nombre"] as? String ?: "",
            apellidos = map["apellidos"] as? String ?: "",
            telefono = map["telefono"] as? String ?: "",
            email = map["email"] as? String ?: "",
            direccion = map["direccion"] as? String ?: "",
            whatsapp = map["whatsapp"] as? String ?: "",
            notas = map["notas"] as? String ?: "",
            estado = map["estado"] as? String ?: "ACTIVO",
            totalGastado = (map["totalGastado"] as? Number)?.toDouble() ?: 0.0,
            deudaTotal = (map["deudaTotal"] as? Number)?.toDouble() ?: 0.0,
            ultimoAlquiler = map["ultimoAlquiler"] as? Timestamp,
            fotoDniFrontUrl = map["fotoDniFrontUrl"] as? String,
            fotoDniBackUrl = map["fotoDniBackUrl"] as? String,
            fotoRostroUrl = map["fotoRostroUrl"] as? String,
            createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now()
        )

        private fun buildSearchTerms(dni: String, nombre: String, apellidos: String): List<String> {
            val baseTokens = listOf(dni, nombre, apellidos)
                .flatMap { it.trim().lowercase().split("\\s+".toRegex()) }
                .filter { it.isNotBlank() }

            val prefixes = mutableSetOf<String>()
            baseTokens.forEach { token ->
                for (i in 1..token.length) {
                    prefixes.add(token.substring(0, i))
                }
            }
            return prefixes.toList()
        }
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "workspaceId" to workspaceId,
        "negocioId" to workspaceId,
        "dni" to dni,
        "nombre" to nombre,
        "apellidos" to apellidos,
        "telefono" to telefono,
        "email" to email,
        "direccion" to direccion,
        "whatsapp" to whatsapp,
        "notas" to notas,
        "estado" to estado,
        "totalGastado" to totalGastado,
        "deudaTotal" to deudaTotal,
        "ultimoAlquiler" to ultimoAlquiler,
        "fotoDniFrontUrl" to fotoDniFrontUrl,
        "fotoDniBackUrl" to fotoDniBackUrl,
        "fotoRostroUrl" to fotoRostroUrl,
        "createdAt" to createdAt,
        "searchTerms" to buildSearchTerms(dni, nombre, apellidos)
    )
}
