package com.raymi.app.data.repository

import com.google.firebase.Timestamp
import com.raymi.app.core.cache.SmartCache
import com.raymi.app.core.utils.FirebaseErrorMapper
import com.raymi.app.core.workspace.WorkspaceManager
import com.raymi.app.data.model.dto.AlquilerDto
import com.raymi.app.data.remote.FirebaseDataSource
import com.raymi.app.data.remote.RentalDataSource
import com.raymi.app.domain.model.*
import com.raymi.app.domain.repository.AlquilerRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlquilerRepositoryImpl @Inject constructor(
    private val firestore: com.google.firebase.firestore.FirebaseFirestore, // ✅ Inyectado directamente para batches
    private val dataSource: FirebaseDataSource,
    private val rentalDataSource: RentalDataSource,
    private val workspaceManager: WorkspaceManager
) : AlquilerRepository {

    // OPTIMIZACIÓN: TTL de 2 minutos para alquileres (cambian frecuente)
    private val cacheMap = mutableMapOf<String, SmartCache<List<Alquiler>>>()
    private val TTL_2_MIN = 2 * 60 * 1000L

    private fun getCacheFor(workspaceId: String) = cacheMap.getOrPut(workspaceId) { SmartCache() }

    override fun getAlquileres(workspaceId: String): Flow<Resource<List<Alquiler>>> {
        return flow {
            emit(Resource.Loading())
            emit(getAlquileresOnce(workspaceId))
        }
    }

    override suspend fun getAlquileresOnce(workspaceId: String, limit: Long, lastSnapshot: Any?): Resource<List<Alquiler>> {
        val cache = getCacheFor(workspaceId)
        if (lastSnapshot == null) {
            cache.get()?.let { return Resource.Success(it) }
        }

        return try {
            val fetchLimit = if (limit > 0) limit else 20
            val docs = dataSource.getBusinessDocumentsPaged(
                collection = "alquileres", 
                limit = fetchLimit, 
                lastSnapshot = lastSnapshot as? com.google.firebase.firestore.DocumentSnapshot,
                negocioId = workspaceId
            )
            val list = docs.mapNotNull { doc ->
                doc.data?.let { AlquilerDto.fromMap(doc.id, it).toDomain() }
            }

            if (lastSnapshot == null) {
                cache.set(list, TTL_2_MIN)
            }
            // OPTIMIZACIÓN: Devolver el último snapshot como cursor
            Resource.Success(list, cursor = docs.lastOrNull())
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error(FirebaseErrorMapper.mapError(e))
        }
    }

    override fun getAlquilerById(id: String): Flow<Resource<Alquiler>> = flow {
        emit(Resource.Loading())
        val result = try {
            val workspaceId = workspaceManager.getWorkspaceId() ?: throw IllegalStateException("Negocio no seleccionado")
            val data = rentalDataSource.getAlquiler(workspaceId, id)
            if (data != null) {
                Resource.Success(AlquilerDto.fromMap(id, data).toDomain())
            } else {
                Resource.Error("Alquiler no encontrado")
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error(FirebaseErrorMapper.mapError(e))
        }
        emit(result)
    }

    override fun getAlquileresByEstado(workspaceId: String, estado: EstadoAlquiler): Flow<Resource<List<Alquiler>>> = flow {
        emit(Resource.Loading())
        val result = try {
            val documents = dataSource.queryBusinessDocuments("alquileres", "estado", estado.name, limit = 50, negocioId = workspaceId)
            val alquileres = documents.map { (id, data) -> AlquilerDto.fromMap(id, data).toDomain() }
            Resource.Success(alquileres.sortedByDescending { it.createdAt })
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error(FirebaseErrorMapper.mapError(e))
        }
        emit(result)
    }

    override fun getAlquileresByDateRange(
        workspaceId: String,
        start: Timestamp,
        end: Timestamp
    ): Flow<Resource<List<Alquiler>>> = flow {
        emit(Resource.Loading())
        val result = try {
            // ✅ COSTO 1 FIX: Usar 'fechaFinPrevista' para detectar vencimientos reales
            val docs = dataSource.queryBusinessDocumentsRange("alquileres", "fechaFinPrevista", start, end, limit = 100, negocioId = workspaceId)
            val list = docs.map { (id, data) -> AlquilerDto.fromMap(id, data).toDomain() }
            Resource.Success(list)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error(FirebaseErrorMapper.mapError(e))
        }
        emit(result)
    }

    override fun createAlquiler(alquiler: Alquiler): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        val result = try {
            val dto = AlquilerDto.fromDomain(alquiler)
            val dataMap = dto.toMap().filterValues { it != null }.mapValues { it.value!! }
            val id = rentalDataSource.createAlquilerTransactional(alquiler.workspaceId, dataMap)
            getCacheFor(alquiler.workspaceId).invalidate()
            Resource.Success(id)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error(FirebaseErrorMapper.mapError(e))
        }
        emit(result)
    }

    override fun updateAlquiler(alquiler: Alquiler): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        val result = try {
            val dataMap = AlquilerDto.fromDomain(alquiler.copy(updatedAt = Timestamp.now())).toMap().filterValues { it != null }.mapValues { it.value!! }
            rentalDataSource.updateAlquiler(alquiler.workspaceId, alquiler.id, dataMap)
            getCacheFor(alquiler.workspaceId).invalidate()
            Resource.Success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error(FirebaseErrorMapper.mapError(e))
        }
        emit(result)
    }

    override fun updateAlquilerConStock(alquiler: Alquiler, diffCantidad: Int): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        val result = try {
            val dataMap = AlquilerDto.fromDomain(alquiler.copy(updatedAt = Timestamp.now())).toMap().filterValues { it != null }.mapValues { it.value!! }
            rentalDataSource.updateAlquilerTransactional(alquiler.workspaceId, alquiler.id, dataMap, alquiler.itemId, diffCantidad)
            getCacheFor(alquiler.workspaceId).invalidate()
            Resource.Success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error(FirebaseErrorMapper.mapError(e))
        }
        emit(result)
    }

    override fun registrarDevolucion(
        alquilerId: String,
        penalidad: Double,
        observaciones: String,
        montoGarantiaRetenida: Double,
        unidadesARetornar: Int
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        val result = try {
            val workspaceId = workspaceManager.getWorkspaceId() ?: throw IllegalStateException("No seleccionado")
            rentalDataSource.registrarDevolucionTransactional(workspaceId, alquilerId, penalidad, observaciones, montoGarantiaRetenida, unidadesARetornar)
            getCacheFor(workspaceId).invalidate()
            Resource.Success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error(FirebaseErrorMapper.mapError(e))
        }
        emit(result)
    }

    override fun cancelarAlquiler(alquilerId: String, motivo: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        val result = try {
            val workspaceId = workspaceManager.getWorkspaceId() ?: throw IllegalStateException("No seleccionado")
            rentalDataSource.cancelarAlquilerTransactional(workspaceId, alquilerId, motivo)
            getCacheFor(workspaceId).invalidate()
            Resource.Success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error(FirebaseErrorMapper.mapError(e))
        }
        emit(result)
    }

    override fun updateEstadoAlquiler(alquilerId: String, estado: EstadoAlquiler): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        val result = try {
            val workspaceId = workspaceManager.getWorkspaceId() ?: throw IllegalStateException("No seleccionado")
            rentalDataSource.updateAlquiler(workspaceId, alquilerId, mapOf("estado" to estado.name, "updatedAt" to Timestamp.now()))
            getCacheFor(workspaceId).invalidate()
            Resource.Success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error(FirebaseErrorMapper.mapError(e))
        }
        emit(result)
    }

    override fun getAlquileresByEstados(
        workspaceId: String,
        estados: List<EstadoAlquiler>,
        limit: Long
    ): Flow<Resource<List<Alquiler>>> = flow {
        emit(Resource.Loading())
        try {
            val statusNames = estados.map { it.name }
            val snapshot = firestore.collection("negocios")
                .document(workspaceId)
                .collection("alquileres")
                .whereIn("estado", statusNames)
                .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
            
            val list = snapshot.documents.mapNotNull { doc ->
                doc.data?.let { AlquilerDto.fromMap(doc.id, it).toDomain() }
            }
            emit(Resource.Success(list))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error(FirebaseErrorMapper.mapError(e)))
        }
    }

    override fun updateAlquileresEstadoBatch(
        workspaceId: String,
        alquilerIds: List<String>,
        nuevoEstado: EstadoAlquiler
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val negocioRef = firestore.collection(com.raymi.app.core.utils.Constants.COLLECTION_NEGOCIOS).document(workspaceId)
            val now = Timestamp.now()

            // ✅ BUG 8 & [M-07] FIX: Firestore limit is 500 per batch. Chunking to 400 for safety.
            alquilerIds.chunked(400).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { id ->
                    val ref = negocioRef.collection("alquileres").document(id)
                    batch.update(ref, mapOf("estado" to nuevoEstado.name, "updatedAt" to now))
                }
                batch.commit().await()
            }

            getCacheFor(workspaceId).invalidate()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error(FirebaseErrorMapper.mapError(e)))
        }
    }

    override suspend fun getAlquileresCerrados(
        workspaceId: String,
        limit: Long,
        lastSnapshot: Any?
    ): Resource<List<Alquiler>> {
        return try {
            var query = firestore.collection("negocios").document(workspaceId).collection("alquileres")
                .whereIn("estado", listOf("DEVUELTO", "CANCELADO"))
                .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit)

            (lastSnapshot as? com.google.firebase.firestore.DocumentSnapshot)?.let { 
                query = query.startAfter(it) 
            }

            val snap = query.get().await()
            val list = snap.documents.mapNotNull { doc ->
                doc.data?.let { AlquilerDto.fromMap(doc.id, it).toDomain() }
            }
            Resource.Success(list, cursor = snap.documents.lastOrNull())
        } catch (e: Exception) { 
            Resource.Error(FirebaseErrorMapper.mapError(e))
        }
    }

    override fun deleteAlquiler(alquilerId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        val result = try {
            val workspaceId = workspaceManager.getWorkspaceId() ?: throw IllegalStateException("No seleccionado")
            rentalDataSource.deleteAlquilerTransactional(workspaceId, alquilerId)
            getCacheFor(workspaceId).invalidate()
            Resource.Success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error(FirebaseErrorMapper.mapError(e))
        }
        emit(result)
    }

    override fun addPago(workspaceId: String, alquilerId: String, pago: Pago): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        val result = try {
            val data = mapOf("alquilerId" to alquilerId, "monto" to pago.monto, "metodoPago" to pago.metodoPago.name, "referencia" to pago.referencia, "fecha" to pago.fecha)
            rentalDataSource.addPago(workspaceId, alquilerId, data)
            getCacheFor(workspaceId).invalidate()
            Resource.Success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error(FirebaseErrorMapper.mapError(e))
        }
        emit(result)
    }

    override fun getPagos(workspaceId: String, alquilerId: String): Flow<Resource<List<Pago>>> = flow {
        emit(Resource.Loading())
        val result = try {
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
            Resource.Success(pagos)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error(FirebaseErrorMapper.mapError(e))
        }
        emit(result)
    }

    override fun getAlquileresByCliente(workspaceId: String, clienteId: String): Flow<Resource<List<Alquiler>>> = flow {
        emit(Resource.Loading())
        val result = try {
            val documents = dataSource.queryBusinessDocuments("alquileres", "clienteId", clienteId, limit = 50, negocioId = workspaceId)
            val list = documents.map { (id, data) -> AlquilerDto.fromMap(id, data).toDomain() }
            Resource.Success(list)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error(FirebaseErrorMapper.mapError(e))
        }
        emit(result)
    }

    override fun getAlquileresByItem(workspaceId: String, itemId: String): Flow<Resource<List<Alquiler>>> = flow {
        emit(Resource.Loading())
        val result = try {
            val documents = dataSource.queryBusinessDocuments("alquileres", "itemId", itemId, limit = 50, negocioId = workspaceId)
            val list = documents.map { (id, data) -> AlquilerDto.fromMap(id, data).toDomain() }
            Resource.Success(list)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Resource.Error(FirebaseErrorMapper.mapError(e))
        }
        emit(result)
    }

    override suspend fun getPagosDeAlquileres(workspaceId: String, alquilerIds: List<String>): Resource<List<Pago>> {
        return try {
            val docs = rentalDataSource.getPagosDeAlquileres(workspaceId, alquilerIds)
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
            Resource.Success(pagos)
        } catch (e: Exception) {
            Resource.Error("Error auditando pagos: ${e.message}")
        }
    }
}
