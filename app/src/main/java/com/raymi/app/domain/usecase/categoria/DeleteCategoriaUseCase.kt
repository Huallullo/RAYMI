package com.raymi.app.domain.usecase.categoria

import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.CategoriaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DeleteCategoriaUseCase @Inject constructor(
    private val repository: CategoriaRepository
) {
    suspend operator fun invoke(workspaceId: String, categoriaId: String): Flow<Resource<Unit>> {
        return repository.deleteCategoria(workspaceId, categoriaId)
    }
}
