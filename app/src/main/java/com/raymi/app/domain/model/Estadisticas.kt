package com.raymi.app.domain.model

data class Estadisticas(
    val totalClientes: Int = 0,
    val totalItems: Int = 0,
    val itemsDisponibles: Int = 0,
    val alquileresActivos: Int = 0,
    val alquileresVencidos: Int = 0,
    val ingresosMes: Double = 0.0,
    val ingresosTotales: Double = 0.0,
    val topItems: List<ItemStats> = emptyList(),
    val topCategorias: List<CategoriaStats> = emptyList()
)

data class ItemStats(
    val nombre: String,
    val cantidad: Int
)

data class CategoriaStats(
    val categoria: String,
    val cantidad: Int
)
