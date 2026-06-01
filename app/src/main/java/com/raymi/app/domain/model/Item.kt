package com.raymi.app.domain.model

import com.google.firebase.Timestamp

/**
 * Item GENÉRICO para cualquier tipo de alquiler
 * Reemplaza al anterior "Vestuario" que era específico de ropa
 * 
 * Ahora Item es flexible y se adapta a cualquier tipo:
 * - Vestuarios: { talla, color, danza }
 * - Equipos de cine: { marca, modelo, resolution }
 * - Vehículos: { placa, capacidad_pasajeros, combustible }
 * - Herramientas: { dimension, peso, tipo }
 * 
 * El usuario define qué atributos tiene cada item
 */
data class Item(
    val id: String = "",
    val workspaceId: String = "",              // A qué workspace pertenece (IMPORTANTE)
    val nombre: String = "",                   // "Traje de Marinera", "Cámara Sony", "Toyota Corolla"
    val codigo: String = "",                   // SKU único dentro del workspace (ej: VES-001)
    val categoriaId: String = "",              // Referencia a Categoria
    val descripcion: String = "",
    val precio: Double = 0.0,
    val cantidad: Int = 1,                     // Cuántos items iguales tienes (STOCK TOTAL)
    val unidadesAlquiladas: Int = 0,           // Cuántos de esos items están fuera ahora
    val estado: String = "DISPONIBLE",         // DISPONIBLE|ALQUILADO|MANTENIMIENTO|NO_DISPONIBLE
    
    // Atributos dinámicos - El usuario define según su tipo de negocio
    val atributos: Map<String, String> = mapOf(),
    // Ejemplos:
    // Vestuarios:       { "talla": "M", "color": "rojo", "danza": "Marinera" }
    // Equipos cine:     { "marca": "Sony", "modelo": "A7III", "resolution": "4K" }
    // Vehículos:        { "placa": "ABC123", "capacidad": "5 pasajeros", "combustible": "gasolina" }
    // Herramientas:     { "dimension": "2m x 1m", "peso": "50kg", "tipo": "escalera" }
    
    val imagenUrl: String? = null,
    val imagenesSuplementarias: List<String> = emptyList(),
    
    // Metadata
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    /**
     * Calcula cuántos items están disponibles para alquilar
     * = cantidad total - (alquileres activos)
     * 
     * Nota: Para obtener alquileres activos, necesitarás hacer query en Alquileres
     * Esta es solo una sugerencia de cálculo
     */
    fun obtenerDisponibilidad(alquileresActivos: Int): Int {
        return (cantidad - alquileresActivos).coerceAtLeast(0)
    }
}

