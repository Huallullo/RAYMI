package com.raymi.app.data.repository

import com.raymi.app.core.workspace.WorkspaceManager
import com.raymi.app.data.model.dto.ClienteDto
import com.raymi.app.data.remote.ClientDataSource
import com.raymi.app.data.remote.FirebaseDataSource
import com.raymi.app.data.remote.ObserverDataSource
import com.raymi.app.domain.model.Cliente
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.ClienteRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class ClienteRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseDataSource,
    private val clientDataSource: ClientDataSource,
    private val observerDataSource: ObserverDataSource,
    private val workspaceManager: WorkspaceManager
) : ClienteRepository {

    private fun getWorkspaceId() = workspaceManager.getWorkspaceId() ?: throw IllegalStateException("Negocio no seleccionado")

    override suspend fun getClientes(): Flow<Resource<List<Cliente>>> {
        return observerDataSource.observeBusinessCollection(
            workspaceId = getWorkspaceId(),
            collection = "clientes",
            orderByField = "createdAt",
            descending = true,
            limit = 50
        )
            .map { documents ->
                val clientes = documents.map { (id, data) -> ClienteDto.fromMap(id, data).toDomain() }
                Resource.Success(clientes) as Resource<List<Cliente>>
            }
            .onStart { emit(Resource.Loading()) }
            .catch { e ->
                if (e is CancellationException) throw e
                emit(Resource.Error("Error al obtener clientes: ${e.message}"))
            }
    }

    override suspend fun getClienteById(id: String): Flow<Resource<Cliente>> = flow {
        emit(Resource.Loading())
        try {
            val data = dataSource.getBusinessDocument("clientes", id, getWorkspaceId())
            if (data != null) {
                emit(Resource.Success(ClienteDto.fromMap(id, data).toDomain()))
            } else {
                emit(Resource.Error("Cliente no encontrado"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error("Error al obtener cliente: ${e.message}"))
        }
    }

    override suspend fun searchClienteByDni(dni: String): Flow<Resource<Cliente?>> = flow {
        emit(Resource.Loading())
        try {
            val workspaceId = getWorkspaceId()
            val document = dataSource.getBusinessDocument("clientes_dni_index", dni, workspaceId)
            if (document != null) {
                val clienteId = document["clienteId"] as? String
                if (clienteId != null) {
                    val clienteData = dataSource.getBusinessDocument("clientes", clienteId, workspaceId)
                    if (clienteData != null) {
                        emit(Resource.Success(ClienteDto.fromMap(clienteId, clienteData).toDomain()))
                        return@flow
                    }
                }
            }
            emit(Resource.Success(null))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error("Error al buscar: ${e.message}"))
        }
    }

    override suspend fun addCliente(cliente: Cliente): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            val dto = ClienteDto.fromDomain(cliente)
            val data = dto.toMap().filterValues { it != null }.mapValues { it.value!! }
            val id = clientDataSource.addClienteTransactional(cliente.workspaceId, data, cliente.dni)
            emit(Resource.Success(id))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error(e.message ?: "Error al agregar"))
        }
    }

    override suspend fun updateCliente(cliente: Cliente): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val dto = ClienteDto.fromDomain(cliente)
            val data = dto.toMap().filterValues { it != null }.mapValues { it.value!! }
            dataSource.updateBusinessDocument("clientes", cliente.id, data, cliente.workspaceId)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error("Error al actualizar: ${e.message}"))
        }
    }

    override suspend fun deleteCliente(clienteId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            // Necesitamos el DNI para borrar el índice. 
            // Podríamos obtener el cliente primero o cambiar el parámetro.
            // Para simplicidad en esta interfaz vieja, asumimos que el DNI no es necesario si el repo lo busca.
            val workspaceId = getWorkspaceId()
            val data = dataSource.getBusinessDocument("clientes", clienteId, workspaceId)
            val dni = data?.get("dni") as? String ?: ""
            clientDataSource.deleteClienteTransactional(workspaceId, clienteId, dni)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error("Error al eliminar: ${e.message}"))
        }
    }

    override suspend fun searchClientes(query: String): Flow<Resource<List<Cliente>>> = flow {
        emit(Resource.Loading())
        try {
            val documents = dataSource.queryBusinessDocuments("clientes", "nombre", query, limit = 50, negocioId = getWorkspaceId())
            val clientes = documents.map { (id, data) -> ClienteDto.fromMap(id, data).toDomain() }
            emit(Resource.Success(clientes))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error("Error en búsqueda: ${e.message}"))
        }
    }
}
