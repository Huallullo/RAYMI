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
 * Implementación del repositorio de alquileres con rutas SaaS (negocios/{negocioId}/alquileres)
 */
class AlquilerRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseDataSource
) : AlquilerRepository {

    override suspend fun getAlquileres(workspaceId: String): Flow<Resource<List<Alquiler>>> {
        return dataSource.observeBusinessAlquileresOrderedLimited(
            orderByField = "createdAt",
            descending = true,
            limit = 500,
            negocioId = workspaceId
        )
            .map { documents ->
                val alquileres = documents
                    .map { (id, data) -> AlquilerDto.fromMap(id, data).toDomain() }
                Resource.Success(alquileres) as Resource<List<Alquiler>>
            }
            .onStart { emit(Resource.Loading()) }
            .catch { e ->
                if (e.message?.contains("Usuario no autenticado") == true) {
                    emit(Resource.Error("Debe iniciar sesión para acceder a los datos"))
                } else {
                    emit(Resource.Error("Error al obtener alquileres: ${e.message}"))
                }
            }
    }

    override suspend fun getAlquilerById(id: String): Flow<Resource<Alquiler>> = flow {
        try {
            emit(Resource.Loading())
            val data = dataSource.getBusinessAlquiler(id)
            if (data != null) {
                val alquiler = AlquilerDto.fromMap(id, data).toDomain()
                emit(Resource.Success(alquiler))
            } else {
                emit(Resource.Error("Alquiler no encontrado"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error("Error al obtener alquiler: ${e.message}"))
        }
    }

    override suspend fun getAlquileresByEstado(
        estado: EstadoAlquiler
    ): Flow<Resource<List<Alquiler>>> = flow {
        try {
            emit(Resource.Loading())
            val documents = dataSource.queryBusinessAlquileres(
                field = "estado",
                value = estado.name,
                limit = 300
            )
            val alquileres = documents.map { (id, data) ->
                AlquilerDto.fromMap(id, data).toDomain()
            }.sortedByDescending { it.createdAt }
            emit(Resource.Success(alquileres))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error("Error al obtener alquileres por estado: ${e.message}"))
        }
    }

    override suspend fun getAlquileresByCliente(
        clienteId: String
    ): Flow<Resource<List<Alquiler>>> = flow {
        try {
            emit(Resource.Loading())
            val documents = dataSource.queryBusinessAlquileres(
                field = "clienteId",
                value = clienteId,
                limit = 300
            )
            val alquileres = documents.map { (id, data) ->
                AlquilerDto.fromMap(id, data).toDomain()
            }.sortedByDescending { it.createdAt }
            emit(Resource.Success(alquileres))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error("Error al obtener alquileres del cliente: ${e.message}"))
        }
    }

    override suspend fun getAlquileresByVestuario(
        vestuarioId: String
    ): Flow<Resource<List<Alquiler>>> = flow {
        try {
            emit(Resource.Loading())
            // QA Fix: Buscar poritemId (nuevo) y vestuarioId (legacy)
            val byItemId = dataSource.queryBusinessAlquileres("itemId", vestuarioId, 500)
            val byVestuarioId = dataSource.queryBusinessAlquileres("vestuarioId", vestuarioId, 500)
            
            val allDocs = (byItemId + byVestuarioId).distinctBy { it.first }
            
            val alquileres = allDocs.map { (id, data) ->
                AlquilerDto.fromMap(id, data).toDomain()
            }.sortedByDescending { it.createdAt }
            emit(Resource.Success(alquileres))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error("Error al obtener alquileres del vestuario: ${e.message}"))
        }
    }

    override suspend fun createAlquiler(alquiler: Alquiler): Flow<Resource<String>> = flow {
        try {
            emit(Resource.Loading())
            val dto = AlquilerDto.fromDomain(alquiler)
            val documentId = dataSource.addBusinessAlquiler(
                alquilerData = dto.toMap().filterValues { it != null }.mapValues { it.value!! },
                itemId = alquiler.itemId
            )
            
            // Actualización de estadísticas atómicas
            dataSource.updateStats(alquiler.workspaceId, "alquileresActivos", 1L)

            emit(Resource.Success(documentId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e(
                tag = "AlquilerRepository",
                message = "Error al crear alquiler. itemId=${alquiler.itemId}, clienteId=${alquiler.clienteId}",
                throwable = e
            )
            emit(Resource.Error("Error al crear alquiler: ${e.message}"))
        }
    }

    override suspend fun updateAlquiler(alquiler: Alquiler): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            if (alquiler.id.isBlank()) {
                emit(Resource.Error("ID de alquiler inválido"))
                return@flow
            }
            val alquilerActualizado = alquiler.copy(updatedAt = Timestamp.now())
            val dto = AlquilerDto.fromDomain(alquilerActualizado)
            val dataMap = dto.toMap().filterValues { it != null }.mapValues { it.value!! }
            dataSource.updateBusinessAlquiler(alquiler.id, dataMap)
            emit(Resource.Success(Unit))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error("Error al actualizar alquiler: ${e.message}"))
        }
    }

    override suspend fun registrarDevolucion(alquilerId: String): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            
            // Verificación Senior: No permitir devolución si hay saldo pendiente
            val alquilerSnapshot = dataSource.getBusinessAlquiler(alquilerId)
            val saldo = (alquilerSnapshot?.get("saldo") as? Number)?.toDouble() ?: 0.0
            
            if (saldo > 0.01) { // Pequeño margen para errores de precisión double
                emit(Resource.Error("No se puede registrar devolución: Existe un saldo pendiente de S/. $saldo"))
                return@flow
            }

            val workspaceId = alquilerSnapshot?.get("workspaceId") as? String
            
            dataSource.registrarDevolucionBusiness(alquilerId)
            
            // Actualización de estadísticas atómicas
            if (workspaceId != null) {
                dataSource.updateStats(workspaceId, "alquileresActivos", -1L)
            }

            emit(Resource.Success(Unit))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e(
                tag = "AlquilerRepository",
                message = "Error al registrar devolución. alquilerId=$alquilerId",
                throwable = e
            )
            emit(Resource.Error("Error al registrar devolución: ${e.message}"))
        }
    }

    override suspend fun updateEstadoAlquiler(
        alquilerId: String,
        estado: EstadoAlquiler
    ): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            
            // Validar transición (Regla Senior)
            val actual = dataSource.getBusinessAlquiler(alquilerId)
            val estadoActualStr = actual?.get("estado") as? String ?: "ACTIVO"
            val estadoActual = try { EstadoAlquiler.valueOf(estadoActualStr) } catch(_: Exception) { EstadoAlquiler.ACTIVO }

            if (estadoActual == EstadoAlquiler.DEVUELTO || estadoActual == EstadoAlquiler.CANCELADO) {
                emit(Resource.Error("No se puede cambiar el estado de un alquiler ya finalizado (${estadoActual.name})"))
                return@flow
            }

            dataSource.updateBusinessAlquiler(
                alquilerId,
                mapOf(
                    "estado" to estado.name,
                    "updatedAt" to Timestamp.now()
                )
            )
            emit(Resource.Success(Unit))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error("Error al actualizar estado: ${e.message}"))
        }
    }

    override suspend fun deleteAlquiler(alquilerId: String): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            dataSource.deleteBusinessAlquiler(alquilerId)
            emit(Resource.Success(Unit))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error("Error al eliminar alquiler: ${e.message}"))
        }
    }
}