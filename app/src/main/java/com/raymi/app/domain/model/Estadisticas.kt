package com.raymi.app.domain.model

data class Estadisticas(
    val totalClientes: Int = 0,
    val totalVestuarios: Int = 0,
    val vestuariosDisponibles: Int = 0,
    val alquileresActivos: Int = 0,
    val alquileresVencidos: Int = 0,
    val ingresosMes: Double = 0.0,
    val ingresosTotales: Double = 0.0,
    val topVestuarios: List<VestuarioStats> = emptyList(),
    val topDepartamentos: List<DepartamentoStats> = emptyList()
)

data class VestuarioStats(
    val nombre: String,
    val cantidad: Int
)

data class DepartamentoStats(
    val departamento: String,
    val cantidad: Int
)