package com.raymi.app.domain.usecase.item

import com.raymi.app.domain.model.Item
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Caso de uso para agregar un nuevo ítem.
 */
class AddItemUseCase @Inject constructor(
    private val repository: ItemRepository
) {
    suspend operator fun invoke(item: Item): Flow<Resource<String>> {
        return repository.addItem(item)
    }
}
