package com.raymi.app.domain.model

import com.google.firebase.Timestamp

/**
 * Registro de mantenimiento preventivo o correctivo para un Ítem.
 */
data class Mantenimiento(
    val id: String = "",
    val itemId: String = "",
    val workspaceId: String = "",
    val fecha: Timestamp = Timestamp.now(),
    val motivo: String = "",
    val costo: Double = 0.0,
    val descripcion: String = "",
    val responsable: String = "",
    val estadoFinal: String = "OPERATIVO", // OPERATIVO|DAÑADO|DE_BAJA
    val createdAt: Timestamp = Timestamp.now()
)
