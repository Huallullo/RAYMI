package com.raymi.app.data.repository

import com.raymi.app.core.utils.Constants.COLLECTION_NEGOCIOS
import com.raymi.app.data.model.dto.WorkspaceDto
import com.raymi.app.data.remote.FirebaseDataSource
import com.raymi.app.data.remote.WorkspaceDataSource
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.Workspace
import com.raymi.app.domain.repository.WorkspaceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class WorkspaceRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseDataSource,
    private val workspaceDataSource: WorkspaceDataSource
) : WorkspaceRepository {

    override suspend fun getWorkspacesByUser(userId: String): Flow<Resource<List<Workspace>>> = flow {
        emit(Resource.Loading())
        try {
            val response = dataSource.queryDocuments(COLLECTION_NEGOCIOS, "ownerUid", userId)
            val workspaces = response.map { (id, data) -> WorkspaceDto.fromMap(id, data).toDomain() }
            emit(Resource.Success(workspaces))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error("Error al obtener negocios: ${e.message}"))
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
            if (e is CancellationException) throw e
            emit(Resource.Error("Error al obtener datos del negocio: ${e.message}"))
        }
    }

    override suspend fun createWorkspace(workspace: Workspace): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            val dto = WorkspaceDto.fromDomain(workspace)
            val data = dto.toMap().filterValues { it != null }.mapValues { it.value!! }
            val id = workspaceDataSource.createWorkspaceAtomic(data, emptyMap(), workspace.ownerId)
            emit(Resource.Success(id))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val msg = e.localizedMessage ?: ""
            val error = when {
                msg.contains("PERMISSION_DENIED") -> "Error de permisos: No puedes crear más negocios o no tienes acceso. Verifica tu plan."
                msg.contains("network") -> "Error de red: No se pudo conectar al servidor."
                else -> "Error al crear negocio: ${e.message}"
            }
            emit(Resource.Error(error))
        }
    }

    override suspend fun updateWorkspace(workspace: Workspace): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val dto = WorkspaceDto.fromDomain(workspace)
            val data = dto.toMap().filterValues { it != null }.mapValues { it.value!! }
            dataSource.updateDocument(COLLECTION_NEGOCIOS, workspace.id, data)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error("Error al actualizar negocio: ${e.message}"))
        }
    }

    override suspend fun deleteWorkspace(workspaceId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            dataSource.deleteDocument(COLLECTION_NEGOCIOS, workspaceId)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error("Error al eliminar negocio: ${e.message}"))
        }
    }

    override suspend fun getCurrentWorkspace(userId: String): Flow<Resource<Workspace?>> = flow {
        emit(Resource.Loading())
        try {
            // El primer negocio que encuentre para este ADMIN
            val response = dataSource.queryDocuments(COLLECTION_NEGOCIOS, "ownerUid", userId)
            if (response.isNotEmpty()) {
                val (id, data) = response.first()
                emit(Resource.Success(WorkspaceDto.fromMap(id, data).toDomain()))
            } else {
                emit(Resource.Success(null))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error("Error: ${e.message}"))
        }
    }

    override suspend fun updateStats(workspaceId: String, data: Map<String, Any>) {
        dataSource.updateBusinessDocument("metadata", "stats", data, workspaceId)
    }
}
