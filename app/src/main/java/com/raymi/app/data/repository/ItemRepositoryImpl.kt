package com.raymi.app.data.repository

import com.raymi.app.core.cache.SmartCache
import com.raymi.app.core.utils.FirebaseErrorMapper
import com.raymi.app.data.model.dto.ItemDto
import com.raymi.app.data.remote.FirebaseDataSource
import com.raymi.app.data.remote.ItemDataSource
import com.raymi.app.domain.model.Item
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.ItemRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseDataSource,
    private val itemDataSource: ItemDataSource
) : ItemRepository {

    // OPTIMIZACIÓN: TTL de 10 minutos para items
    private val cacheMap = mutableMapOf<String, SmartCache<List<Item>>>()
    private val TTL_10_MIN = 10 * 60 * 1000L

    private fun getCacheFor(workspaceId: String) = cacheMap.getOrPut(workspaceId) { SmartCache() }

    override suspend fun getItemsByWorkspace(workspaceId: String, limit: Long, startAfterValue: Any?): Flow<Resource<List<Item>>> {
        return flow {
            emit(Resource.Loading())
            emit(getItemsByWorkspaceOnce(workspaceId, limit))
        }
    }

    override suspend fun getItemsByWorkspaceOnce(workspaceId: String, limit: Long, lastSnapshot: Any?): Resource<List<Item>> {
        val cache = getCacheFor(workspaceId)
        // Solo usamos cache si es la primera página
        if (lastSnapshot == null) {
            cache.get()?.let { return Resource.Success(it) }
        }

        return try {
            val fetchLimit = if (limit > 0) limit else 20
            val docs = dataSource.getBusinessDocumentsPaged(
                collection = "items", 
                limit = fetchLimit, 
                lastSnapshot = lastSnapshot as? com.google.firebase.firestore.DocumentSnapshot,
                negocioId = workspaceId
            )
            val items = docs.mapNotNull { doc ->
                doc.data?.let { ItemDto.fromMap(doc.id, it).toDomain() }
            }
            
            if (lastSnapshot == null) {
                cache.set(items, TTL_10_MIN)
            }
            // OPTIMIZACIÓN: Cursor real
            Resource.Success(items, cursor = docs.lastOrNull())
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Resource.Error(FirebaseErrorMapper.mapError(e))
        }
    }

    override suspend fun getItemById(workspaceId: String, itemId: String): Flow<Resource<Item>> = flow {
        emit(Resource.Loading())
        val result = try {
            val data = dataSource.getBusinessDocument("items", itemId, workspaceId)
            if (data != null) {
                Resource.Success(ItemDto.fromMap(itemId, data).toDomain())
            } else {
                Resource.Error("Producto no encontrado")
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Resource.Error("Error: ${e.message}")
        }
        emit(result)
    }

    override suspend fun addItem(item: Item): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        val result = try {
            // OPTIMIZACIÓN: Asegurar Timestamp para el ordenamiento del Dashboard
            val itemWithDate = if (item.createdAt == null) {
                item.copy(createdAt = com.google.firebase.Timestamp.now(), updatedAt = com.google.firebase.Timestamp.now())
            } else item
            
            val dto = ItemDto.fromDomain(itemWithDate)
            val data = dto.toMapForCreate().filterValues { it != null }.mapValues { it.value!! }
            val id = itemDataSource.addItemTransactional(item.workspaceId, data, item.codigo)
            
            invalidateCache(item.workspaceId) // Ahora es suspend y thread-safe
            Resource.Success(id)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Resource.Error("Error al agregar: ${e.message}")
        }
        emit(result)
    }

    override suspend fun updateItem(item: Item): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        val result = try {
            val workspaceId = item.workspaceId
            
            // ✅ [A-06] Actualizar índice de códigos si cambió
            val oldItemSnap = dataSource.getBusinessDocument("items", item.id, workspaceId)
            val oldCodigo = oldItemSnap?.get("codigo") as? String
            
            if (oldCodigo != null && oldCodigo != item.codigo) {
                val negocioRef = dataSource.firestore.collection("negocios").document(workspaceId)
                dataSource.firestore.runTransaction { transaction ->
                    val oldIndexRef = negocioRef.collection("items_codigo_index").document(oldCodigo)
                    val newIndexRef = negocioRef.collection("items_codigo_index").document(item.codigo)
                    
                    if (transaction.get(newIndexRef).exists()) {
                        throw IllegalStateException("Ya existe un producto con el código ${item.codigo}")
                    }
                    
                    transaction.delete(oldIndexRef)
                    transaction.set(newIndexRef, mapOf("itemId" to item.id, "codigo" to item.codigo))
                }.await()
            }

            val dto = ItemDto.fromDomain(item)
            val data = dto.toMapForUpdate().filterValues { it != null }.mapValues { it.value!! }
            dataSource.updateBusinessDocument("items", item.id, data, item.workspaceId)
            getCacheFor(item.workspaceId).invalidate()
            Resource.Success(Unit)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Resource.Error("Error al actualizar: ${e.message}")
        }
        emit(result)
    }

    override suspend fun deleteItem(workspaceId: String, itemId: String, codigo: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        val result = try {
            itemDataSource.deleteItemTransactional(workspaceId, itemId, codigo)
            getCacheFor(workspaceId).invalidate()
            Resource.Success(Unit)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Resource.Error("Error al eliminar: ${e.message}")
        }
        emit(result)
    }

    override suspend fun searchItems(workspaceId: String, query: String): Flow<Resource<List<Item>>> = flow {
        emit(Resource.Loading())
        val result = try {
            if (query.isBlank()) {
                val docs = dataSource.getBusinessDocumentsPaged("items", limit = 25, negocioId = workspaceId)
                Resource.Success(
                    docs.mapNotNull { doc -> doc.data?.let { ItemDto.fromMap(doc.id, it).toDomain() } }
                )
            } else {
                val docs = dataSource.queryBusinessArrayContainsLimited(
                    collection = "items", 
                    field = "searchTerms", 
                    value = query.lowercase().trim(), 
                    limit = 25,
                    negocioId = workspaceId
                )
                val items = docs.map { (id, data) -> ItemDto.fromMap(id, data).toDomain() }
                Resource.Success(items)
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Resource.Error("Error en búsqueda: ${e.message}")
        }
        emit(result)
    }

    override suspend fun getItemsByCategoria(workspaceId: String, categoriaId: String): Flow<Resource<List<Item>>> = flow {
        emit(Resource.Loading())
        val result = try {
            val documents = dataSource.queryBusinessDocuments("items", "categoriaId", categoriaId, limit = 50, negocioId = workspaceId)
            val items = documents.map { (id, data) -> ItemDto.fromMap(id, data).toDomain() }
            Resource.Success(items)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Resource.Error("Error al filtrar: ${e.message}")
        }
        emit(result)
    }

    override suspend fun updateEstadoItem(workspaceId: String, itemId: String, estado: String): Resource<Unit> {
        return try {
            val data = mapOf(
                "estado" to estado,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            dataSource.updateBusinessDocument("items", itemId, data, workspaceId)
            invalidateCache(workspaceId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Fallo al actualizar estado: ${e.message}")
        }
    }

    override suspend fun invalidateCache(workspaceId: String) {
        getCacheFor(workspaceId).invalidate()
    }
}
