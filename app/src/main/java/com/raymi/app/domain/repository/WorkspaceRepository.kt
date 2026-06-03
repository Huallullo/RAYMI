package com.raymi.app.domain.repository

import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.Workspace
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para la gestión de Workspaces (Negocios)
 */
interface WorkspaceRepository {
    
    /**
     * Obtiene todos los workspaces asociados a un usuario
     */
    suspend fun getWorkspacesByUser(userId: String): Flow<Resource<List<Workspace>>>
    
    /**
     * Obtiene un workspace por su ID
     */
    suspend fun getWorkspaceById(workspaceId: String): Flow<Resource<Workspace>>
    
    /**
     * Crea un nuevo workspace
     */
    suspend fun createWorkspace(workspace: Workspace): Flow<Resource<String>>
    
    /**
     * Actualiza la información de un workspace
     */
    suspend fun updateWorkspace(workspace: Workspace): Flow<Resource<Unit>>
    
    /**
     * Elimina un workspace
     */
    suspend fun deleteWorkspace(workspaceId: String): Flow<Resource<Unit>>
    
    /**
     * Obtiene el workspace que el usuario tiene marcado como actual/favorito
     */
    suspend fun getCurrentWorkspace(userId: String): Flow<Resource<Workspace?>>

    /**
     * Actualiza metadatos de estadísticas
     */
    suspend fun updateStats(workspaceId: String, data: Map<String, Any>)

    /**
     * [M-12] Cuenta cuántos negocios posee un usuario.
     */
    suspend fun countWorkspacesByOwner(userId: String): Int
}
