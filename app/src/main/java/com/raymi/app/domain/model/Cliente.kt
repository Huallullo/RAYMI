package com.raymi.app.domain.model

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

data class Cliente(
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
    val estado: EstadoCliente = EstadoCliente.ACTIVO,
    val totalGastado: Double = 0.0,
    val deudaTotal: Double = 0.0,
    val ultimoAlquiler: Timestamp? = null,
    val fotoDniFrontUrl: String? = null,
    val fotoDniBackUrl: String? = null,
    val fotoRostroUrl: String? = null,
    val createdAt: Timestamp = Timestamp.now()
) {
    val nombreCompleto: String get() = "$nombre $apellidos"

    val iniciales: String
        get() = "${nombre.firstOrNull()?.uppercase() ?: ""}${apellidos.firstOrNull()?.uppercase() ?: ""}"

    val createdAtFormatted: String
        get() = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(createdAt.toDate())
}

enum class EstadoCliente {
    ACTIVO,
    FRECUENTE,
    MOROSO,
    BLOQUEADO
}
