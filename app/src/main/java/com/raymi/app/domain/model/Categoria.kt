package com.raymi.app.domain.model

import com.google.firebase.Timestamp

data class Categoria(
    val id: String = "",
    val workspaceId: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val icono: String? = null,
    val color: String = "#4F46E5",
    val activa: Boolean = true,
    val orden: Int = 0,
    val attributeTemplates: List<String> = emptyList(), // Placa, Año, Color, etc.
    val createdAt: Timestamp = Timestamp.now()
)
