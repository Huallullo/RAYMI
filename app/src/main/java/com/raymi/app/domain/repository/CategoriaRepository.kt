package com.raymi.app.domain.repository

import com.raymi.app.domain.model.Categoria
import com.raymi.app.domain.model.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para la gestión de categorías de productos en un workspace.
 */
interface CategoriaRepository {
    
    /**
     * Obtiene todas las categorías activas de un negocio.
     */
    fun getCategorias(workspaceId: String): Flow<Resource<List<Categoria>>>
    
    /**
     * Agrega una nueva categoría.
     */
    fun addCategoria(categoria: Categoria): Flow<Resource<String>>
    
    /**
     * Actualiza una categoría existente.
     */
    fun updateCategoria(categoria: Categoria): Flow<Resource<Unit>>
    
    /**
     * Elimina una categoría.
     */
    fun deleteCategoria(workspaceId: String, categoriaId: String): Flow<Resource<Unit>>

    /**
     * Invalida el caché de categorías para forzar una recarga desde Firestore.
     */
    suspend fun invalidarCache(workspaceId: String)
}
