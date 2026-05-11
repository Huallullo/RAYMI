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
 * Implementación del repositorio de vestuarios (items) con rutas SaaS por negocio.
 */
class VestuarioRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseDataSource
) : VestuarioRepository {

    override suspend fun getVestuarios(): Flow<Resource<List<Vestuario>>> {
        return dataSource.observeBusinessItemsOrderedLimited(
            orderByField = "createdAt",
            descending = true,
            limit = 500
        )
            .map { documents ->
                val items = documents
                    .map { (id, data) -> VestuarioDto.fromMap(id, data).toDomain() }
                Resource.Success(items) as Resource<List<Vestuario>>
            }
            .onStart { emit(Resource.Loading()) }
            .catch { e ->
                if (e.message?.contains("Usuario no autenticado") == true) {
                    emit(Resource.Error("Debe iniciar sesión para acceder a los datos"))
                } else {
                    emit(Resource.Error("Error al obtener vestuarios: ${e.message}"))
                }
            }
    }

    override suspend fun getVestuarioById(id: String): Flow<Resource<Vestuario>> = flow {
        try {
            emit(Resource.Loading())
            val data = dataSource.getBusinessDocument("items", id)
            if (data != null) {
                val vestuario = VestuarioDto.fromMap(id, data).toDomain()
                emit(Resource.Success(vestuario))
            } else {
                emit(Resource.Error("Vestuario no encontrado"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error("Error al obtener vestuario: ${e.message}"))
        }
    }

    override suspend fun getVestuariosByEstado(
        estado: EstadoVestuario
    ): Flow<Resource<List<Vestuario>>> = flow {
        try {
            emit(Resource.Loading())

            val documents = dataSource.queryBusinessDocuments(
                collection = "items",
                field = "estado",
                value = estado.name,
                limit = 300
            )

            val vestuarios = documents.map { (id, data) ->
                VestuarioDto.fromMap(id, data).toDomain()
            }.sortedBy { it.codigo }

            emit(Resource.Success(vestuarios))

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error("Error al obtener vestuarios por estado: ${e.message}"))
        }
    }

    override suspend fun searchVestuarioByCodigo(codigo: String): Flow<Resource<Vestuario?>> = flow {
        try {
            emit(Resource.Loading())
            val documents = dataSource.queryBusinessItemByCodigo(codigo, limit = 5)
            val vestuario = documents.firstOrNull()?.let { (id, data) ->
                VestuarioDto.fromMap(id, data).toDomain()
            }
            emit(Resource.Success(vestuario))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error("Error al buscar por código: ${e.message}"))
        }
    }

    override suspend fun addVestuario(vestuario: Vestuario): Flow<Resource<String>> = flow {
        try {
            emit(Resource.Loading())
            val dto = VestuarioDto.fromDomain(vestuario)
            val documentId = dataSource.addBusinessItemWithUniqueCodigo(
                itemData = dto.toMap(),
                codigoRaw = vestuario.codigo
            )
            emit(Resource.Success(documentId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val error = if (e.message?.contains("Ya existe un vestuario con este código") == true)
                "Ya existe un vestuario con el código ${vestuario.codigo}"
            else
                "Error al agregar vestuario: ${e.message}"
            emit(Resource.Error(error))
        }
    }

    override suspend fun updateVestuario(vestuario: Vestuario): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            val dto = VestuarioDto.fromDomain(vestuario)
            dataSource.updateBusinessDocument(
                collection = "items",
                documentId = vestuario.id,
                data = dto.toMap()
            )
            emit(Resource.Success(Unit))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error("Error al actualizar vestuario: ${e.message}"))
        }
    }

    override suspend fun updateEstadoVestuario(
        vestuarioId: String,
        estado: EstadoVestuario
    ): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())

            dataSource.updateBusinessDocument(
                collection = "items",
                documentId = vestuarioId,
                data = mapOf("estado" to estado.name)
            )

            emit(Resource.Success(Unit))

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error("Error al actualizar estado: ${e.message}"))
        }
    }

    override suspend fun deleteVestuario(vestuarioId: String): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())

            // Temporal: mientras no migramos alquileres, seguimos usando la colección global
            val alquileres = dataSource.queryDocumentsLimited(
                FirebaseDataSource.COLLECTION_ALQUILERES,
                "vestuarioId",
                vestuarioId,
                limit = 300
            )

            val tieneAlquileresActivos = alquileres.any { (_, data) ->
                data["estado"] == "ACTIVO"
            }

            if (tieneAlquileresActivos) {
                emit(Resource.Error("No se puede eliminar. El vestuario tiene alquileres activos"))
                return@flow
            }

            dataSource.deleteBusinessDocument(
                collection = "items",
                documentId = vestuarioId
            )

            emit(Resource.Success(Unit))

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error("Error al eliminar vestuario: ${e.message}"))
        }
    }

    override suspend fun searchVestuarios(query: String): Flow<Resource<List<Vestuario>>> = flow {
        try {
            emit(Resource.Loading())

            val q = query.trim().lowercase()
            if (q.isBlank()) {
                val documents = dataSource.getAllBusinessDocumentsOrderedLimited(
                    collection = "items",
                    orderByField = "codigo",
                    descending = false,
                    limit = 300
                )
                val vestuarios = documents
                    .map { (id, data) -> VestuarioDto.fromMap(id, data).toDomain() }
                emit(Resource.Success(vestuarios))
                return@flow
            }

            val documents = dataSource.queryBusinessArrayContainsLimited(
                collection = "items",
                field = "searchTerms",
                value = q,
                limit = 200
            )

            val vestuarios = documents
                .map { (id, data) -> VestuarioDto.fromMap(id, data).toDomain() }
                .sortedBy { it.codigo }

            emit(Resource.Success(vestuarios))

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error("Error al buscar vestuarios: ${e.message}"))
        }
    }
}