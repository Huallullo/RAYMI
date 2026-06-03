package com.raymi.app.domain.usecase.item

import com.raymi.app.domain.model.Item
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.ItemRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * Caso de uso para actualizar un ítem.
 * [C-12] Implementación de validaciones de dominio.
 */
class UpdateItemUseCase @Inject constructor(
    private val repository: ItemRepository
) {
    operator fun invoke(item: Item): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        
        if (item.id.isBlank()) { emit(Resource.Error("ID de ítem inválido")); return@flow }
        if (item.nombre.isBlank()) { emit(Resource.Error("El nombre es obligatorio")); return@flow }
        if (item.precio <= 0) { emit(Resource.Error("El precio debe ser mayor a 0")); return@flow }
        
        // No permitir reducir el stock total por debajo de lo que está fuera alquilado
        if (item.cantidad < item.unidadesAlquiladas) {
            emit(Resource.Error("No puedes reducir el stock total (${item.cantidad}) por debajo de las unidades actualmente alquiladas (${item.unidadesAlquiladas})"))
            return@flow
        }
        
        emitAll(repository.updateItem(item))
    }
}
