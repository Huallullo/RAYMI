package com.raymi.app.domain.usecase.workspace

import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.Workspace
import com.raymi.app.domain.repository.WorkspaceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Caso de uso para actualizar la configuración de un negocio.
 */
class UpdateWorkspaceUseCase @Inject constructor(
    private val repository: WorkspaceRepository
) {
    suspend operator fun invoke(workspace: Workspace): Flow<Resource<Unit>> {
        return repository.updateWorkspace(workspace)
    }
}
