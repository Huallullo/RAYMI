package com.raymi.app.data.repository

import com.google.firebase.Timestamp
import com.raymi.app.data.model.dto.AlquilerDto
import com.raymi.app.data.remote.FirebaseDataSource
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.EstadoAlquiler
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.AlquilerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
    override suspend fun getAlquileres(): Flow<Resource<List<Alquiler>>> = flow {
        try {
            emit(Resource.Loading())

            val documents = dataSource.getAllDocuments(
                FirebaseDataSource.COLLECTION_ALQUILERES
            )

            val alquileres = documents.map { (id, data) ->
                AlquilerDto.fromMap(id, data).toDomain()
            }.sortedByDescending { it.createdAt }

            emit(Resource.Success(alquileres))

        } catch (e: Exception) {
            emit(Resource.Error("Error al obtener alquileres: ${e.message}"))
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

        } catch (e: Exception) {
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

        } catch (e: Exception) {
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

        } catch (e: Exception) {
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

            // Verificar que el vestuario esté disponible
            val vestuarioData = dataSource.getDocument(
                FirebaseDataSource.COLLECTION_VESTUARIOS,
                alquiler.vestuarioId
            )

            if (vestuarioData == null) {
                emit(Resource.Error("Vestuario no encontrado"))
                return@flow
            }

            val estadoVestuario = vestuarioData["estado"] as? String
            if (estadoVestuario != "DISPONIBLE") {
                emit(Resource.Error("El vestuario no está disponible"))
                return@flow
            }

            // Crear el alquiler
            val dto = AlquilerDto.fromDomain(alquiler)
            val documentId = dataSource.addDocument(
                FirebaseDataSource.COLLECTION_ALQUILERES,
                dto.toMap()
            )

            // Actualizar estado del vestuario a ALQUILADO
            dataSource.updateDocument(
                FirebaseDataSource.COLLECTION_VESTUARIOS,
                alquiler.vestuarioId,
                mapOf("estado" to "ALQUILADO")
            )

            emit(Resource.Success(documentId))

        } catch (e: Exception) {
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

        } catch (e: Exception) {
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

            // Obtener el alquiler
            val alquilerData = dataSource.getDocument(
                FirebaseDataSource.COLLECTION_ALQUILERES,
                alquilerId
            )

            if (alquilerData == null) {
                emit(Resource.Error("Alquiler no encontrado"))
                return@flow
            }

            val vestuarioId = alquilerData["vestuarioId"] as? String

            if (vestuarioId == null) {
                emit(Resource.Error("Vestuario no encontrado en el alquiler"))
                return@flow
            }

            // Actualizar alquiler a DEVUELTO con fecha de devolución
            dataSource.updateDocument(
                FirebaseDataSource.COLLECTION_ALQUILERES,
                alquilerId,
                mapOf(
                    "estado" to "DEVUELTO",
                    "fechaDevolucion" to Timestamp.now(),
                    "updatedAt" to Timestamp.now()
                )
            )

            // Liberar vestuario (DISPONIBLE)
            dataSource.updateDocument(
                FirebaseDataSource.COLLECTION_VESTUARIOS,
                vestuarioId,
                mapOf("estado" to "DISPONIBLE")
            )

            emit(Resource.Success(Unit))

        } catch (e: Exception) {
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

        } catch (e: Exception) {
            emit(Resource.Error("Error al eliminar alquiler: ${e.message}"))
        }
    }
}