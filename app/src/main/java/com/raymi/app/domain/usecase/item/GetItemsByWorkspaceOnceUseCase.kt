package com.raymi.app.domain.usecase.item

import com.raymi.app.domain.model.Item
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.ItemRepository
import javax.inject.Inject

class GetItemsByWorkspaceOnceUseCase @Inject constructor(
    private val repository: ItemRepository
) {
    suspend operator fun invoke(workspaceId: String, limit: Long = 100): Resource<List<Item>> {
        return repository.getItemsByWorkspaceOnce(workspaceId, limit)
    }
}
