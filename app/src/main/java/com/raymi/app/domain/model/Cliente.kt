package com.raymi.app.domain.model

import com.google.firebase.Timestamp

data class Cliente(
    val id: String = "",
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
}
