package com.raymi.app.domain.usecase.categoria

import com.raymi.app.domain.model.Categoria
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.CategoriaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AddCategoriaUseCase @Inject constructor(
    private val repository: CategoriaRepository
) {
    suspend operator fun invoke(categoria: Categoria): Flow<Resource<String>> {
        return repository.addCategoria(categoria)
    }
}
