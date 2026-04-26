package com.raymi.app.presentation.clientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.domain.model.Cliente
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.usecase.cliente.AddClienteUseCase
import com.raymi.app.domain.usecase.cliente.DeleteClienteUseCase
import com.raymi.app.domain.usecase.cliente.GetClientesUseCase
import com.raymi.app.domain.usecase.cliente.UpdateClienteUseCase
import com.raymi.app.domain.usecase.reniec.ConsultarReniecUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClientesViewModel @Inject constructor(
    private val getClientesUseCase: GetClientesUseCase,
    private val addClienteUseCase: AddClienteUseCase,
    private val updateClienteUseCase: UpdateClienteUseCase,
    private val deleteClienteUseCase: DeleteClienteUseCase,
    private val consultarReniecUseCase: ConsultarReniecUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClientesUiState())
    val uiState: StateFlow<ClientesUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        loadClientes()
    }

    fun loadClientes() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            getClientesUseCase().collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }

                    is Resource.Success -> {
                        val clientes = result.data ?: emptyList()
                        _uiState.value = _uiState.value.copy(
                            clientes = clientes,
                            filteredClientes = applySearchFilter(clientes, _uiState.value.searchQuery),
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

    fun searchClientes(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredClientes = applySearchFilter(_uiState.value.clientes, query)
        )
    }

    private fun applySearchFilter(clientes: List<Cliente>, query: String): List<Cliente> {
        if (query.isBlank()) return clientes
        return clientes.filter { cliente ->
            cliente.nombre.contains(query, ignoreCase = true) ||
                    cliente.apellidos.contains(query, ignoreCase = true) ||
                    cliente.dni.contains(query, ignoreCase = true) ||
                    cliente.telefono.contains(query, ignoreCase = true)
        }
    }

    fun showAddClienteDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true)
    }

    fun hideAddClienteDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false)
    }

    fun showEditClienteDialog(cliente: Cliente) {
        _uiState.value = _uiState.value.copy(
            showEditDialog = true,
            selectedCliente = cliente
        )
    }

    fun hideEditClienteDialog() {
        _uiState.value = _uiState.value.copy(
            showEditDialog = false,
            selectedCliente = null
        )
    }

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
                        // sin recarga manual: realtime lo actualiza
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

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            error = null,
            successMessage = null
        )
    }

    fun consultarReniec(dni: String, onResult: (Result<com.raymi.app.data.remote.ReniecData>) -> Unit) {
        viewModelScope.launch {
            consultarReniecUseCase(dni).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        // Loading state handled in dialog
                    }
                    is Resource.Success -> {
                        resource.data?.let { data ->
                            onResult(Result.success(data))
                        }
                    }
                    is Resource.Error -> {
                        onResult(Result.failure(Exception(resource.message)))
                    }
                }
            }
        }
    }
}

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