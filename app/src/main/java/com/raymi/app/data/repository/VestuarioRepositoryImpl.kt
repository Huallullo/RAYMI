package com.raymi.app.data.repository

import com.raymi.app.data.model.dto.VestuarioDto
import com.raymi.app.data.remote.FirebaseDataSource
import com.raymi.app.domain.model.EstadoVestuario
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.Vestuario
import com.raymi.app.domain.repository.VestuarioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Implementación del repositorio de vestuarios
 * Maneja todas las operaciones CRUD de vestuarios con Firebase
 */
class VestuarioRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseDataSource
) : VestuarioRepository {

    /**
     * Obtiene todos los vestuarios de Firebase
     * @return Flow con la lista de vestuarios o error
     */
    override suspend fun getVestuarios(): Flow<Resource<List<Vestuario>>> {
        return dataSource.observeCollection(FirebaseDataSource.COLLECTION_VESTUARIOS)
            .map { documents ->
                val vestuarios = documents
                    .map { (id, data) -> VestuarioDto.fromMap(id, data).toDomain() }
                    .sortedBy { it.codigo }
                Resource.Success(vestuarios) as Resource<List<Vestuario>>
            }
            .onStart { emit(Resource.Loading()) }
            .catch { e -> emit(Resource.Error("Error al obtener vestuarios: ${e.message}")) }
    }

    /**
     * Obtiene un vestuario específico por su ID
     * @param id ID del vestuario
     * @return Flow con el vestuario o error
     */
    override suspend fun getVestuarioById(id: String): Flow<Resource<Vestuario>> = flow {
        try {
            emit(Resource.Loading())

            val data = dataSource.getDocument(
                FirebaseDataSource.COLLECTION_VESTUARIOS,
                id
            )

            if (data != null) {
                val vestuario = VestuarioDto.fromMap(id, data).toDomain()
                emit(Resource.Success(vestuario))
            } else {
                emit(Resource.Error("Vestuario no encontrado"))
            }

        } catch (e: CancellationException) {
            throw e
        }catch (e: Exception) {
            emit(Resource.Error("Error al obtener vestuario: ${e.message}"))
        }
    }

    /**
     * Obtiene vestuarios filtrados por estado
     * @param estado Estado del vestuario
     * @return Flow con la lista de vestuarios
     */
    override suspend fun getVestuariosByEstado(
        estado: EstadoVestuario
    ): Flow<Resource<List<Vestuario>>> = flow {
        try {
            emit(Resource.Loading())

            val documents = dataSource.queryDocuments(
                FirebaseDataSource.COLLECTION_VESTUARIOS,
                "estado",
                estado.name
            )

            val vestuarios = documents.map { (id, data) ->
                VestuarioDto.fromMap(id, data).toDomain()
            }.sortedBy { it.codigo }

            emit(Resource.Success(vestuarios))

        } catch (e: CancellationException) {
            throw e
        }catch (e: Exception) {
            emit(Resource.Error("Error al obtener vestuarios por estado: ${e.message}"))
        }
    }

    /**
     * Busca un vestuario por su código
     * @param codigo Código del vestuario
     * @return Flow con el vestuario encontrado o null
     */
    override suspend fun searchVestuarioByCodigo(
        codigo: String
    ): Flow<Resource<Vestuario?>> = flow {
        try {
            emit(Resource.Loading())

            val documents = dataSource.queryDocuments(
                FirebaseDataSource.COLLECTION_VESTUARIOS,
                "codigo",
                codigo
            )

            if (documents.isNotEmpty()) {
                val (id, data) = documents.first()
                val vestuario = VestuarioDto.fromMap(id, data).toDomain()
                emit(Resource.Success(vestuario))
            } else {
                emit(Resource.Success(null))
            }

        }catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error("Error al buscar vestuario: ${e.message}"))
        }
    }

    /**
     * Agrega un nuevo vestuario a Firebase
     * @param vestuario Vestuario a agregar
     * @return Flow con el ID del vestuario creado o error
     */
    override suspend fun addVestuario(vestuario: Vestuario): Flow<Resource<String>> = flow {
        try {
            emit(Resource.Loading())

            val dto = VestuarioDto.fromDomain(vestuario)
            val documentId = dataSource.addVestuarioWithUniqueCodigo(
                vestuarioData = dto.toMap(),
                codigoRaw = vestuario.codigo
            )

            emit(Resource.Success(documentId))

        } catch (e: CancellationException) {
            throw e
        }catch (e: Exception) {
            emit(Resource.Error("Error al agregar vestuario: ${e.message}"))
        }
    }

    /**
     * Actualiza un vestuario existente
     * @param vestuario Vestuario con los datos actualizados
     * @return Flow con el resultado de la operación
     */
    override suspend fun updateVestuario(vestuario: Vestuario): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())

            if (vestuario.id.isBlank()) {
                emit(Resource.Error("ID de vestuario inválido"))
                return@flow
            }

            val dto = VestuarioDto.fromDomain(vestuario)
            dataSource.updateDocument(
                FirebaseDataSource.COLLECTION_VESTUARIOS,
                vestuario.id,
                dto.toMap()
            )

            emit(Resource.Success(Unit))

        } catch (e: CancellationException) {
            throw e
        }catch (e: Exception) {
            emit(Resource.Error("Error al actualizar vestuario: ${e.message}"))
        }
    }

    /**
     * Actualiza solo el estado de un vestuario
     * @param vestuarioId ID del vestuario
     * @param estado Nuevo estado
     * @return Flow con el resultado de la operación
     */
    override suspend fun updateEstadoVestuario(
        vestuarioId: String,
        estado: EstadoVestuario
    ): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())

            dataSource.updateDocument(
                FirebaseDataSource.COLLECTION_VESTUARIOS,
                vestuarioId,
                mapOf("estado" to estado.name)
            )

            emit(Resource.Success(Unit))

        } catch (e: CancellationException) {
            throw e
        }catch (e: Exception) {
            emit(Resource.Error("Error al actualizar estado: ${e.message}"))
        }
    }

    /**
     * Elimina un vestuario de Firebase
     * @param vestuarioId ID del vestuario a eliminar
     * @return Flow con el resultado de la operación
     */
    override suspend fun deleteVestuario(vestuarioId: String): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())

            // Verificar que no tenga alquileres activos
            val alquileres = dataSource.queryDocuments(
                FirebaseDataSource.COLLECTION_ALQUILERES,
                "vestuarioId",
                vestuarioId
            )

            val tieneAlquileresActivos = alquileres.any { (_, data) ->
                data["estado"] == "ACTIVO"
            }

            if (tieneAlquileresActivos) {
                emit(Resource.Error("No se puede eliminar. El vestuario tiene alquileres activos"))
                return@flow
            }

            dataSource.deleteDocument(
                FirebaseDataSource.COLLECTION_VESTUARIOS,
                vestuarioId
            )

            emit(Resource.Success(Unit))

        } catch (e: CancellationException) {
            throw e
        }catch (e: Exception) {
            emit(Resource.Error("Error al eliminar vestuario: ${e.message}"))
        }
    }

    /**
     * Busca vestuarios por danza, departamento o descripción
     * @param query Texto a buscar
     * @return Flow con la lista de vestuarios que coinciden
     */
    override suspend fun searchVestuarios(query: String): Flow<Resource<List<Vestuario>>> = flow {
        try {
            emit(Resource.Loading())

            val q = query.trim().lowercase()
            if (q.isBlank()) {
                val documents = dataSource.getAllDocuments(FirebaseDataSource.COLLECTION_VESTUARIOS)
                val vestuarios = documents
                    .map { (id, data) -> VestuarioDto.fromMap(id, data).toDomain() }
                    .sortedBy { it.codigo }

                emit(Resource.Success(vestuarios))
                return@flow
            }

            val documents = dataSource.queryArrayContains(
                collection = FirebaseDataSource.COLLECTION_VESTUARIOS,
                field = "searchTerms",
                value = q
            )

            val vestuarios = documents
                .map { (id, data) -> VestuarioDto.fromMap(id, data).toDomain() }
                .sortedBy { it.codigo }

            emit(Resource.Success(vestuarios))

        } catch (e: CancellationException) {
            throw e
        }catch (e: Exception) {
            emit(Resource.Error("Error al buscar vestuarios: ${e.message}"))
        }
    }
}