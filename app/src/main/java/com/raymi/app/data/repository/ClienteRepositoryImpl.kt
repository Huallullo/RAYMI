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

/**
 * Implementación del repositorio de clientes
 * Maneja todas las operaciones CRUD de clientes con Firebase
 */
class ClienteRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseDataSource
) : ClienteRepository {

    /**
     * Obtiene todos los clientes de Firebase
     * @return Flow con la lista de clientes o error
     */
    override suspend fun getClientes(): Flow<Resource<List<Cliente>>> {
        return dataSource.observeCollection(FirebaseDataSource.COLLECTION_CLIENTES)
            .map { documents ->
                val source = if (documents.isEmpty()) {
                    dataSource.getAllDocuments(FirebaseDataSource.COLLECTION_CLIENTES)
                } else {
                    documents
                }

                val clientes = source
                    .map { (id, data) -> ClienteDto.fromMap(id, data).toDomain() }
                    .sortedByDescending { it.createdAt }

                Resource.Success(clientes) as Resource<List<Cliente>>
            }
            .onStart { emit(Resource.Loading()) }
            .catch { e -> emit(Resource.Error("Error al obtener clientes: ${e.message}")) }
    }
    /**
     * Obtiene un cliente específico por su ID
     * @param id ID del cliente
     * @return Flow con el cliente o error
     */
    override suspend fun getClienteById(id: String): Flow<Resource<Cliente>> = flow {
        try {
            emit(Resource.Loading())

            val data = dataSource.getDocument(
                FirebaseDataSource.COLLECTION_CLIENTES,
                id
            )

            if (data != null) {
                val cliente = ClienteDto.fromMap(id, data).toDomain()
                emit(Resource.Success(cliente))
            } else {
                emit(Resource.Error("Cliente no encontrado"))
            }

        }catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error("Error al obtener cliente: ${e.message}"))
        }
    }

    /**
     * Busca un cliente por su DNI
     * @param dni DNI del cliente
     * @return Flow con el cliente encontrado o null
     */
    override suspend fun searchClienteByDni(dni: String): Flow<Resource<Cliente?>> = flow {
        try {
            emit(Resource.Loading())

            val documents = dataSource.queryDocuments(
                FirebaseDataSource.COLLECTION_CLIENTES,
                "dni",
                dni
            )

            if (documents.isNotEmpty()) {
                val (id, data) = documents.first()
                val cliente = ClienteDto.fromMap(id, data).toDomain()
                emit(Resource.Success(cliente))
            } else {
                emit(Resource.Success(null))
            }

        }catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error("Error al buscar cliente: ${e.message}"))
        }
    }

    /**
     * Agrega un nuevo cliente a Firebase
     * @param cliente Cliente a agregar
     * @return Flow con el ID del cliente creado o error
     */
    override suspend fun addCliente(cliente: Cliente): Flow<Resource<String>> = flow {
        try {
            emit(Resource.Loading())

            val dto = ClienteDto.fromDomain(cliente)
            val documentId = dataSource.addClienteWithUniqueDni(
                clienteData = dto.toMap(),
                dniRaw = cliente.dni
            )

            emit(Resource.Success(documentId))

        } catch (e: CancellationException) {
            throw e
        }catch (e: Exception) {
            emit(Resource.Error("Error al agregar cliente: ${e.message}"))
        }
    }

    /**
     * Actualiza un cliente existente
     * @param cliente Cliente con los datos actualizados
     * @return Flow con el resultado de la operación
     */
    override suspend fun updateCliente(cliente: Cliente): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())

            if (cliente.id.isBlank()) {
                emit(Resource.Error("ID de cliente inválido"))
                return@flow
            }

            // Convertir a DTO y actualizar
            val dto = ClienteDto.fromDomain(cliente)
            dataSource.updateDocument(
                FirebaseDataSource.COLLECTION_CLIENTES,
                cliente.id,
                dto.toMap()
            )

            emit(Resource.Success(Unit))

        } catch (e: CancellationException) {
            throw e
        }catch (e: Exception) {
            emit(Resource.Error("Error al actualizar cliente: ${e.message}"))
        }
    }

    /**
     * Elimina un cliente de Firebase
     * @param clienteId ID del cliente a eliminar
     * @return Flow con el resultado de la operación
     */
    override suspend fun deleteCliente(clienteId: String): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())

            // Verificar que no tenga alquileres activos
            val alquileres = dataSource.queryDocuments(
                FirebaseDataSource.COLLECTION_ALQUILERES,
                "clienteId",
                clienteId
            )

            val tieneAlquileresActivos = alquileres.any { (_, data) ->
                data["estado"] == "ACTIVO"
            }

            if (tieneAlquileresActivos) {
                emit(Resource.Error("No se puede eliminar. El cliente tiene alquileres activos"))
                return@flow
            }

            // Eliminar cliente
            dataSource.deleteDocument(
                FirebaseDataSource.COLLECTION_CLIENTES,
                clienteId
            )

            emit(Resource.Success(Unit))

        } catch (e: CancellationException) {
            throw e
        }catch (e: Exception) {
            emit(Resource.Error("Error al eliminar cliente: ${e.message}"))
        }
    }

    /**
     * Busca clientes por nombre o apellidos
     * @param query Texto a buscar
     * @return Flow con la lista de clientes que coinciden
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

            val documents = dataSource.queryArrayContains(
                collection = FirebaseDataSource.COLLECTION_CLIENTES,
                field = "searchTerms",
                value = q
            )

            val clientes = documents
                .map { (id, data) -> ClienteDto.fromMap(id, data).toDomain() }
                .sortedByDescending { it.createdAt }

            emit(Resource.Success(clientes))

        } catch (e: CancellationException) {
            throw e
        }catch (e: Exception) {
            emit(Resource.Error("Error al buscar clientes: ${e.message}"))
        }
    }
}