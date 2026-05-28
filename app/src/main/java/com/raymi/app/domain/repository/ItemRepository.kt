package com.raymi.app.domain.repository

import com.raymi.app.domain.model.Item
import com.raymi.app.domain.model.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio genérico para ítems (reemplaza a VestuarioRepository)
 */
interface ItemRepository {
    
    /**
     * Obtiene todos los ítems de un workspace
     */
    suspend fun getItemsByWorkspace(workspaceId: String): Flow<Resource<List<Item>>>
    
    /**
     * Obtiene un ítem por su ID
     */
    suspend fun getItemById(workspaceId: String, itemId: String): Flow<Resource<Item>>
    
    /**
     * Agrega un nuevo ítem al workspace
     */
    suspend fun addItem(item: Item): Flow<Resource<String>>
    
    /**
     * Actualiza un ítem existente
     */
    suspend fun updateItem(item: Item): Flow<Resource<Unit>>
    
    /**
     * Elimina un ítem del workspace limpiando sus índices
     */
    suspend fun deleteItem(workspaceId: String, itemId: String, codigo: String): Flow<Resource<Unit>>
    
    /**
     * Busca ítems por nombre o código dentro de un workspace
     */
    suspend fun searchItems(workspaceId: String, query: String): Flow<Resource<List<Item>>>

    /**
     * Obtiene ítems filtrados por categoría
     */
    suspend fun getItemsByCategoria(workspaceId: String, categoriaId: String): Flow<Resource<List<Item>>>
}
