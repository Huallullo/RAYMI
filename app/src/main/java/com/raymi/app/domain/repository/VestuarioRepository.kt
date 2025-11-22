package com.raymi.app.domain.repository

import com.raymi.app.domain.model.EstadoVestuario
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.Vestuario
import kotlinx.coroutines.flow.Flow

/**
 * Interfaz del repositorio de vestuarios
 */
interface VestuarioRepository {

    /**
     * Obtiene todos los vestuarios
     */
    suspend fun getVestuarios(): Flow<Resource<List<Vestuario>>>

    /**
     * Obtiene un vestuario por su ID
     */
    suspend fun getVestuarioById(id: String): Flow<Resource<Vestuario>>

    /**
     * Obtiene vestuarios por estado
     */
    suspend fun getVestuariosByEstado(estado: EstadoVestuario): Flow<Resource<List<Vestuario>>>

    /**
     * Busca vestuarios por código
     */
    suspend fun searchVestuarioByCodigo(codigo: String): Flow<Resource<Vestuario?>>

    /**
     * Agrega un nuevo vestuario
     */
    suspend fun addVestuario(vestuario: Vestuario): Flow<Resource<String>>

    /**
     * Actualiza un vestuario existente
     */
    suspend fun updateVestuario(vestuario: Vestuario): Flow<Resource<Unit>>

    /**
     * Actualiza el estado de un vestuario
     */
    suspend fun updateEstadoVestuario(vestuarioId: String, estado: EstadoVestuario): Flow<Resource<Unit>>

    /**
     * Elimina un vestuario
     */
    suspend fun deleteVestuario(vestuarioId: String): Flow<Resource<Unit>>

    /**
     * Busca vestuarios por departamento o danza
     */
    suspend fun searchVestuarios(query: String): Flow<Resource<List<Vestuario>>>
}