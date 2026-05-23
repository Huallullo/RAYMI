package com.raymi.app.domain.model

import com.google.firebase.Timestamp

/**
 * Representa un "espacio de trabajo" = un negocio del usuario
 * Un usuario puede tener múltiples workspaces
 * 
 * Ejemplos:
 * - "Mi Tienda de Vestuarios" (tipo: VESTUARIOS)
 * - "Alquileres de Equipos de Cine" (tipo: EQUIPOS)
 * - "Transportes Lima" (tipo: VEHICULOS)
 */
data class Workspace(
    val id: String = "",
    val ownerId: String = "",                  // Usuario propietario (Firebase Auth UID)
    val nombre: String = "",                   // Nombre del negocio
    val descripcion: String = "",
    val tipoNegocio: String = "",              // VESTUARIOS|EQUIPOS|VEHICULOS|HERRAMIENTAS|OTRO
    val logoUrl: String? = null,
    val activo: Boolean = true,
    
    // Configuración del workspace
    val moneda: String = "PEN",                // Moneda: PEN, USD, ARS, BRL, etc
    val zonaHoraria: String = "America/Lima",  // Zona horaria del usuario
    val idioma: String = "es",                 // Idioma de la interfaz
    val mostrarAnuncios: Boolean = true,       // Mostrar ads si plan = FREE
    
    // Metadata
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val ultimoAcceso: Timestamp = Timestamp.now()
)

data class ConfiguracionWorkspace(
    val moneda: String = "PEN",
    val zonaHoraria: String = "America/Lima",
    val idioma: String = "es",
    val mostrarAnuncios: Boolean = true
)

