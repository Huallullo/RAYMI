package com.raymi.app.domain.usecase.workspace

import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.Workspace
import com.raymi.app.domain.repository.WorkspaceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCurrentWorkspaceUseCase @Inject constructor(
    private val repository: WorkspaceRepository
) {
    suspend operator fun invoke(userId: String): Flow<Resource<Workspace?>> {
        return repository.getCurrentWorkspace(userId)
    }
}
