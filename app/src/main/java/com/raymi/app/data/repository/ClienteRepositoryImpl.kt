package com.raymi.app.data.repository

import com.raymi.app.data.model.dto.ClienteDto
import com.raymi.app.data.remote.ClientDataSource
import com.raymi.app.data.remote.FirebaseDataSource
import com.raymi.app.data.remote.ObserverDataSource
import com.raymi.app.domain.model.Cliente
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.ClienteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class ClienteRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseDataSource,
    private val clientDataSource: ClientDataSource,
    private val observerDataSource: ObserverDataSource
) : ClienteRepository {

    override suspend fun getClientes(): Flow<Resource<List<Cliente>>> = flow {
        emit(Resource.Loading())
        try {
            val workspaceId = dataSource.getCurrentBusinessId()
            // Cambio Senior: Usar una sola lectura para Clientes. 
            // Rara vez necesitas ver un cliente nuevo "aparecer" mágicamente sin recargar.
            val documents = dataSource.getAllBusinessDocumentsOrderedLimited(
                collection = "clientes",
                orderByField = "createdAt",
                descending = true,
                limit = 50 // Bajamos de 500 a 50 (Paginación pendiente)
            )
            val clientes = documents.map { (id, data) -> ClienteDto.fromMap(id, data).toDomain() }
            emit(Resource.Success(clientes))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error al obtener clientes"))
        }
    }

    override suspend fun getClienteById(id: String): Flow<Resource<Cliente>> = flow {
        try {
            emit(Resource.Loading())
            val data = dataSource.getBusinessDocument("clientes", id)
            if (data != null) {
                emit(Resource.Success(ClienteDto.fromMap(id, data).toDomain()))
            } else {
                emit(Resource.Error("Cliente no encontrado"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error("Error al obtener cliente: ${e.message}"))
        }
    }

    override suspend fun searchClienteByDni(dni: String): Flow<Resource<Cliente?>> = flow {
        try {
            emit(Resource.Loading())
            val documents = dataSource.queryBusinessDocuments(
                collection = "clientes",
                field = "dni",
                value = dni,
                limit = 20
            )
            if (documents.isNotEmpty()) {
                val (id, data) = documents.first()
                emit(Resource.Success(ClienteDto.fromMap(id, data).toDomain()))
            } else {
                emit(Resource.Success(null))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error("Error al buscar cliente: ${e.message}"))
        }
    }

    override suspend fun addCliente(cliente: Cliente): Flow<Resource<String>> = flow {
        try {
            emit(Resource.Loading())
            val dto = ClienteDto.fromDomain(cliente)
            val documentId = clientDataSource.addClienteTransactional(
                workspaceId = cliente.workspaceId,
                clienteData = dto.toMap(),
                dni = cliente.dni
            )
            emit(Resource.Success(documentId))
        } catch (e: Exception) {
            val msg = if (e.message?.contains("Ya existe") == true) {
                "El DNI ${cliente.dni} ya está registrado en tu negocio."
            } else {
                "Error al registrar cliente: ${e.message}"
            }
            emit(Resource.Error(msg))
        }
    }

    override suspend fun updateCliente(cliente: Cliente): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            if (cliente.id.isBlank()) {
                emit(Resource.Error("ID de cliente inválido"))
                return@flow
            }
            val dto = ClienteDto.fromDomain(cliente)
            dataSource.updateBusinessDocument(
                collection = "clientes",
                documentId = cliente.id,
                data = dto.toMap()
            )
            emit(Resource.Success(Unit))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error("Error al actualizar cliente: ${e.message}"))
        }
    }

    override suspend fun deleteCliente(clienteId: String): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())

            val alquileres = dataSource.queryBusinessDocuments(
                collection = "alquileres",
                field = "clienteId",
                value = clienteId,
                limit = 300
            )
            if (alquileres.any { (_, data) -> data["estado"] == "ACTIVO" }) {
                emit(Resource.Error("No se puede eliminar. El cliente tiene alquileres activos"))
                return@flow
            }

            // Limpiar índice DNI
            val clienteData = dataSource.getBusinessDocument("clientes", clienteId)
            val dni = clienteData?.get("dni") as? String
            if (!dni.isNullOrBlank()) {
                dataSource.deleteBusinessDocument("clientes_dni_index", dni)
            }

            dataSource.deleteBusinessDocument(
                collection = "clientes",
                documentId = clienteId
            )
            emit(Resource.Success(Unit))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error("Error al eliminar cliente: ${e.message}"))
        }
    }

    override suspend fun searchClientes(query: String): Flow<Resource<List<Cliente>>> = flow {
        try {
            emit(Resource.Loading())

            val q = query.trim().lowercase()

            if (q.isBlank()) {
                val documents = dataSource.getAllBusinessDocumentsOrderedLimited(
                    collection = "clientes",
                    orderByField = "createdAt",
                    descending = true,
                    limit = 300
                )
                val clientes = documents
                    .map { (id, data) -> ClienteDto.fromMap(id, data).toDomain() }
                emit(Resource.Success(clientes))
                return@flow
            }

            if (q.length >= 2) {
                val documents = dataSource.queryBusinessArrayContainsLimited(
                    collection = "clientes",
                    field = "searchTerms",
                    value = q,
                    limit = 200
                )
                val clientes = documents
                    .map { (id, data) -> ClienteDto.fromMap(id, data).toDomain() }
                    .sortedByDescending { it.createdAt }
                emit(Resource.Success(clientes))
            } else {
                val documents = dataSource.getAllBusinessDocumentsOrderedLimited(
                    collection = "clientes",
                    orderByField = "createdAt",
                    descending = true,
                    limit = 300
                )
                val clientes = documents
                    .map { (id, data) -> ClienteDto.fromMap(id, data).toDomain() }
                    .filter { c ->
                        c.nombre.startsWith(q, ignoreCase = true) ||
                                c.apellidos.startsWith(q, ignoreCase = true) ||
                                c.dni.startsWith(q)
                    }
                    .sortedByDescending { it.createdAt }
                emit(Resource.Success(clientes))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error("Error al buscar clientes: ${e.message}"))
        }
    }
}