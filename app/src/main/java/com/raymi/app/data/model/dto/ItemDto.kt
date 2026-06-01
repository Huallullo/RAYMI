package com.raymi.app.data.model.dto

import com.google.firebase.Timestamp
import com.raymi.app.domain.model.Item

data class ItemDto(
    val id: String = "",
    val workspaceId: String = "",
    val nombre: String = "",
    val codigo: String = "",
    val categoriaId: String = "",
    val descripcion: String = "",
    val precio: Double = 0.0,
    val cantidad: Int = 1,
    val unidadesAlquiladas: Int = 0,
    val estado: String = "DISPONIBLE",
    val atributos: Map<String, String> = mapOf(),
    val imagenUrl: String? = null,
    val imagenesSuplementarias: List<String> = emptyList(),
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    fun toDomain(): Item = Item(
        id = id,
        workspaceId = workspaceId,
        nombre = nombre,
        codigo = codigo,
        categoriaId = categoriaId,
        descripcion = descripcion,
        precio = precio,
        cantidad = cantidad,
        unidadesAlquiladas = unidadesAlquiladas,
        estado = estado,
        atributos = atributos,
        imagenUrl = imagenUrl,
        imagenesSuplementarias = imagenesSuplementarias,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(domain: Item): ItemDto = ItemDto(
            id = domain.id,
            workspaceId = domain.workspaceId,
            nombre = domain.nombre,
            codigo = domain.codigo,
            categoriaId = domain.categoriaId,
            descripcion = domain.descripcion,
            precio = domain.precio,
            cantidad = domain.cantidad,
            unidadesAlquiladas = domain.unidadesAlquiladas,
            estado = domain.estado,
            atributos = domain.atributos,
            imagenUrl = domain.imagenUrl,
            imagenesSuplementarias = domain.imagenesSuplementarias,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )

        @Suppress("UNCHECKED_CAST")
        fun fromMap(id: String, map: Map<String, Any>): ItemDto = ItemDto(
            id = id,
            workspaceId = (map["workspaceId"] as? String) ?: (map["negocioId"] as? String) ?: "",
            nombre = map["nombre"] as? String ?: "",
            codigo = map["codigo"] as? String ?: "",
            categoriaId = map["categoriaId"] as? String ?: "",
            descripcion = map["descripcion"] as? String ?: "",
            precio = (map["precio"] as? Number)?.toDouble() ?: 0.0,
            cantidad = (map["cantidad"] as? Number)?.toInt() ?: 1,
            unidadesAlquiladas = (map["unidadesAlquiladas"] as? Number)?.toInt() ?: 0,
            estado = map["estado"] as? String ?: "DISPONIBLE",
            atributos = map["atributos"] as? Map<String, String> ?: mapOf(),
            imagenUrl = map["imagenUrl"] as? String,
            imagenesSuplementarias = map["imagenesSuplementarias"] as? List<String> ?: emptyList(),
            createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now(),
            updatedAt = map["updatedAt"] as? Timestamp ?: Timestamp.now()
        )
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "workspaceId" to workspaceId,
        "negocioId" to workspaceId, // QA Fix: Consistencia con reglas SaaS
        "nombre" to nombre,
        "codigo" to codigo,
        "categoriaId" to categoriaId,
        "descripcion" to descripcion,
        "precio" to precio,
        "cantidad" to cantidad,
        "unidadesAlquiladas" to unidadesAlquiladas,
        "estado" to estado,
        "atributos" to atributos,
        "imagenUrl" to imagenUrl,
        "imagenesSuplementarias" to imagenesSuplementarias,
        "searchTerms" to generateSearchTerms(nombre, codigo),
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )

    private fun generateSearchTerms(nombre: String, codigo: String): List<String> {
        val tokens = (nombre.lowercase() + " " + codigo.lowercase()).split(" ").filter { it.isNotBlank() }
        val prefixes = mutableSetOf<String>()
        tokens.forEach { token ->
            for (i in 1..token.length) {
                prefixes.add(token.substring(0, i))
            }
        }
        return prefixes.toList()
    }
}
