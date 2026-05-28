package com.raymi.app.data.repository

import com.raymi.app.core.utils.Constants.COLLECTION_NEGOCIOS
import com.raymi.app.data.model.dto.WorkspaceDto
import com.raymi.app.data.remote.AuthDataSource
import com.raymi.app.data.remote.FirebaseDataSource
import com.raymi.app.data.remote.WorkspaceDataSource
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.Workspace
import com.raymi.app.domain.repository.WorkspaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkspaceRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseDataSource,
    private val workspaceDataSource: WorkspaceDataSource,
    private val authDataSource: AuthDataSource
) : WorkspaceRepository {

    override suspend fun getWorkspacesByUser(userId: String): Flow<Resource<List<Workspace>>> = flow {
        emit(Resource.Loading())
        try {
            val response = dataSource.queryDocuments(COLLECTION_NEGOCIOS, "ownerUid", userId)
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
            val data = dataSource.getDocument(COLLECTION_NEGOCIOS, workspaceId)
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
            val user = authDataSource.getCurrentUser() ?: throw IllegalStateException("Usuario no autenticado")
            val dto = WorkspaceDto.fromDomain(workspace)
            val workspaceData = dto.toMap().filterValues { it != null }.mapValues { it.value!! }
            
            val statsData = mapOf(
                "totalItems" to 0L,
                "alquileresActivos" to 0L,
                "totalIngresos" to 0.0,
                "totalClientes" to 0L
            )

            val id = workspaceDataSource.createWorkspaceAtomic(
                workspaceData = workspaceData,
                statsData = statsData,
                uid = user.uid
            )
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
            dataSource.updateDocument(COLLECTION_NEGOCIOS, workspace.id, dataMap)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error al actualizar negocio"))
        }
    }

    override suspend fun deleteWorkspace(workspaceId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            dataSource.deleteDocument(COLLECTION_NEGOCIOS, workspaceId)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error al eliminar negocio"))
        }
    }

    override suspend fun getCurrentWorkspace(userId: String): Flow<Resource<Workspace?>> = flow {
        emit(Resource.Loading())
        try {
            // QA Fix: Encontrar negocio asignado al perfil
            val negocioId = dataSource.ensureBusinessProfileForUser(authDataSource.getCurrentUser() ?: throw Exception("No user"))
            
            if (negocioId.isNotBlank()) {
                val data = dataSource.getDocument(COLLECTION_NEGOCIOS, negocioId)
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
