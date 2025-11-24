package com.raymi.app.presentation.clientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.domain.model.Cliente
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.usecase.cliente.AddClienteUseCase
import com.raymi.app.domain.usecase.cliente.DeleteClienteUseCase
import com.raymi.app.domain.usecase.cliente.GetClientesUseCase
import com.raymi.app.domain.usecase.cliente.UpdateClienteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la gestión de clientes
 * Maneja la lista de clientes, búsqueda y operaciones CRUD
 */
@HiltViewModel
class ClientesViewModel @Inject constructor(
    private val getClientesUseCase: GetClientesUseCase,
    private val addClienteUseCase: AddClienteUseCase,
    private val updateClienteUseCase: UpdateClienteUseCase,
    private val deleteClienteUseCase: DeleteClienteUseCase
) : ViewModel() {

    // ========== ESTADOS UI ==========

    private val _uiState = MutableStateFlow(ClientesUiState())
    val uiState: StateFlow<ClientesUiState> = _uiState.asStateFlow()

    init {
        loadClientes()
    }

    // ========== ACCIONES ==========

    /**
     * Carga la lista de clientes
     */
    fun loadClientes() {
        viewModelScope.launch {
            getClientesUseCase().collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }

                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            clientes = result.data ?: emptyList(),
                            filteredClientes = result.data ?: emptyList(),
                            isLoading = false,
                            error = null
                        )
                    }

                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Busca clientes por texto
     */
    fun searchClientes(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)

        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(
                filteredClientes = _uiState.value.clientes
            )
            return
        }

        val filtered = _uiState.value.clientes.filter { cliente ->
            cliente.nombre.contains(query, ignoreCase = true) ||
                    cliente.apellidos.contains(query, ignoreCase = true) ||
                    cliente.dni.contains(query, ignoreCase = true) ||
                    cliente.telefono.contains(query, ignoreCase = true)
        }

        _uiState.value = _uiState.value.copy(filteredClientes = filtered)
    }

    /**
     * Muestra el diálogo para agregar cliente
     */
    fun showAddClienteDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true)
    }

    /**
     * Oculta el diálogo de agregar cliente
     */
    fun hideAddClienteDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false)
    }

    /**
     * Muestra el diálogo para editar cliente
     */
    fun showEditClienteDialog(cliente: Cliente) {
        _uiState.value = _uiState.value.copy(
            showEditDialog = true,
            selectedCliente = cliente
        )
    }

    /**
     * Oculta el diálogo de editar cliente
     */
    fun hideEditClienteDialog() {
        _uiState.value = _uiState.value.copy(
            showEditDialog = false,
            selectedCliente = null
        )
    }

    /**
     * Agrega un nuevo cliente
     */
    fun addCliente(cliente: Cliente) {
        viewModelScope.launch {
            addClienteUseCase(cliente).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isSaving = true)
                    }

                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            showAddDialog = false,
                            successMessage = "Cliente agregado correctamente"
                        )
                        loadClientes()
                    }

                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Actualiza un cliente existente
     */
    fun updateCliente(cliente: Cliente) {
        viewModelScope.launch {
            updateClienteUseCase(cliente).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isSaving = true)
                    }

                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            showEditDialog = false,
                            selectedCliente = null,
                            successMessage = "Cliente actualizado correctamente"
                        )
                        loadClientes()
                    }

                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Elimina un cliente
     */
    fun deleteCliente(clienteId: String) {
        viewModelScope.launch {
            deleteClienteUseCase(clienteId).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isDeleting = true)
                    }

                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isDeleting = false,
                            successMessage = "Cliente eliminado correctamente"
                        )
                        loadClientes()
                    }

                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isDeleting = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Limpia los mensajes
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            error = null,
            successMessage = null
        )
    }
}

/**
 * Estado UI para la pantalla de clientes
 */
data class ClientesUiState(
    val clientes: List<Cliente> = emptyList(),
    val filteredClientes: List<Cliente> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val selectedCliente: Cliente? = null,
    val error: String? = null,
    val successMessage: String? = null
)