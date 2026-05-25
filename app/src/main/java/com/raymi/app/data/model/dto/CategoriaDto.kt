package com.raymi.app.data.model.dto

import com.google.firebase.Timestamp
import com.raymi.app.domain.model.Categoria

data class CategoriaDto(
    val id: String = "",
    val workspaceId: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val icono: String? = null,
    val color: String = "#3F51B5",
    val activa: Boolean = true,
    val orden: Int = 0,
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
            createdAt = domain.createdAt
        )

        fun fromMap(id: String, map: Map<String, Any>): CategoriaDto = CategoriaDto(
            id = id,
            workspaceId = (map["workspaceId"] as? String) ?: (map["negocioId"] as? String) ?: "",
            nombre = map["nombre"] as? String ?: "",
            descripcion = map["descripcion"] as? String ?: "",
            icono = map["icono"] as? String,
            color = map["color"] as? String ?: "#3F51B5",
            activa = map["activa"] as? Boolean ?: true,
            orden = (map["orden"] as? Number)?.toInt() ?: 0,
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
        "createdAt" to createdAt
    )
}
