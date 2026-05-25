package com.raymi.app.domain.usecase.workspace

import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.Workspace
import com.raymi.app.domain.repository.WorkspaceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Caso de uso para crear un nuevo espacio de trabajo (negocio).
 */
class CreateWorkspaceUseCase @Inject constructor(
    private val repository: WorkspaceRepository
) {
    /**
     * Ejecuta la creación del workspace.
     * @param workspace Objeto con la información del nuevo negocio.
     * @return Un Flow con el estado del recurso y el ID generado.
     */
    suspend operator fun invoke(workspace: Workspace): Flow<Resource<String>> {
        return repository.createWorkspace(workspace)
    }
}
