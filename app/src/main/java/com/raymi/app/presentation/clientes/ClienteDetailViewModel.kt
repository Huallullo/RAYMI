// ========== ClienteDetailViewModel.kt ==========
package com.raymi.app.presentation.clientes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.Cliente
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.usecase.alquiler.GetAlquileresUseCase
import com.raymi.app.domain.usecase.cliente.GetClienteByIdUseCase
import com.raymi.app.domain.usecase.cliente.UpdateClienteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClienteDetailViewModel @Inject constructor(
    private val getClienteByIdUseCase: GetClienteByIdUseCase,
    private val updateClienteUseCase: UpdateClienteUseCase,
    private val deleteClienteUseCase: com.raymi.app.domain.usecase.cliente.DeleteClienteUseCase,
    private val getAlquileresByClienteUseCase: com.raymi.app.domain.usecase.alquiler.GetAlquileresByClienteUseCase,
    private val workspaceManager: com.raymi.app.core.workspace.WorkspaceManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val clienteId: String = savedStateHandle["clienteId"] ?: ""

    private val _uiState = MutableStateFlow(ClienteDetailUiState())
    val uiState: StateFlow<ClienteDetailUiState> = _uiState.asStateFlow()

    init {
        loadClienteData()
    }

    fun loadClienteData() {
        viewModelScope.launch {
            // Cargar datos del cliente
            getClienteByIdUseCase(clienteId).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            cliente = result.data,
                            isLoading = false
                        )
                        // Cargar alquileres del cliente
                        loadAlquileres()
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

    private fun loadAlquileres() {
        viewModelScope.launch {
            val workspaceId = workspaceManager.getWorkspaceId() ?: return@launch
            getAlquileresByClienteUseCase(workspaceId, clienteId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val alquileres = result.data ?: emptyList()

                        val totalAlquileres = alquileres.size
                        val totalGastado = alquileres.sumOf { it.precioTotal }

                        _uiState.value = _uiState.value.copy(
                            alquileres = alquileres,
                            totalAlquileres = totalAlquileres,
                            totalGastado = totalGastado
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = result.message
                        )
                    }
                    is Resource.Loading -> {}
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Actualiza los datos del cliente
     */
    fun updateCliente(cliente: Cliente) {
        viewModelScope.launch {
            updateClienteUseCase(cliente).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            successMessage = "Cliente actualizado correctamente"
                        )
                        // Recargar datos
                        loadClienteData()
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

    fun eliminarCliente(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            deleteClienteUseCase(clienteId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update { it.copy(isLoading = false, successMessage = "Cliente eliminado") }
                        onSuccess()
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                    is Resource.Loading -> { }
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
}

data class ClienteDetailUiState(
    val cliente: Cliente? = null,
    val alquileres: List<Alquiler> = emptyList(),
    val totalAlquileres: Int = 0,
    val totalGastado: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
