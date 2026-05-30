package com.raymi.app.domain.repository

import com.raymi.app.domain.model.Cliente
import com.raymi.app.domain.model.Resource
import kotlinx.coroutines.flow.Flow

interface ClienteRepository {
    suspend fun getClientes(): Flow<Resource<List<Cliente>>>
    suspend fun getClientesOnce(): Resource<List<Cliente>>
    suspend fun getClienteById(id: String): Flow<Resource<Cliente>>
    suspend fun searchClienteByDni(dni: String): Flow<Resource<Cliente?>>
    suspend fun addCliente(cliente: Cliente): Flow<Resource<String>>
    suspend fun updateCliente(cliente: Cliente): Flow<Resource<Unit>>
    suspend fun deleteCliente(clienteId: String): Flow<Resource<Unit>>
    suspend fun searchClientes(query: String): Flow<Resource<List<Cliente>>>
}
