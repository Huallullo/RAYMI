package com.raymi.app.domain.model

import com.google.firebase.Timestamp

data class Workspace(
    val id: String = "",
    val ownerId: String = "",
    val nombre: String = "",
    val nombreComercial: String = "",
    val ruc: String = "",
    val direccion: String = "",
    val telefono: String = "",
    val descripcion: String = "",
    val slogan: String = "",
    val tipoNegocio: String = "",
    val logoUrl: String? = null,
    val sloganImageUrl: String? = null,
    val googleMapsUrl: String = "",
    val activo: Boolean = true,
    
    // Configuración
    val moneda: String = "PEN",
    val zonaHoraria: String = "America/Lima",
    val idioma: String = "es",
    val mostrarAnuncios: Boolean = true,
    
    // Series de comprobantes
    val serieTicket: String = "T001",
    val serieBoleta: String = "B001",
    val serieFactura: String = "F001",
    
    // Términos
    val terminosCondiciones: String = "",
    val politicaPenalidades: String = "",
    
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val ultimoAcceso: Timestamp = Timestamp.now()
)
