package com.raymi.app.data.repository

import com.raymi.app.data.model.dto.WorkspaceDto
import com.raymi.app.data.remote.FirebaseDataSource
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.Workspace
import com.raymi.app.domain.repository.WorkspaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkspaceRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseDataSource
) : WorkspaceRepository {

    override suspend fun getWorkspacesByUser(userId: String): Flow<Resource<List<Workspace>>> = flow {
        emit(Resource.Loading())
        try {
            // Siguiendo el plan SaaS: colección "negocios" (antes workspaces)
            val response = dataSource.queryDocuments(FirebaseDataSource.COLLECTION_NEGOCIOS, "ownerUid", userId)
            val workspaces = response.map { (id, data) ->
                WorkspaceDto.fromMap(id, data).toDomain()
            }
            emit(Resource.Success(workspaces))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error al obtener negocios"))
        }
    }

    override suspend fun getWorkspaceById(workspaceId: String): Flow<Resource<Workspace>> = flow {
        emit(Resource.Loading())
        try {
            val data = dataSource.getDocument(FirebaseDataSource.COLLECTION_NEGOCIOS, workspaceId)
            if (data != null) {
                emit(Resource.Success(WorkspaceDto.fromMap(workspaceId, data).toDomain()))
            } else {
                emit(Resource.Error("Negocio no encontrado"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error al obtener negocio"))
        }
    }

    override suspend fun createWorkspace(workspace: Workspace): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            val dto = WorkspaceDto.fromDomain(workspace)
            val workspaceData = dto.toMap().filterValues { it != null }.mapValues { it.value!! }
            
            val statsData = mapOf(
                "totalItems" to 0L,
                "alquileresActivos" to 0L,
                "totalIngresos" to 0.0,
                "totalClientes" to 0L
            )

            val id = dataSource.createWorkspaceAtomic(workspaceData, statsData)
            emit(Resource.Success(id))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error al crear workspace"))
        }
    }

    override suspend fun updateWorkspace(workspace: Workspace): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val dto = WorkspaceDto.fromDomain(workspace)
            val dataMap = dto.toMap().filterValues { it != null }.mapValues { it.value!! }
            dataSource.updateDocument(FirebaseDataSource.COLLECTION_NEGOCIOS, workspace.id, dataMap)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error al actualizar negocio"))
        }
    }

    override suspend fun deleteWorkspace(workspaceId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            dataSource.deleteDocument(FirebaseDataSource.COLLECTION_NEGOCIOS, workspaceId)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error al eliminar negocio"))
        }
    }

    override suspend fun getCurrentWorkspace(userId: String): Flow<Resource<Workspace?>> = flow {
        emit(Resource.Loading())
        try {
            // Estrategia Senior: Primero obtener el negocioId del perfil de usuario
            // Esto evita problemas de permisos con queries en la colección negocios
            val negocioId = dataSource.getCurrentBusinessId()
            
            if (negocioId.isNotBlank()) {
                val data = dataSource.getDocument(FirebaseDataSource.COLLECTION_NEGOCIOS, negocioId)
                if (data != null) {
                    emit(Resource.Success(WorkspaceDto.fromMap(negocioId, data).toDomain()))
                } else {
                    emit(Resource.Success(null))
                }
            } else {
                emit(Resource.Success(null))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error al obtener negocio actual"))
        }
    }
}
