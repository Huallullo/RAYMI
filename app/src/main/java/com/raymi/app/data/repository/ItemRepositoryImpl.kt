package com.raymi.app.data.repository

import com.raymi.app.core.cache.SmartCache
import com.raymi.app.data.model.dto.ItemDto
import com.raymi.app.data.remote.FirebaseDataSource
import com.raymi.app.data.remote.ItemDataSource
import com.raymi.app.data.remote.ObserverDataSource
import com.raymi.app.domain.model.Item
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.ItemRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseDataSource,
    private val itemDataSource: ItemDataSource,
    private val observerDataSource: ObserverDataSource
) : ItemRepository {

    // Cache segmentada por Workspace (Multi-tenancy Fix)
    private val cacheMap = mutableMapOf<String, SmartCache<List<Item>>>()
    private val TTL_5_MIN = 5 * 60 * 1000L

    private fun getCacheFor(workspaceId: String) = cacheMap.getOrPut(workspaceId) { SmartCache() }

    override suspend fun getItemsByWorkspace(workspaceId: String, limit: Long, startAfterValue: Any?): Flow<Resource<List<Item>>> {
        return flow {
            emit(Resource.Loading())
            emit(getItemsByWorkspaceOnce(workspaceId, limit))
        }
    }

    override suspend fun getItemsByWorkspaceOnce(workspaceId: String, limit: Long): Resource<List<Item>> {
        val cache = getCacheFor(workspaceId)
        cache.get()?.let { return Resource.Success(it) }

        return try {
            val pagedDocs = dataSource.getBusinessDocumentsPaged("items", limit = limit, negocioId = workspaceId)
            val items = pagedDocs.map { (id, data) -> ItemDto.fromMap(id, data).toDomain() }
            
            cache.set(items, TTL_5_MIN)
            Resource.Success(items)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Resource.Error("Error al cargar inventario: ${e.message}")
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
            val dto = ItemDto.fromDomain(item)
            val data = dto.toMap().filterValues { it != null }.mapValues { it.value!! }
            val id = itemDataSource.addItemTransactional(item.workspaceId, data, item.codigo)
            getCacheFor(item.workspaceId).invalidate()
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
            val dto = ItemDto.fromDomain(item)
            val data = dto.toMap().filterValues { it != null }.mapValues { it.value!! }
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
            val documents = if (query.isBlank()) {
                dataSource.getBusinessDocumentsPaged("items", limit = 20)
            } else {
                dataSource.queryBusinessArrayContainsLimited("items", "searchTerms", query.lowercase().trim(), limit = 20)
            }
            val items = documents.map { (id, data) -> ItemDto.fromMap(id, data).toDomain() }
            Resource.Success(items)
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

    override fun invalidateCache(workspaceId: String) {
        getCacheFor(workspaceId).invalidate()
    }
}
