package com.raymi.app.domain.usecase.item

import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DeleteItemUseCase @Inject constructor(
    private val repository: ItemRepository
) {
    suspend operator fun invoke(workspaceId: String, itemId: String, codigo: String): Flow<Resource<Unit>> {
        return repository.deleteItem(workspaceId, itemId, codigo)
    }
}
