package com.raymi.app.domain.model

data class PersonaData(
    val dni: String,
    val nombres: String,
    val apellidoPaterno: String,
    val apellidoMaterno: String = "",
    val fechaNacimiento: String = ""
) {
    val nombreCompleto: String get() = "$nombres $apellidoPaterno $apellidoMaterno".trim()
}

data class EmpresaData(
    val ruc: String,
    val razonSocial: String,
    val estado: String? = null,
    val condicion: String? = null,
    val direccion: String? = null,
    val departamento: String? = null,
    val provincia: String? = null,
    val distrito: String? = null
)
