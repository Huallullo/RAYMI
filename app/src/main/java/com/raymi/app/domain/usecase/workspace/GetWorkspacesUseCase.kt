package com.raymi.app.domain.usecase.workspace

import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.Workspace
import com.raymi.app.domain.repository.WorkspaceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetWorkspacesUseCase @Inject constructor(
    private val repository: WorkspaceRepository
) {
    suspend operator fun invoke(userId: String): Flow<Resource<List<Workspace>>> {
        return repository.getWorkspacesByUser(userId)
    }
}
