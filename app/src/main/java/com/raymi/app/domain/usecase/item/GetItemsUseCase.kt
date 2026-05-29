package com.raymi.app.domain.usecase.item

import com.raymi.app.domain.model.Item
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Caso de uso para obtener todos los ítems de un espacio de trabajo.
 */
class GetItemsUseCase @Inject constructor(
    private val repository: ItemRepository
) {
    suspend operator fun invoke(
        workspaceId: String,
        limit: Long = 100,
        startAfterValue: Any? = null
    ): Flow<Resource<List<Item>>> {
        return repository.getItemsByWorkspace(workspaceId, limit, startAfterValue)
    }
}
