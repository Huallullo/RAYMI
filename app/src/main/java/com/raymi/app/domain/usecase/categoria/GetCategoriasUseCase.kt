package com.raymi.app.domain.usecase.categoria

import com.raymi.app.domain.model.Categoria
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.CategoriaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoriasUseCase @Inject constructor(
    private val repository: CategoriaRepository
) {
    operator fun invoke(workspaceId: String): Flow<Resource<List<Categoria>>> {
        return repository.getCategorias(workspaceId)
    }

    suspend fun invalidarCache(workspaceId: String) {
        repository.invalidarCache(workspaceId)
    }
}
