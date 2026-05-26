package com.raymi.app.data.repository

import com.google.firebase.Timestamp
import com.raymi.app.core.utils.AppLogger
import com.raymi.app.core.workspace.WorkspaceManager
import com.raymi.app.data.model.dto.AlquilerDto
import com.raymi.app.data.remote.FirebaseDataSource
import com.raymi.app.data.remote.RentalDataSource
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.EstadoAlquiler
import com.raymi.app.domain.model.Pago
import com.raymi.app.domain.model.MetodoPago
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
    private val dataSource: FirebaseDataSource,
    private val rentalDataSource: RentalDataSource,
    private val workspaceManager: WorkspaceManager
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
            val workspaceId = workspaceManager.getWorkspaceId() ?: throw IllegalStateException("Negocio no seleccionado")
            val data = rentalDataSource.getAlquiler(workspaceId, id)
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

    override suspend fun getAlquileresByItem(
        itemId: String
    ): Flow<Resource<List<Alquiler>>> = flow {
        try {
            emit(Resource.Loading())
            // QA Fix: Buscar por itemId (nuevo) y vestuarioId (legacy)
            val byItemId = dataSource.queryBusinessAlquileres("itemId", itemId, 500)
            val byVestuarioId = dataSource.queryBusinessAlquileres("vestuarioId", itemId, 500)
            
            val allDocs = (byItemId + byVestuarioId).distinctBy { it.first }
            
            val alquileres = allDocs.map { (id, data) ->
                AlquilerDto.fromMap(id, data).toDomain()
            }.sortedByDescending { it.createdAt }
            emit(Resource.Success(alquileres))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error("Error al obtener alquileres del producto: ${e.message}"))
        }
    }

    override suspend fun createAlquiler(alquiler: Alquiler): Flow<Resource<String>> = flow {
        try {
            emit(Resource.Loading())
            val dto = AlquilerDto.fromDomain(alquiler)
            val dataMap = dto.toMap().filterValues { it != null }.mapValues { it.value!! }
            
            val documentId = rentalDataSource.createAlquilerTransactional(
                workspaceId = alquiler.workspaceId,
                alquilerData = dataMap,
                itemId = alquiler.itemId
            )

            emit(Resource.Success(documentId))
        } catch (e: Exception) {
            AppLogger.e("AlquilerRepo", "Error en creación transaccional: ${e.message}")
            emit(Resource.Error(e.message ?: "Error al crear alquiler"))
        }
    }

    override suspend fun updateAlquiler(alquiler: Alquiler): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            if (alquiler.id.isBlank()) {
                emit(Resource.Error("ID de alquiler inválido"))
                return@flow
            }
            val dto = AlquilerDto.fromDomain(alquiler.copy(updatedAt = Timestamp.now()))
            val dataMap = dto.toMap().filterValues { it != null }.mapValues { it.value!! }
            rentalDataSource.updateAlquiler(alquiler.workspaceId, alquiler.id, dataMap)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error("Error al actualizar alquiler: ${e.message}"))
        }
    }

    override suspend fun registrarDevolucion(alquilerId: String): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            val workspaceId = workspaceManager.getWorkspaceId() ?: throw IllegalStateException("Negocio no seleccionado")
            rentalDataSource.registrarDevolucionTransactional(workspaceId, alquilerId)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error("Error al registrar devolución: ${e.message}"))
        }
    }

    override suspend fun updateEstadoAlquiler(
        alquilerId: String,
        estado: EstadoAlquiler
    ): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            val workspaceId = workspaceManager.getWorkspaceId() ?: throw IllegalStateException("Negocio no seleccionado")
            
            val actual = rentalDataSource.getAlquiler(workspaceId, alquilerId)
            val estadoActualStr = actual?.get("estado") as? String ?: "ACTIVO"
            val estadoActual = try { EstadoAlquiler.valueOf(estadoActualStr) } catch(_: Exception) { EstadoAlquiler.ACTIVO }

            if (estadoActual == EstadoAlquiler.DEVUELTO || estadoActual == EstadoAlquiler.CANCELADO) {
                emit(Resource.Error("No se puede cambiar el estado de un alquiler ya finalizado"))
                return@flow
            }

            rentalDataSource.updateAlquiler(
                workspaceId,
                alquilerId,
                mapOf("estado" to estado.name, "updatedAt" to Timestamp.now())
            )
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error("Error al actualizar estado: ${e.message}"))
        }
    }

    override suspend fun deleteAlquiler(alquilerId: String): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            val workspaceId = workspaceManager.getWorkspaceId() ?: throw IllegalStateException("Negocio no seleccionado")
            rentalDataSource.deleteAlquiler(workspaceId, alquilerId)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error("Error al eliminar alquiler: ${e.message}"))
        }
    }

    override suspend fun addPago(
        workspaceId: String,
        alquilerId: String,
        pago: Pago
    ): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            val pagoData = mapOf(
                "alquilerId" to alquilerId,
                "monto" to pago.monto,
                "metodoPago" to pago.metodoPago.name,
                "referencia" to pago.referencia,
                "fecha" to pago.fecha
            )
            rentalDataSource.addPago(workspaceId, alquilerId, pagoData)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error("Error al registrar pago: ${e.message}"))
        }
    }

    override suspend fun getPagos(
        workspaceId: String,
        alquilerId: String
    ): Flow<Resource<List<Pago>>> = flow {
        try {
            emit(Resource.Loading())
            val docs = rentalDataSource.getPagos(workspaceId, alquilerId)
            val pagos = docs.map { data ->
                Pago(
                    id = data["id"] as? String ?: "",
                    alquilerId = data["alquilerId"] as? String ?: "",
                    monto = (data["monto"] as? Number)?.toDouble() ?: 0.0,
                    metodoPago = try { 
                        MetodoPago.valueOf(data["metodoPago"] as? String ?: "EFECTIVO") 
                    } catch (_: Exception) {
                        MetodoPago.EFECTIVO
                    },
                    referencia = data["referencia"] as? String ?: "",
                    fecha = data["fecha"] as? Timestamp ?: Timestamp.now()
                )
            }
            emit(Resource.Success(pagos))
        } catch (_: Exception) {
            emit(Resource.Error("Error al obtener historial de pagos"))
        }
    }
}
