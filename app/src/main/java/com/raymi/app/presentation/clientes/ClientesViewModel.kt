package com.raymi.app.presentation.clientes

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.data.remote.StorageDataSource
import com.raymi.app.domain.model.Cliente
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.usecase.cliente.AddClienteUseCase
import com.raymi.app.domain.usecase.cliente.DeleteClienteUseCase
import com.raymi.app.domain.usecase.cliente.GetClientesOnceUseCase
import com.raymi.app.domain.usecase.cliente.UpdateClienteUseCase
import com.raymi.app.domain.usecase.reniec.ConsultarReniecUseCase
import com.raymi.app.data.remote.ReniecData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la gestión inteligente de clientes optimizado para SaaS.
 * Incluye respaldo de identidad (Fotos DNI y Rostro) para máxima seguridad.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class ClientesViewModel @Inject constructor(
    private val getClientesOnceUseCase: GetClientesOnceUseCase,
    private val addClienteUseCase: AddClienteUseCase,
    private val updateClienteUseCase: UpdateClienteUseCase,
    private val deleteClienteUseCase: DeleteClienteUseCase,
    private val consultarReniecUseCase: ConsultarReniecUseCase,
    private val planLimitsUseCase: com.raymi.app.domain.usecase.auth.PlanLimitsUseCase, // OPTIMIZACIÓN: Consolidar límites
    private val userSessionManager: com.raymi.app.core.session.UserSessionManager, // ✅ Centralizado
    private val auth: com.google.firebase.auth.FirebaseAuth,
    private val storageDataSource: StorageDataSource,
    private val adManager: com.raymi.app.core.ads.AdManager,
    private val workspaceManager: com.raymi.app.core.workspace.WorkspaceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClientesUiState())
    val uiState: StateFlow<ClientesUiState> = _uiState.asStateFlow()

    private var allClientes = emptyList<Cliente>()
    private val _searchQuery = MutableStateFlow("")
    private val _orden = MutableStateFlow(OrdenCliente.RECIBIENTES)
    private var lastSnapshot: Any? = null
    private val PAGE_SIZE = 20L

    fun debeMostrarAnuncios(): Boolean = adManager.debeMostrarAnuncios(_uiState.value.userPlan)

    init {
        observeUserSession()
        
        // OPTIMIZACIÓN: Debounce en búsqueda para evitar recomposiciones innecesarias
        _searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .combine(_orden) { query, orden -> query to orden }
            .onEach { (query, orden) ->
                // NOTA: Si el usuario busca, invalidamos la paginación para búsqueda global o manejamos local
                val filtrados = aplicarLogicaFiltro(allClientes, query, orden)
                _uiState.update { state ->
                    state.copy(
                        searchQuery = query,
                        orden = orden,
                        filteredClientes = filtrados,
                        visibleClientes = filtrados.take(state.visibleLimit),
                        hasMoreClientes = filtrados.size > state.visibleLimit
                    )
                }
            }.launchIn(viewModelScope)

        refreshClientes()
    }

    private fun observeUserSession() {
        userSessionManager.userPlan
            .onEach { plan -> _uiState.update { it.copy(userPlan = plan) } }
            .launchIn(viewModelScope)
    }

    fun refreshClientes() {
        lastSnapshot = null
        allClientes = emptyList()
        loadMore()
    }

    fun loadMore() {
        if (_uiState.value.isLoading) return
        
        viewModelScope.launch {
            val workspaceId = workspaceManager.getWorkspaceId() ?: return@launch
            _uiState.update { it.copy(isLoading = true) }
            
            val result = getClientesOnceUseCase(workspaceId, limit = PAGE_SIZE, lastSnapshot = lastSnapshot)
            if (result is Resource.Success) {
                val newItems = result.data ?: emptyList()
                allClientes = if (lastSnapshot == null) newItems else allClientes + newItems
                lastSnapshot = result.cursor
                
                _uiState.update { state ->
                    val filtrados = aplicarLogicaFiltro(allClientes, state.searchQuery, state.orden)
                    state.copy(
                        clientes = allClientes,
                        filteredClientes = filtrados,
                        visibleClientes = filtrados,
                        hasMoreClientes = newItems.size >= PAGE_SIZE,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }

    fun searchClientes(query: String) {
        _searchQuery.value = query
    }

    fun cambiarOrden(nuevoOrden: OrdenCliente) {
        _orden.value = nuevoOrden
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

    /**
     * Agrega un cliente con sus respectivos respaldos visuales de identidad.
     */
    fun addCliente(
        cliente: Cliente,
        dniFront: Uri? = null,
        dniBack: Uri? = null,
        face: Uri? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val workspaceId = workspaceManager.getWorkspaceId()
                if (workspaceId == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Error: Negocio no identificado") }
                    return@launch
                }

                val userId = auth.uid
                if (userId == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Error: Sesión de usuario no válida") }
                    return@launch
                }

                val canAdd = planLimitsUseCase.canAddMoreClients(userId, workspaceId)
                if (!canAdd) {
                    _uiState.update { it.copy(isLoading = false, error = "Has alcanzado el límite de clientes de tu plan. ¡Pásate a PRO para registro ilimitado!") }
                    return@launch
                }

                _uiState.update { it.copy(error = null) } // Limpiar errores previos antes de subir fotos
                var finalCliente = cliente.copy(workspaceId = workspaceId)

                // Subir fotos de seguridad si existen
                dniFront?.let {
                    val url = storageDataSource.uploadFile("negocios/$workspaceId/clientes/${cliente.dni}_front.webp", it)
                    finalCliente = finalCliente.copy(fotoDniFrontUrl = url)
                }
                dniBack?.let {
                    val url = storageDataSource.uploadFile("negocios/$workspaceId/clientes/${cliente.dni}_back.webp", it)
                    finalCliente = finalCliente.copy(fotoDniBackUrl = url)
                }
                face?.let {
                    val url = storageDataSource.uploadFile("negocios/$workspaceId/clientes/${cliente.dni}_face.webp", it)
                    finalCliente = finalCliente.copy(fotoRostroUrl = url)
                }

                addClienteUseCase(finalCliente).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            _uiState.update { it.copy(showAddDialog = false, isLoading = false, successMessage = "Cliente registrado con éxito") }
                            refreshClientes()
                        }
                        is Resource.Error -> {
                            _uiState.update { it.copy(isLoading = false, error = result.message) }
                        }
                        is Resource.Loading -> {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ClientesViewModel", "Error al agregar cliente", e)
                _uiState.update { it.copy(isLoading = false, error = "Error inesperado: ${e.localizedMessage}") }
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
            
            // Buscar cliente para borrar sus fotos
            val target = allClientes.find { it.id == clienteId }
            target?.let { c ->
                listOfNotNull(c.fotoDniFrontUrl, c.fotoDniBackUrl, c.fotoRostroUrl).forEach { url ->
                    storageDataSource.getPathFromUrl(url)?.let { path -> storageDataSource.deleteFile(path) }
                }
            }

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
        val state = _uiState.value
        // ✅ BUG 2 FIX: Trigger real fetch if Firestore has more pages
        if (state.hasMoreClientes && !state.isLoading) {
            loadMore()
        } else {
            // Local expansion for windowing
            _uiState.update { 
                it.copy(
                    visibleLimit = it.visibleLimit + 50,
                    visibleClientes = it.filteredClientes.take(it.visibleLimit + 50)
                )
            }
        }
    }

    fun clearMessages() = _uiState.update { it.copy(error = null, successMessage = null) }

    fun showCameraPermissionAlert() = _uiState.update { it.copy(showCameraPermissionAlert = true) }
    fun dismissCameraPermissionAlert() = _uiState.update { it.copy(showCameraPermissionAlert = false) }

    fun consultarReniec(dni: String, onResult: (Resource<ReniecData>) -> Unit) {
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
    val successMessage: String? = null,
    val showCameraPermissionAlert: Boolean = false
)
