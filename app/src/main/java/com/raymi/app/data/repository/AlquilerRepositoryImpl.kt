package com.raymi.app.data.repository

import com.google.firebase.Timestamp
import com.raymi.app.core.workspace.WorkspaceManager
import com.raymi.app.data.model.dto.AlquilerDto
import com.raymi.app.data.remote.FirebaseDataSource
import com.raymi.app.data.remote.RentalDataSource
import com.raymi.app.data.remote.ObserverDataSource
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.EstadoAlquiler
import com.raymi.app.domain.model.Pago
import com.raymi.app.domain.model.MetodoPago
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.AlquilerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Implementación del repositorio de alquileres con rutas SaaS.
 */
class AlquilerRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseDataSource,
    private val rentalDataSource: RentalDataSource,
    private val observerDataSource: ObserverDataSource,
    private val workspaceManager: WorkspaceManager
) : AlquilerRepository {

    override suspend fun getAlquileres(workspaceId: String): Flow<Resource<List<Alquiler>>> {
        return observerDataSource.observeBusinessCollection(
            workspaceId = workspaceId,
            collection = "alquileres",
            orderByField = "createdAt",
            descending = true,
            limit = 500
        )
            .map { documents ->
                val alquileres = documents
                    .map { (id, data) -> AlquilerDto.fromMap(id, data).toDomain() }
                Resource.Success(alquileres) as Resource<List<Alquiler>>
            }
            .onStart { emit(Resource.Loading()) }
            .catch { e ->
                emit(Resource.Error("Error al obtener alquileres: ${e.message}"))
            }
    }

    override suspend fun getAlquilerById(id: String): Flow<Resource<Alquiler>> = flow {
        try {
            emit(Resource.Loading())
            val workspaceId = workspaceManager.getWorkspaceId() ?: throw IllegalStateException("Negocio no seleccionado")
            val data = rentalDataSource.getAlquiler(workspaceId, id)
            if (data != null) {
                emit(Resource.Success(AlquilerDto.fromMap(id, data).toDomain()))
            } else {
                emit(Resource.Error("Alquiler no encontrado"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Error al obtener alquiler: ${e.message}"))
        }
    }

    override suspend fun getAlquileresByEstado(workspaceId: String, estado: EstadoAlquiler): Flow<Resource<List<Alquiler>>> = flow {
        try {
            emit(Resource.Loading())
            val documents = dataSource.queryBusinessDocuments(
                collection = "alquileres",
                field = "estado",
                value = estado.name,
                limit = 300,
                negocioId = workspaceId
            )
            val alquileres = documents.map { (id, data) -> AlquilerDto.fromMap(id, data).toDomain() }
            emit(Resource.Success(alquileres.sortedByDescending { it.createdAt }))
        } catch (e: Exception) {
            emit(Resource.Error("Error: ${e.message}"))
        }
    }

    override suspend fun getAlquileresByCliente(workspaceId: String, clienteId: String): Flow<Resource<List<Alquiler>>> = flow {
        try {
            emit(Resource.Loading())
            val documents = dataSource.queryBusinessDocuments(
                collection = "alquileres",
                field = "clienteId",
                value = clienteId,
                limit = 300,
                negocioId = workspaceId
            )
            val alquileres = documents.map { (id, data) -> AlquilerDto.fromMap(id, data).toDomain() }
            emit(Resource.Success(alquileres.sortedByDescending { it.createdAt }))
        } catch (e: Exception) {
            emit(Resource.Error("Error: ${e.message}"))
        }
    }

    override suspend fun getAlquileresByItem(workspaceId: String, itemId: String): Flow<Resource<List<Alquiler>>> = flow {
        try {
            emit(Resource.Loading())
            val docs = dataSource.queryBusinessDocuments(
                collection = "alquileres",
                field = "itemId",
                value = itemId,
                limit = 500,
                negocioId = workspaceId
            )
            val alquileres = docs.map { (id, data) -> AlquilerDto.fromMap(id, data).toDomain() }
            emit(Resource.Success(alquileres.sortedByDescending { it.createdAt }))
        } catch (e: Exception) {
            emit(Resource.Error("Error: ${e.message}"))
        }
    }

    override suspend fun createAlquiler(alquiler: Alquiler): Flow<Resource<String>> = flow {
        try {
            emit(Resource.Loading())
            val dto = AlquilerDto.fromDomain(alquiler)
            val dataMap = dto.toMap().filterValues { it != null }.mapValues { it.value!! }
            val id = rentalDataSource.createAlquilerTransactional(alquiler.workspaceId, dataMap, alquiler.itemId)
            emit(Resource.Success(id))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error al crear"))
        }
    }

    override suspend fun updateAlquiler(alquiler: Alquiler): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            val dataMap = AlquilerDto.fromDomain(alquiler.copy(updatedAt = Timestamp.now())).toMap().filterValues { it != null }.mapValues { it.value!! }
            rentalDataSource.updateAlquiler(alquiler.workspaceId, alquiler.id, dataMap)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error("Error: ${e.message}"))
        }
    }

    override suspend fun registrarDevolucion(alquilerId: String): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            val workspaceId = workspaceManager.getWorkspaceId() ?: throw IllegalStateException("No seleccionado")
            rentalDataSource.registrarDevolucionTransactional(workspaceId, alquilerId)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error("Error: ${e.message}"))
        }
    }

    override suspend fun updateEstadoAlquiler(alquilerId: String, estado: EstadoAlquiler): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            val workspaceId = workspaceManager.getWorkspaceId() ?: throw IllegalStateException("No seleccionado")
            rentalDataSource.updateAlquiler(workspaceId, alquilerId, mapOf("estado" to estado.name, "updatedAt" to Timestamp.now()))
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error("Error: ${e.message}"))
        }
    }

    override suspend fun deleteAlquiler(alquilerId: String): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            val workspaceId = workspaceManager.getWorkspaceId() ?: throw IllegalStateException("No seleccionado")
            rentalDataSource.deleteAlquiler(workspaceId, alquilerId)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error("Error: ${e.message}"))
        }
    }

    override suspend fun addPago(workspaceId: String, alquilerId: String, pago: Pago): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            val data = mapOf("alquilerId" to alquilerId, "monto" to pago.monto, "metodoPago" to pago.metodoPago.name, "referencia" to pago.referencia, "fecha" to pago.fecha)
            rentalDataSource.addPago(workspaceId, alquilerId, data)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error("Error: ${e.message}"))
        }
    }

    override suspend fun getPagos(workspaceId: String, alquilerId: String): Flow<Resource<List<Pago>>> = flow {
        try {
            emit(Resource.Loading())
            val docs = rentalDataSource.getPagos(workspaceId, alquilerId)
            val pagos = docs.map { data ->
                Pago(
                    id = data["id"] as? String ?: "",
                    alquilerId = data["alquilerId"] as? String ?: "",
                    monto = (data["monto"] as? Number)?.toDouble() ?: 0.0,
                    metodoPago = try { MetodoPago.valueOf(data["metodoPago"] as? String ?: "EFECTIVO") } catch (_: Exception) { MetodoPago.EFECTIVO },
                    referencia = data["referencia"] as? String ?: "",
                    fecha = data["fecha"] as? Timestamp ?: Timestamp.now()
                )
            }
            emit(Resource.Success(pagos))
        } catch (_: Exception) {
            emit(Resource.Error("Error al obtener pagos"))
        }
    }
}
