package com.raymi.app.domain.model

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

data class Cliente(
    val id: String = "",
    val workspaceId: String = "",              // A qué workspace pertenece (SaaS)
    val dni: String = "",
    val nombre: String = "",
    val apellidos: String = "",
    val telefono: String = "",
    val email: String = "",
    val direccion: String = "",
    val createdAt: Timestamp = Timestamp.now()
) {
    val nombreCompleto: String
        get() = "$nombre $apellidos"

    val iniciales: String
        get() = "${nombre.firstOrNull()?.uppercase() ?: ""}${apellidos.firstOrNull()?.uppercase() ?: ""}"

    val createdAtFormatted: String
        get() = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(createdAt.toDate())
}
