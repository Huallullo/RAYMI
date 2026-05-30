package com.raymi.app.presentation.clientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.domain.model.Cliente
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.usecase.cliente.AddClienteUseCase
import com.raymi.app.domain.usecase.cliente.DeleteClienteUseCase
import com.raymi.app.domain.usecase.cliente.GetClientesOnceUseCase
import com.raymi.app.domain.usecase.cliente.UpdateClienteUseCase
import com.raymi.app.domain.usecase.reniec.ConsultarReniecUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la gestión inteligente de clientes optimizado para SaaS.
 * Uso de Snapshots para ahorro de costos y búsqueda local.
 */
@HiltViewModel
class ClientesViewModel @Inject constructor(
    private val getClientesOnceUseCase: GetClientesOnceUseCase,
    private val addClienteUseCase: AddClienteUseCase,
    private val updateClienteUseCase: UpdateClienteUseCase,
    private val deleteClienteUseCase: DeleteClienteUseCase,
    private val consultarReniecUseCase: ConsultarReniecUseCase,
    private val userPlanRepository: com.raymi.app.domain.repository.UserPlanRepository,
    private val auth: com.google.firebase.auth.FirebaseAuth,
    private val adManager: com.raymi.app.core.ads.AdManager,
    private val workspaceManager: com.raymi.app.core.workspace.WorkspaceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClientesUiState())
    val uiState: StateFlow<ClientesUiState> = _uiState.asStateFlow()

    private var allClientes = emptyList<Cliente>()

    fun debeMostrarAnuncios(): Boolean = adManager.debeMostrarAnuncios(_uiState.value.userPlan)

    init {
        refreshClientes()
        loadUserPlan()
    }

    private fun loadUserPlan() {
        viewModelScope.launch {
            auth.uid?.let { uid ->
                userPlanRepository.getUserPlan(uid).collect { result ->
                    if (result is Resource.Success) {
                        _uiState.update { it.copy(userPlan = result.data) }
                    }
                }
            }
        }
    }

    fun refreshClientes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = getClientesOnceUseCase()
            if (result is Resource.Success) {
                allClientes = result.data ?: emptyList()
                _uiState.update { state ->
                    val filtrados = aplicarLogicaFiltro(allClientes, state.searchQuery, state.orden)
                    state.copy(
                        clientes = allClientes,
                        filteredClientes = filtrados,
                        visibleClientes = filtrados.take(state.visibleLimit),
                        hasMoreClientes = filtrados.size > state.visibleLimit,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }

    fun searchClientes(query: String) {
        _uiState.update { state ->
            val filtrados = aplicarLogicaFiltro(allClientes, query, state.orden)
            state.copy(
                searchQuery = query,
                filteredClientes = filtrados,
                visibleClientes = filtrados.take(state.visibleLimit),
                hasMoreClientes = filtrados.size > state.visibleLimit
            )
        }
    }

    fun cambiarOrden(nuevoOrden: OrdenCliente) {
        _uiState.update { state ->
            val filtrados = aplicarLogicaFiltro(allClientes, state.searchQuery, nuevoOrden)
            state.copy(
                orden = nuevoOrden,
                filteredClientes = filtrados,
                visibleClientes = filtrados.take(state.visibleLimit)
            )
        }
    }

    private fun aplicarLogicaFiltro(lista: List<Cliente>, query: String, orden: OrdenCliente): List<Cliente> {
        val filtrada = if (query.isBlank()) lista else {
            lista.filter { it.nombreCompleto.contains(query, ignoreCase = true) || it.dni.contains(query) }
        }
        
        return when (orden) {
            OrdenCliente.RECIBIENTES -> filtrada.sortedByDescending { it.createdAt }
            OrdenCliente.ALFABETICO -> filtrada.sortedBy { it.nombreCompleto }
            OrdenCliente.ANTIGUOS -> filtrada.sortedBy { it.createdAt }
        }
    }

    fun showAddClienteDialog() = _uiState.update { it.copy(showAddDialog = true) }
    fun hideAddClienteDialog() = _uiState.update { it.copy(showAddDialog = false) }
    fun addCliente(cliente: Cliente) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val currentWorkspaceId = workspaceManager.getWorkspaceId()
            if (currentWorkspaceId == null) {
                _uiState.update { it.copy(isLoading = false, error = "Negocio no identificado") }
                return@launch
            }

            val userId = auth.uid ?: return@launch
            val canAdd = userPlanRepository.canAddMoreClients(userId, currentWorkspaceId)
            if (!canAdd) {
                _uiState.update { it.copy(
                    isLoading = false, 
                    error = "Límite de clientes alcanzado en Plan FREE."
                ) }
                return@launch
            }

            val clienteConWorkspace = if (cliente.workspaceId.isBlank()) {
                cliente.copy(workspaceId = currentWorkspaceId)
            } else {
                cliente
            }

            addClienteUseCase(clienteConWorkspace).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update { it.copy(showAddDialog = false, isLoading = false, successMessage = "Cliente registrado") }
                        refreshClientes()
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                    else -> {}
                }
            }
        }
    }

    fun updateCliente(cliente: Cliente) {
        viewModelScope.launch {
            updateClienteUseCase(cliente).collect { result ->
                if (result is Resource.Success) {
                    _uiState.update { it.copy(successMessage = "Actualizado") }
                    refreshClientes()
                }
            }
        }
    }

    fun eliminarCliente(clienteId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            deleteClienteUseCase(clienteId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update { it.copy(isLoading = false, successMessage = "Cliente eliminado") }
                        refreshClientes()
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                    else -> {}
                }
            }
        }
    }

    fun loadMoreClientes() {
        _uiState.update { state ->
            val nuevoLimite = state.visibleLimit + 50
            state.copy(
                visibleLimit = nuevoLimite,
                visibleClientes = state.filteredClientes.take(nuevoLimite),
                hasMoreClientes = state.filteredClientes.size > nuevoLimite
            )
        }
    }

    fun clearMessages() = _uiState.update { it.copy(error = null, successMessage = null) }

    fun consultarReniec(dni: String, onResult: (com.raymi.app.domain.model.Resource<com.raymi.app.data.remote.ReniecData>) -> Unit) {
        viewModelScope.launch {
            consultarReniecUseCase(dni).collect { resource ->
                onResult(resource)
            }
        }
    }
}

enum class OrdenCliente { RECIBIENTES, ALFABETICO, ANTIGUOS }

data class ClientesUiState(
    val clientes: List<Cliente> = emptyList(),
    val filteredClientes: List<Cliente> = emptyList(),
    val visibleClientes: List<Cliente> = emptyList(),
    val searchQuery: String = "",
    val orden: OrdenCliente = OrdenCliente.RECIBIENTES,
    val visibleLimit: Int = 50,
    val hasMoreClientes: Boolean = false,
    val userPlan: com.raymi.app.domain.model.UserPlan? = null,
    val isLoading: Boolean = false,
    val showAddDialog: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
