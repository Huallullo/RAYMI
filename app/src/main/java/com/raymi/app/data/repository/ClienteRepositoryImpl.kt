package com.raymi.app.data.repository

import com.raymi.app.core.cache.SmartCache
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
import javax.inject.Singleton

@Singleton
class ClienteRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseDataSource,
    private val clientDataSource: ClientDataSource,
    private val observerDataSource: ObserverDataSource,
    private val workspaceManager: WorkspaceManager
) : ClienteRepository {

    // Cache segmentada por Workspace (Multi-tenancy Fix)
    private val cacheMap = mutableMapOf<String, SmartCache<List<Cliente>>>()
    private val TTL_10_MIN = 10 * 60 * 1000L

    private fun getCacheFor(workspaceId: String) = cacheMap.getOrPut(workspaceId) { SmartCache() }

    private fun getWorkspaceId() = workspaceManager.getWorkspaceId() ?: throw IllegalStateException("Negocio no seleccionado")

    override suspend fun getClientes(): Flow<Resource<List<Cliente>>> {
        return flow {
            emit(Resource.Loading())
            emit(getClientesOnce(getWorkspaceId()))
        }
    }

    override suspend fun getClientesOnce(workspaceId: String): Resource<List<Cliente>> {
        val cache = getCacheFor(workspaceId)
        cache.get()?.let { return Resource.Success(it) }

        return try {
            val docs = dataSource.getBusinessDocumentsPaged("clientes", limit = 25)
            val list = docs.map { (id, data) -> ClienteDto.fromMap(id, data).toDomain() }
            cache.set(list, TTL_10_MIN)
            Resource.Success(list)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error("Error al cargar clientes: ${e.message}")
        }
    }

    override suspend fun getClienteById(id: String): Flow<Resource<Cliente>> = flow {
        emit(Resource.Loading())
        val result = try {
            val data = dataSource.getBusinessDocument("clientes", id, getWorkspaceId())
            if (data != null) {
                Resource.Success(ClienteDto.fromMap(id, data).toDomain())
            } else {
                Resource.Error("Cliente no encontrado")
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error("Error al obtener cliente: ${e.message}")
        }
        emit(result)
    }

    override suspend fun searchClienteByDni(dni: String): Flow<Resource<Cliente?>> = flow {
        emit(Resource.Loading())
        val result: Resource<Cliente?> = try {
            val workspaceId = getWorkspaceId()
            val document = dataSource.getBusinessDocument("clientes_dni_index", dni, workspaceId)
            if (document != null) {
                val clienteId = document["clienteId"] as? String
                if (clienteId != null) {
                    val clienteData = dataSource.getBusinessDocument("clientes", clienteId, workspaceId)
                    if (clienteData != null) {
                        Resource.Success(ClienteDto.fromMap(clienteId, clienteData).toDomain())
                    } else Resource.Success(null)
                } else Resource.Success(null)
            } else Resource.Success(null)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error("Error al buscar: ${e.message}")
        }
        emit(result)
    }

    override suspend fun addCliente(cliente: Cliente): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        val result = try {
            val dto = ClienteDto.fromDomain(cliente)
            val data = dto.toMap().filterValues { it != null }.mapValues { it.value!! }
            val id = clientDataSource.addClienteTransactional(cliente.workspaceId, data, cliente.dni)
            getCacheFor(cliente.workspaceId).invalidate()
            Resource.Success(id)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error(e.message ?: "Error al agregar")
        }
        emit(result)
    }

    override suspend fun updateCliente(cliente: Cliente): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        val result = try {
            val dto = ClienteDto.fromDomain(cliente)
            val data = dto.toMap().filterValues { it != null }.mapValues { it.value!! }
            dataSource.updateBusinessDocument("clientes", cliente.id, data, cliente.workspaceId)
            getCacheFor(cliente.workspaceId).invalidate()
            Resource.Success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error("Error al actualizar: ${e.message}")
        }
        emit(result)
    }

    override suspend fun deleteCliente(clienteId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        val result = try {
            val workspaceId = getWorkspaceId()
            val data = dataSource.getBusinessDocument("clientes", clienteId, workspaceId)
            val dni = data?.get("dni") as? String ?: ""
            clientDataSource.deleteClienteTransactional(workspaceId, clienteId, dni)
            getCacheFor(workspaceId).invalidate()
            Resource.Success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error("Error al eliminar: ${e.message}")
        }
        emit(result)
    }

    override suspend fun searchClientes(query: String): Flow<Resource<List<Cliente>>> = flow {
        emit(Resource.Loading())
        val result = try {
            val documents = if (query.isBlank()) {
                dataSource.getBusinessDocumentsPaged("clientes", limit = 25)
            } else {
                dataSource.queryBusinessArrayContainsLimited("clientes", "searchTerms", query.lowercase().trim(), limit = 25)
            }
            val clientes = documents.map { (id, data) -> ClienteDto.fromMap(id, data).toDomain() }
            Resource.Success(clientes)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error("Error en búsqueda: ${e.message}")
        }
        emit(result)
    }
}
