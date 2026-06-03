package com.raymi.app.domain.usecase.alquiler

import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.DomainError
import com.raymi.app.domain.model.EstadoCliente
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.AlquilerRepository
import com.raymi.app.domain.repository.ClienteRepository
import com.raymi.app.domain.repository.ItemRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * Caso de uso para crear un nuevo alquiler
 * [C-11] Incluye verificación de stock y estado del cliente en el dominio.
 */
class CreateAlquilerUseCase @Inject constructor(
    private val alquilerRepository: AlquilerRepository,
    private val itemRepository: ItemRepository,
    private val clienteRepository: ClienteRepository
) {
    operator fun invoke(alquiler: Alquiler): Flow<Resource<String>> = flow {
        emit(Resource.Loading())

        // 1. Validaciones de datos básicos
        if (alquiler.clienteId.isBlank()) { emit(Resource.Error("Debe seleccionar un cliente")); return@flow }
        if (alquiler.itemId.isBlank()) { emit(Resource.Error("Debe seleccionar un producto")); return@flow }
        if (alquiler.precioTotal <= 0) { emit(Resource.Error("El precio debe ser mayor a 0")); return@flow }
        if (alquiler.adelanto < 0) { emit(Resource.Error(DomainError.NegativeBalance.message)); return@flow }
        if (alquiler.adelanto > alquiler.precioTotal) { emit(Resource.Error("El adelanto no puede superar el precio total")); return@flow }
        if (alquiler.fechaFinPrevista.seconds <= alquiler.fechaInicio.seconds) { emit(Resource.Error(DomainError.InvalidDateRange.message)); return@flow }

        // 2. VERIFICACIÓN DE STOCK (CRÍTICO)
        val itemResult = itemRepository.getItemById(alquiler.workspaceId, alquiler.itemId).first { it !is Resource.Loading }
        val item = (itemResult as? Resource.Success)?.data
            ?: run { emit(Resource.Error("Producto no encontrado en inventario")); return@flow }
        
        val disponibles = item.cantidad - item.unidadesAlquiladas
        if (disponibles < alquiler.cantidad) {
            emit(Resource.Error("Stock insuficiente. Disponibles: $disponibles, Solicitadas: ${alquiler.cantidad}"))
            return@flow
        }

        // 3. VERIFICACIÓN DE ESTADO DEL CLIENTE
        val clienteResult = clienteRepository.getClienteById(alquiler.clienteId).first { it !is Resource.Loading }
        val cliente = (clienteResult as? Resource.Success)?.data
            ?: run { emit(Resource.Error("Cliente no encontrado")); return@flow }
        
        if (cliente.estado == EstadoCliente.BLOQUEADO) {
            emit(Resource.Error("El cliente está BLOQUEADO. Resuelva su situación antes de un nuevo alquiler."))
            return@flow
        }

        // Si pasa las validaciones, crear el alquiler
        emitAll(alquilerRepository.createAlquiler(alquiler))
    }
}
