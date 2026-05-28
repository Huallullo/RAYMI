package com.raymi.app.data.repository

import com.raymi.app.data.model.dto.ItemDto
import com.raymi.app.data.remote.FirebaseDataSource
import com.raymi.app.data.remote.ItemDataSource
import com.raymi.app.data.remote.ObserverDataSource
import com.raymi.app.data.remote.StatsDataSource
import com.raymi.app.domain.model.Item
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseDataSource,
    private val itemDataSource: ItemDataSource,
    private val observerDataSource: ObserverDataSource,
    private val statsDataSource: StatsDataSource
) : ItemRepository {

    override suspend fun getItemsByWorkspace(workspaceId: String): Flow<Resource<List<Item>>> {
        return observerDataSource.observeBusinessCollection(
            workspaceId = workspaceId,
            collection = "items",
            orderByField = "nombre",
            descending = false,
            limit = 500
        )
            .map { documents ->
                val items = documents.map { (id, data) ->
                    ItemDto.fromMap(id, data).toDomain()
                }
                Resource.Success(items) as Resource<List<Item>>
            }
            .onStart { emit(Resource.Loading()) }
            .catch { e ->
                emit(Resource.Error("Error al obtener ítems: ${e.message}"))
            }
    }

    override suspend fun getItemById(workspaceId: String, itemId: String): Flow<Resource<Item>> = flow {
        emit(Resource.Loading())
        try {
            val data = dataSource.getBusinessDocument("items", itemId, workspaceId)
            if (data != null) {
                emit(Resource.Success(ItemDto.fromMap(itemId, data).toDomain()))
            } else {
                emit(Resource.Error("Ítem no encontrado"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Error al obtener ítem: ${e.message}"))
        }
    }

    override suspend fun addItem(item: Item): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            val dto = ItemDto.fromDomain(item)
            val id = itemDataSource.addItemTransactional(
                workspaceId = item.workspaceId,
                itemData = dto.toMap().filterValues { it != null }.mapValues { it.value!! },
                codigo = item.codigo
            )
            emit(Resource.Success(id))
        } catch (e: IllegalStateException) {
            if (e.message?.contains("Ya existe") == true) {
                emit(Resource.Error("El código '${item.codigo}' ya está en uso. Usa otro o deja que el sistema genere uno nuevo."))
            } else {
                emit(Resource.Error(e.localizedMessage ?: "Error de validación"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Error al agregar ítem: ${e.message}"))
        }
    }

    override suspend fun updateItem(item: Item): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val dto = ItemDto.fromDomain(item)
            dataSource.updateBusinessDocument(
                collection = "items",
                documentId = item.id,
                data = dto.toMap().filterValues { it != null }.mapValues { it.value!! }
            )
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error("Error al actualizar ítem: ${e.message}"))
        }
    }

    override suspend fun deleteItem(workspaceId: String, itemId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            dataSource.deleteBusinessDocument("items", itemId, workspaceId)
            statsDataSource.updateStats(workspaceId, "totalItems", -1L)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error("Error al eliminar ítem: ${e.message}"))
        }
    }

    override suspend fun searchItems(workspaceId: String, query: String): Flow<Resource<List<Item>>> = flow {
        emit(Resource.Loading())
        try {
            val documents = dataSource.queryBusinessArrayContainsLimited(
                collection = "items",
                field = "searchTerms",
                value = query.lowercase(),
                limit = 100
            )
            val items = documents.map { (id, data) ->
                ItemDto.fromMap(id, data).toDomain()
            }
            emit(Resource.Success(items))
        } catch (e: Exception) {
            emit(Resource.Error("Error al buscar ítems: ${e.message}"))
        }
    }

    override suspend fun getItemsByCategoria(workspaceId: String, categoriaId: String): Flow<Resource<List<Item>>> = flow {
        emit(Resource.Loading())
        try {
            val documents = dataSource.queryBusinessDocuments(
                collection = "items",
                field = "categoriaId",
                value = categoriaId,
                limit = 300,
                negocioId = workspaceId
            )
            val items = documents.map { (id, data) -> ItemDto.fromMap(id, data).toDomain() }
            emit(Resource.Success(items))
        } catch (e: Exception) {
            emit(Resource.Error("Error al filtrar por categoría: ${e.message}"))
        }
    }
}
