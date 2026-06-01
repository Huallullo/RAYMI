package com.raymi.app.domain.repository

import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.EstadoAlquiler
import com.raymi.app.domain.model.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Interfaz del repositorio de alquileres
 */
interface AlquilerRepository {

    /**
     * Obtiene todos los alquileres (Stream real-time)
     */
    suspend fun getAlquileres(workspaceId: String): Flow<Resource<List<Alquiler>>>

    /**
     * Obtiene todos los alquileres una sola vez (Snapshot)
     */
    suspend fun getAlquileresOnce(workspaceId: String): Resource<List<Alquiler>>

    /**
     * Obtiene un alquiler por su ID
     */
    suspend fun getAlquilerById(id: String): Flow<Resource<Alquiler>>

    /**
     * Obtiene alquileres por estado
     */
    suspend fun getAlquileresByEstado(workspaceId: String, estado: EstadoAlquiler): Flow<Resource<List<Alquiler>>>

    /**
     * Obtiene alquileres de un cliente específico
     */
    suspend fun getAlquileresByCliente(workspaceId: String, clienteId: String): Flow<Resource<List<Alquiler>>>

    /**
     * Obtiene alquileres de un ítem específico
     */
    suspend fun getAlquileresByItem(workspaceId: String, itemId: String): Flow<Resource<List<Alquiler>>>

    /**
     * Obtiene alquileres filtrados por rango de fecha de creación (Optimizado para Dashboard)
     */
    suspend fun getAlquileresByDateRange(workspaceId: String, start: com.google.firebase.Timestamp, end: com.google.firebase.Timestamp): Flow<Resource<List<Alquiler>>>

    /**
     * Crea un nuevo alquiler
     */
    suspend fun createAlquiler(alquiler: Alquiler): Flow<Resource<String>>

    /**
     * Actualiza un alquiler existente
     */
    suspend fun updateAlquiler(alquiler: Alquiler): Flow<Resource<Unit>>

    suspend fun updateAlquilerConStock(alquiler: Alquiler, diffCantidad: Int): Flow<Resource<Unit>>

    /**
     * Registra la devolución de un alquiler con penalidades opcionales
     */
    suspend fun registrarDevolucion(
        alquilerId: String,
        penalidad: Double = 0.0,
        observaciones: String = "",
        montoGarantiaRetenida: Double = 0.0,
        unidadesARetornar: Int = 0
    ): Flow<Resource<Unit>>

    /**
     * Cancela un alquiler y libera el stock
     */
    suspend fun cancelarAlquiler(alquilerId: String, motivo: String): Flow<Resource<Unit>>

    /**
     * Actualiza el estado de un alquiler
     */
    suspend fun updateEstadoAlquiler(alquilerId: String, estado: EstadoAlquiler): Flow<Resource<Unit>>

    /**
     * Elimina un alquiler
     */
    suspend fun deleteAlquiler(alquilerId: String): Flow<Resource<Unit>>

    /**
     * Registra un nuevo pago para un alquiler
     */
    suspend fun addPago(workspaceId: String, alquilerId: String, pago: com.raymi.app.domain.model.Pago): Flow<Resource<Unit>>

    /**
     * Obtiene el historial de pagos de un alquiler
     */
    suspend fun getPagos(workspaceId: String, alquilerId: String): Flow<Resource<List<com.raymi.app.domain.model.Pago>>>

    /**
     * Obtiene todos los pagos de una lista de alquileres
     */
    suspend fun getPagosDeAlquileres(workspaceId: String, alquilerIds: List<String>): Resource<List<com.raymi.app.domain.model.Pago>>
}
