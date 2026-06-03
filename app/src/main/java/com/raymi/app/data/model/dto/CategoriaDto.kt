package com.raymi.app.data.model.dto

import com.google.firebase.Timestamp
import com.raymi.app.domain.model.Categoria

data class CategoriaDto(
    val id: String = "",
    val workspaceId: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val icono: String? = null,
    val color: String = "#4F46E5",
    val activa: Boolean = true,
    val orden: Int = 0,
    val attributeTemplates: List<String> = emptyList(),
    val createdAt: Timestamp = Timestamp.now()
) {
    fun toDomain(): Categoria = Categoria(
        id = id,
        workspaceId = workspaceId,
        nombre = nombre,
        descripcion = descripcion,
        icono = icono,
        color = color,
        activa = activa,
        orden = orden,
        attributeTemplates = attributeTemplates,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(domain: Categoria): CategoriaDto = CategoriaDto(
            id = domain.id,
            workspaceId = domain.workspaceId,
            nombre = domain.nombre,
            descripcion = domain.descripcion,
            icono = domain.icono,
            color = domain.color,
            activa = domain.activa,
            orden = domain.orden,
            attributeTemplates = domain.attributeTemplates,
            createdAt = domain.createdAt
        )

        @Suppress("UNCHECKED_CAST")
        fun fromMap(id: String, map: Map<String, Any>): CategoriaDto = CategoriaDto(
            id = id,
            workspaceId = (map["workspaceId"] as? String) ?: (map["negocioId"] as? String) ?: "",
            nombre = map["nombre"] as? String ?: "",
            descripcion = map["descripcion"] as? String ?: "",
            icono = map["icono"] as? String,
            color = map["color"] as? String ?: "#4F46E5",
            activa = map["activa"] as? Boolean ?: true,
            orden = (map["orden"] as? Number)?.toInt() ?: 0,
            attributeTemplates = map["attributeTemplates"] as? List<String> ?: emptyList(),
            createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now()
        )
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "workspaceId" to workspaceId,
        "negocioId" to workspaceId,
        "nombre" to nombre,
        "descripcion" to descripcion,
        "icono" to icono,
        "color" to color,
        "activa" to activa,
        "orden" to orden,
        "attributeTemplates" to attributeTemplates,
        "createdAt" to createdAt
    )
}
