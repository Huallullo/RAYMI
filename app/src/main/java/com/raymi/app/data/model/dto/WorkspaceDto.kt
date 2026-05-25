package com.raymi.app.data.model.dto

import com.google.firebase.Timestamp
import com.raymi.app.domain.model.Workspace

data class WorkspaceDto(
    val id: String = "",
    val ownerId: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val tipoNegocio: String = "",
    val logoUrl: String? = null,
    val activo: Boolean = true,
    val moneda: String = "PEN",
    val zonaHoraria: String = "America/Lima",
    val idioma: String = "es",
    val mostrarAnuncios: Boolean = true,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val ultimoAcceso: Timestamp = Timestamp.now()
) {
    fun toDomain(): Workspace = Workspace(
        id = id,
        ownerId = ownerId,
        nombre = nombre,
        descripcion = descripcion,
        tipoNegocio = tipoNegocio,
        logoUrl = logoUrl,
        activo = activo,
        moneda = moneda,
        zonaHoraria = zonaHoraria,
        idioma = idioma,
        mostrarAnuncios = mostrarAnuncios,
        createdAt = createdAt,
        updatedAt = updatedAt,
        ultimoAcceso = ultimoAcceso
    )

    companion object {
        fun fromDomain(domain: Workspace): WorkspaceDto = WorkspaceDto(
            id = domain.id,
            ownerId = domain.ownerId,
            nombre = domain.nombre,
            descripcion = domain.descripcion,
            tipoNegocio = domain.tipoNegocio,
            logoUrl = domain.logoUrl,
            activo = domain.activo,
            moneda = domain.moneda,
            zonaHoraria = domain.zonaHoraria,
            idioma = domain.idioma,
            mostrarAnuncios = domain.mostrarAnuncios,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
            ultimoAcceso = domain.ultimoAcceso
        )

        fun fromMap(id: String, map: Map<String, Any>): WorkspaceDto = WorkspaceDto(
            id = id,
            ownerId = (map["ownerUid"] as? String) ?: (map["ownerId"] as? String) ?: "",
            nombre = map["nombre"] as? String ?: "",
            descripcion = map["descripcion"] as? String ?: "",
            tipoNegocio = map["tipoNegocio"] as? String ?: "",
            logoUrl = map["logoUrl"] as? String,
            activo = map["activo"] as? Boolean ?: true,
            moneda = map["moneda"] as? String ?: "PEN",
            zonaHoraria = map["zonaHoraria"] as? String ?: "America/Lima",
            idioma = map["idioma"] as? String ?: "es",
            mostrarAnuncios = map["mostrarAnuncios"] as? Boolean ?: true,
            createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now(),
            updatedAt = map["updatedAt"] as? Timestamp ?: Timestamp.now(),
            ultimoAcceso = map["ultimoAcceso"] as? Timestamp ?: Timestamp.now()
        )
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "ownerUid" to ownerId,
        "nombre" to nombre,
        "descripcion" to descripcion,
        "tipoNegocio" to tipoNegocio,
        "logoUrl" to logoUrl,
        "activo" to activo,
        "moneda" to moneda,
        "zonaHoraria" to zonaHoraria,
        "idioma" to idioma,
        "mostrarAnuncios" to mostrarAnuncios,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "ultimoAcceso" to ultimoAcceso
    )
}
