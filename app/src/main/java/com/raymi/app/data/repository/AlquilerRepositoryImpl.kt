package com.raymi.app.data.repository

import com.google.firebase.Timestamp
import com.raymi.app.core.utils.AppLogger
import com.raymi.app.data.model.dto.AlquilerDto
import com.raymi.app.data.remote.FirebaseDataSource
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.EstadoAlquiler
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.AlquilerRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Implementación del repositorio de alquileres
 * Maneja todas las operaciones CRUD de alquileres con Firebase
 */
class AlquilerRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseDataSource
) : AlquilerRepository {

    /**
     * Obtiene todos los alquileres de Firebase
     * @return Flow con la lista de alquileres o error
     */
    override suspend fun getAlquileres(): Flow<Resource<List<Alquiler>>> {
        return dataSource.observeCollection(FirebaseDataSource.COLLECTION_ALQUILERES)
            .map { documents ->
                val alquileres = documents
                    .map { (id, data) -> AlquilerDto.fromMap(id, data).toDomain() }
                    .sortedByDescending { it.createdAt }
                Resource.Success(alquileres) as Resource<List<Alquiler>>
            }
            .onStart { emit(Resource.Loading()) }
            .catch { e ->
                // Manejar error de autenticación
                if (e.message?.contains("Usuario no autenticado") == true) {
                    emit(Resource.Error("Debe iniciar sesión para acceder a los datos"))
                } else {
                    emit(Resource.Error("Error al obtener alquileres: ${e.message}"))
                }
            }
    }

    /**
     * Obtiene un alquiler específico por su ID
     * @param id ID del alquiler
     * @return Flow con el alquiler o error
     */
    override suspend fun getAlquilerById(id: String): Flow<Resource<Alquiler>> = flow {
        try {
            emit(Resource.Loading())

            val data = dataSource.getDocument(
                FirebaseDataSource.COLLECTION_ALQUILERES,
                id
            )

            if (data != null) {
                val alquiler = AlquilerDto.fromMap(id, data).toDomain()
                emit(Resource.Success(alquiler))
            } else {
                emit(Resource.Error("Alquiler no encontrado"))
            }

        } catch (e: CancellationException) {
            throw e
        }catch (e: Exception) {
            emit(Resource.Error("Error al obtener alquiler: ${e.message}"))
        }
    }

    /**
     * Obtiene alquileres filtrados por estado
     * @param estado Estado del alquiler
     * @return Flow con la lista de alquileres
     */
    override suspend fun getAlquileresByEstado(
        estado: EstadoAlquiler
    ): Flow<Resource<List<Alquiler>>> = flow {
        try {
            emit(Resource.Loading())

            val documents = dataSource.queryDocuments(
                FirebaseDataSource.COLLECTION_ALQUILERES,
                "estado",
                estado.name
            )

            val alquileres = documents.map { (id, data) ->
                AlquilerDto.fromMap(id, data).toDomain()
            }.sortedByDescending { it.createdAt }

            emit(Resource.Success(alquileres))

        } catch (e: CancellationException) {
            throw e
        }catch (e: Exception) {
            emit(Resource.Error("Error al obtener alquileres por estado: ${e.message}"))
        }
    }

    /**
     * Obtiene todos los alquileres de un cliente específico
     * @param clienteId ID del cliente
     * @return Flow con la lista de alquileres del cliente
     */
    override suspend fun getAlquileresByCliente(
        clienteId: String
    ): Flow<Resource<List<Alquiler>>> = flow {
        try {
            emit(Resource.Loading())

            val documents = dataSource.queryDocuments(
                FirebaseDataSource.COLLECTION_ALQUILERES,
                "clienteId",
                clienteId
            )

            val alquileres = documents.map { (id, data) ->
                AlquilerDto.fromMap(id, data).toDomain()
            }.sortedByDescending { it.createdAt }

            emit(Resource.Success(alquileres))

        }catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error("Error al obtener alquileres del cliente: ${e.message}"))
        }
    }

    /**
     * Obtiene todos los alquileres de un vestuario específico
     * @param vestuarioId ID del vestuario
     * @return Flow con la lista de alquileres del vestuario
     */
    override suspend fun getAlquileresByVestuario(
        vestuarioId: String
    ): Flow<Resource<List<Alquiler>>> = flow {
        try {
            emit(Resource.Loading())

            val documents = dataSource.queryDocuments(
                FirebaseDataSource.COLLECTION_ALQUILERES,
                "vestuarioId",
                vestuarioId
            )

            val alquileres = documents.map { (id, data) ->
                AlquilerDto.fromMap(id, data).toDomain()
            }.sortedByDescending { it.createdAt }

            emit(Resource.Success(alquileres))

        } catch (e: CancellationException) {
            throw e
        }catch (e: Exception) {
            emit(Resource.Error("Error al obtener alquileres del vestuario: ${e.message}"))
        }
    }

    /**
     * Crea un nuevo alquiler
     * Actualiza el estado del vestuario a ALQUILADO
     * @param alquiler Alquiler a crear
     * @return Flow con el ID del alquiler creado o error
     */
    override suspend fun createAlquiler(alquiler: Alquiler): Flow<Resource<String>> = flow {
        try {
            emit(Resource.Loading())

            val dto = AlquilerDto.fromDomain(alquiler)
            val documentId = dataSource.createAlquilerAndMarkVestuarioAlquilado(
                alquilerData = dto.toMap(),
                vestuarioId = alquiler.vestuarioId
            )

            emit(Resource.Success(documentId))

        }catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e(
                tag = "AlquilerRepository",
                message = "Error al crear alquiler. vestuarioId=${alquiler.vestuarioId}, clienteId=${alquiler.clienteId}",
                throwable = e
            )
            emit(Resource.Error("Error al crear alquiler: ${e.message}"))
        }
    }

    /**
     * Actualiza un alquiler existente
     * @param alquiler Alquiler con los datos actualizados
     * @return Flow con el resultado de la operación
     */
    override suspend fun updateAlquiler(alquiler: Alquiler): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())

            if (alquiler.id.isBlank()) {
                emit(Resource.Error("ID de alquiler inválido"))
                return@flow
            }

            // Actualizar con timestamp actual
            val alquilerActualizado = alquiler.copy(updatedAt = Timestamp.now())
            val dto = AlquilerDto.fromDomain(alquilerActualizado)

            dataSource.updateDocument(
                FirebaseDataSource.COLLECTION_ALQUILERES,
                alquiler.id,
                dto.toMap()
            )

            emit(Resource.Success(Unit))

        } catch (e: CancellationException) {
            throw e
        }catch (e: Exception) {
            emit(Resource.Error("Error al actualizar alquiler: ${e.message}"))
        }
    }

    /**
     * Registra la devolución de un alquiler
     * Actualiza el estado a DEVUELTO y libera el vestuario
     * @param alquilerId ID del alquiler
     * @return Flow con el resultado de la operación
     */
    override suspend fun registrarDevolucion(alquilerId: String): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())

            dataSource.registrarDevolucionAtomica(alquilerId)

            emit(Resource.Success(Unit))

        } catch (e: CancellationException) {
            throw e
        }catch (e: Exception) {
            AppLogger.e(
                tag = "AlquilerRepository",
                message = "Error al registrar devolución. alquilerId=$alquilerId",
                throwable = e
            )
            emit(Resource.Error("Error al registrar devolución: ${e.message}"))
        }
    }

    /**
     * Actualiza solo el estado de un alquiler
     * @param alquilerId ID del alquiler
     * @param estado Nuevo estado
     * @return Flow con el resultado de la operación
     */
    override suspend fun updateEstadoAlquiler(
        alquilerId: String,
        estado: EstadoAlquiler
    ): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())

            dataSource.updateDocument(
                FirebaseDataSource.COLLECTION_ALQUILERES,
                alquilerId,
                mapOf(
                    "estado" to estado.name,
                    "updatedAt" to Timestamp.now()
                )
            )

            emit(Resource.Success(Unit))

        }catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error("Error al actualizar estado: ${e.message}"))
        }
    }

    /**
     * Elimina un alquiler de Firebase
     * @param alquilerId ID del alquiler a eliminar
     * @return Flow con el resultado de la operación
     */
    override suspend fun deleteAlquiler(alquilerId: String): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())

            dataSource.deleteDocument(
                FirebaseDataSource.COLLECTION_ALQUILERES,
                alquilerId
            )

            emit(Resource.Success(Unit))

        } catch (e: CancellationException) {
            throw e
        }catch (e: Exception) {
            emit(Resource.Error("Error al eliminar alquiler: ${e.message}"))
        }
    }
}
