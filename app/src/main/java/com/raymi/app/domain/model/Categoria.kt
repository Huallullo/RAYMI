package com.raymi.app.domain.model

import com.google.firebase.Timestamp

/**
 * Categoría para agrupar Items dentro de un workspace
 * 
 * Ejemplos según tipo de negocio:
 * 
 * Para VESTUARIOS:
 * - "Trajes Folklóricos"
 * - "Accesorios"
 * - "Complementos"
 * 
 * Para EQUIPOS DE CINE:
 * - "Cámaras"
 * - "Lentes"
 * - "Iluminación"
 * - "Audio"
 * 
 * Para VEHÍCULOS:
 * - "SUVs"
 * - "Sedanes"
 * - "Camionetas"
 * 
 * Cada usuario crea las categorías que necesita para su negocio
 */
data class Categoria(
    val id: String = "",
    val workspaceId: String = "",              // A qué workspace pertenece
    val nombre: String = "",                   // "Trajes Folklóricos"
    val descripcion: String = "",
    val icono: String? = null,                 // URL o emoji (ej: "👔", "🎬", "🚗")
    val color: String = "#3F51B5",             // Color hexadecimal para UI
    val activa: Boolean = true,
    val orden: Int = 0,                        // Para ordenar en UI (0 = primero)
    val createdAt: Timestamp = Timestamp.now()
)

