package com.raymi.app.domain.usecase.item

import com.raymi.app.domain.model.Item
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.ItemRepository
import com.raymi.app.domain.usecase.auth.PlanLimitsUseCase
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * Caso de uso para agregar un nuevo ítem.
 * [C-10] Implementación de validaciones y límites de plan server-side (dominio).
 */
class AddItemUseCase @Inject constructor(
    private val repository: ItemRepository,
    private val planLimitsUseCase: PlanLimitsUseCase
) {
    operator fun invoke(item: Item, userId: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        
        // Validaciones de negocio
        if (item.nombre.isBlank()) { emit(Resource.Error("El nombre es obligatorio")); return@flow }
        if (item.precio <= 0) { emit(Resource.Error("El precio debe ser mayor a 0")); return@flow }
        if (item.cantidad <= 0) { emit(Resource.Error("La cantidad debe ser al menos 1")); return@flow }
        if (item.workspaceId.isBlank()) { emit(Resource.Error("Workspace no seleccionado")); return@flow }
        if (item.categoriaId.isBlank()) { emit(Resource.Error("Selecciona una categoría")); return@flow }
        if (item.codigo.isBlank()) { emit(Resource.Error("El código no puede estar vacío")); return@flow }
        
        // Verificar límite de plan
        val canAdd = planLimitsUseCase.canAddMoreItems(userId, item.workspaceId)
        if (!canAdd) { 
            emit(Resource.Error("Has alcanzado el límite de ítems de tu plan. ¡Pásate a PRO para registro ilimitado!"))
            return@flow 
        }
        
        emitAll(repository.addItem(item))
    }
}
