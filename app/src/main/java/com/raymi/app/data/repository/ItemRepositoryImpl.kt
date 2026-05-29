package com.raymi.app.data.repository

import com.raymi.app.data.model.dto.ItemDto
import com.raymi.app.data.remote.FirebaseDataSource
import com.raymi.app.data.remote.ItemDataSource
import com.raymi.app.data.remote.ObserverDataSource
import com.raymi.app.domain.model.Item
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.ItemRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class ItemRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseDataSource,
    private val itemDataSource: ItemDataSource,
    private val observerDataSource: ObserverDataSource
) : ItemRepository {

    override suspend fun getItemsByWorkspace(workspaceId: String, limit: Long, startAfterValue: Any?): Flow<Resource<List<Item>>> {
        return observerDataSource.observeBusinessCollection(
            workspaceId = workspaceId,
            collection = "items",
            orderByField = "nombre",
            descending = false,
            limit = limit,
            startAfterValue = startAfterValue
        )
            .map { documents ->
                val items = documents.map { (id, data) -> ItemDto.fromMap(id, data).toDomain() }
                Resource.Success(items) as Resource<List<Item>>
            }
            .onStart { emit(Resource.Loading()) }
            .catch { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                emit(Resource.Error("Error al obtener inventario: ${e.message}"))
            }
    }

    override suspend fun getItemById(workspaceId: String, itemId: String): Flow<Resource<Item>> = flow {
        emit(Resource.Loading())
        try {
            val data = dataSource.getBusinessDocument("items", itemId, workspaceId)
            if (data != null) {
                emit(Resource.Success(ItemDto.fromMap(itemId, data).toDomain()))
            } else {
                emit(Resource.Error("Producto no encontrado"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error: ${e.message}"))
        }
    }

    override suspend fun addItem(item: Item): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            val dto = ItemDto.fromDomain(item)
            val data = dto.toMap().filterValues { it != null }.mapValues { it.value!! }
            val id = itemDataSource.addItemTransactional(item.workspaceId, data, item.codigo)
            emit(Resource.Success(id))
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error al agregar: ${e.message}"))
        }
    }

    override suspend fun updateItem(item: Item): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val dto = ItemDto.fromDomain(item)
            val data = dto.toMap().filterValues { it != null }.mapValues { it.value!! }
            dataSource.updateBusinessDocument("items", item.id, data, item.workspaceId)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error al actualizar: ${e.message}"))
        }
    }

    override suspend fun deleteItem(workspaceId: String, itemId: String, codigo: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            itemDataSource.deleteItemTransactional(workspaceId, itemId, codigo)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error al eliminar: ${e.message}"))
        }
    }

    override suspend fun searchItems(workspaceId: String, query: String): Flow<Resource<List<Item>>> = flow {
        emit(Resource.Loading())
        try {
            val documents = if (query.isBlank()) {
                dataSource.getAllBusinessDocumentsOrderedLimited("items", "nombre", false, 50)
            } else {
                dataSource.queryBusinessArrayContainsLimited("items", "searchTerms", query.lowercase().trim())
            }
            val items = documents.map { (id, data) -> ItemDto.fromMap(id, data).toDomain() }
            emit(Resource.Success(items))
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error en búsqueda: ${e.message}"))
        }
    }

    override suspend fun getItemsByCategoria(workspaceId: String, categoriaId: String): Flow<Resource<List<Item>>> = flow {
        emit(Resource.Loading())
        try {
            val documents = dataSource.queryBusinessDocuments("items", "categoriaId", categoriaId, limit = 50, negocioId = workspaceId)
            val items = documents.map { (id, data) -> ItemDto.fromMap(id, data).toDomain() }
            emit(Resource.Success(items))
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error al filtrar: ${e.message}"))
        }
    }
}
