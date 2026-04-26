package com.raymi.app.data.repository

import com.raymi.app.data.model.dto.ClienteDto
import com.raymi.app.data.remote.FirebaseDataSource
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
    private val dataSource: FirebaseDataSource
) : ClienteRepository {

    /**
     * Observa los clientes en tiempo real mediante Firestore snapshots.
     * ✅ FIX: eliminado el doble-fetch innecesario (getAllDocuments dentro del map).
     */
    override suspend fun getClientes(): Flow<Resource<List<Cliente>>> {
        return dataSource.observeCollection(FirebaseDataSource.COLLECTION_CLIENTES)
            .map { documents ->
                val clientes = documents
                    .map { (id, data) -> ClienteDto.fromMap(id, data).toDomain() }
                    .sortedByDescending { it.createdAt }
                Resource.Success(clientes) as Resource<List<Cliente>>
            }
            .onStart { emit(Resource.Loading()) }
            .catch { e ->
                if (e.message?.contains("Usuario no autenticado") == true) {
                    emit(Resource.Error("Debe iniciar sesión para acceder a los datos"))
                } else {
                    emit(Resource.Error("Error al obtener clientes: ${e.message}"))
                }
            }
    }

    override suspend fun getClienteById(id: String): Flow<Resource<Cliente>> = flow {
        try {
            emit(Resource.Loading())
            val data = dataSource.getDocument(FirebaseDataSource.COLLECTION_CLIENTES, id)
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
            val documents = dataSource.queryDocuments(
                FirebaseDataSource.COLLECTION_CLIENTES, "dni", dni
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
            val dto        = ClienteDto.fromDomain(cliente)
            val documentId = dataSource.addClienteWithUniqueDni(
                clienteData = dto.toMap(),
                dniRaw      = cliente.dni
            )
            emit(Resource.Success(documentId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error("Error al agregar cliente: ${e.message}"))
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
            dataSource.updateDocument(
                FirebaseDataSource.COLLECTION_CLIENTES, cliente.id, dto.toMap()
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

            val alquileres = dataSource.queryDocuments(
                FirebaseDataSource.COLLECTION_ALQUILERES, "clienteId", clienteId
            )
            if (alquileres.any { (_, data) -> data["estado"] == "ACTIVO" }) {
                emit(Resource.Error("No se puede eliminar. El cliente tiene alquileres activos"))
                return@flow
            }

            dataSource.deleteDocument(FirebaseDataSource.COLLECTION_CLIENTES, clienteId)
            emit(Resource.Success(Unit))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error("Error al eliminar cliente: ${e.message}"))
        }
    }

    /**
     * Búsqueda local cuando la query es corta o contiene caracteres especiales,
     * y búsqueda por searchTerms en Firestore para queries de 2+ caracteres.
     */
    override suspend fun searchClientes(query: String): Flow<Resource<List<Cliente>>> = flow {
        try {
            emit(Resource.Loading())

            val q = query.trim().lowercase()

            if (q.isBlank()) {
                val documents = dataSource.getAllDocuments(FirebaseDataSource.COLLECTION_CLIENTES)
                val clientes = documents
                    .map { (id, data) -> ClienteDto.fromMap(id, data).toDomain() }
                    .sortedByDescending { it.createdAt }
                emit(Resource.Success(clientes))
                return@flow
            }

            // Usar searchTerms (índice de prefijos) para queries de 2+ chars
            if (q.length >= 2) {
                val documents = dataSource.queryArrayContains(
                    collection = FirebaseDataSource.COLLECTION_CLIENTES,
                    field      = "searchTerms",
                    value      = q
                )
                val clientes = documents
                    .map { (id, data) -> ClienteDto.fromMap(id, data).toDomain() }
                    .sortedByDescending { it.createdAt }
                emit(Resource.Success(clientes))
            } else {
                // Para queries de 1 carácter, búsqueda local sobre todos los clientes
                val documents = dataSource.getAllDocuments(FirebaseDataSource.COLLECTION_CLIENTES)
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