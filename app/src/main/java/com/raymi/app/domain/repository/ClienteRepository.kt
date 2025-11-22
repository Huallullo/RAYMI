package com.raymi.app.domain.repository

import com.raymi.app.domain.model.Cliente
import com.raymi.app.domain.model.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Interfaz del repositorio de clientes
 */
interface ClienteRepository {

    /**
     * Obtiene todos los clientes
     */
    suspend fun getClientes(): Flow<Resource<List<Cliente>>>

    /**
     * Obtiene un cliente por su ID
     */
    suspend fun getClienteById(id: String): Flow<Resource<Cliente>>

    /**
     * Busca clientes por DNI
     */
    suspend fun searchClienteByDni(dni: String): Flow<Resource<Cliente?>>

    /**
     * Agrega un nuevo cliente
     */
    suspend fun addCliente(cliente: Cliente): Flow<Resource<String>>

    /**
     * Actualiza un cliente existente
     */
    suspend fun updateCliente(cliente: Cliente): Flow<Resource<Unit>>

    /**
     * Elimina un cliente
     */
    suspend fun deleteCliente(clienteId: String): Flow<Resource<Unit>>

    /**
     * Busca clientes por nombre
     */
    suspend fun searchClientes(query: String): Flow<Resource<List<Cliente>>>
}