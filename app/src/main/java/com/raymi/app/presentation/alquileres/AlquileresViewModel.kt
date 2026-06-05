package com.raymi.app.presentation.alquileres

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.core.workspace.WorkspaceManager
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.EstadoAlquiler
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.usecase.alquiler.GetAlquileresUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class AlquileresViewModel @Inject constructor(
    private val getAlquileresUseCase: GetAlquileresUseCase,
    private val getAlquileresOnceUseCase: com.raymi.app.domain.usecase.alquiler.GetAlquileresOnceUseCase,
    private val alquilerRepository: com.raymi.app.domain.repository.AlquilerRepository,
    private val userSessionManager: com.raymi.app.core.session.UserSessionManager, // ✅ Inyectado
    private val adManager: com.raymi.app.core.ads.AdManager,
    private val workspaceManager: WorkspaceManager
) : ViewModel() {

    private var observeJob: Job? = null
    private var lastSnapshot: Any? = null
    private val PAGE_SIZE = 20L
    
    private val _uiState = MutableStateFlow(AlquileresUiState())
    val uiState: StateFlow<AlquileresUiState> = _uiState.asStateFlow()

    private val _allAlquileres = MutableStateFlow<List<Alquiler>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _selectedEstado = MutableStateFlow<EstadoAlquiler?>(null)

    fun debeMostrarAnuncios(): Boolean = adManager.debeMostrarAnuncios(_uiState.value.userPlan)

    init {
        observeUserSession()
        
        // ✅ BUSCADOR REACTIVO: Sincronizado con Alquileres, Query y Estado
        combine(_allAlquileres, _searchQuery, _selectedEstado) { lista, query, estado ->
            Triple(lista, query, estado)
        }.onEach { (lista, query, estado) ->
            val filtrados = filterAlquileres(lista, query, estado)
            _uiState.update { state ->
                state.copy(
                    alquileres = lista,
                    filteredAlquileres = filtrados,
                    searchQuery = query,
                    selectedEstado = estado
                )
            }
        }.launchIn(viewModelScope)

        refreshAlquileres()
    }

    private fun observeUserSession() {
        userSessionManager.userPlan
            .onEach { plan -> _uiState.update { it.copy(userPlan = plan) } }
            .launchIn(viewModelScope)
    }

    /**
     * Reinicia y carga la primera página de alquileres.
     */
    fun refreshAlquileres() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            lastSnapshot = null
            loadMore()
        }
    }

    fun loadMore() {
        viewModelScope.launch {
            try {
                val workspaceId = workspaceManager.getWorkspaceId() ?: return@launch
                _uiState.update { it.copy(isLoading = true) }
                
                val result = getAlquileresOnceUseCase(workspaceId, limit = PAGE_SIZE, lastSnapshot = lastSnapshot)
                when (result) {
                    is Resource.Success -> {
                        val newData = result.data ?: emptyList()
                        val updatedList = if (lastSnapshot == null) newData else _allAlquileres.value + newData
                        lastSnapshot = result.cursor
                        
                        _allAlquileres.value = updatedList
                        _uiState.update { it.copy(hasMore = newData.size >= PAGE_SIZE) }
                        verificarVencidos(newData)
                    }
                    is Resource.Error -> _uiState.update { it.copy(error = result.message) }
                    else -> {}
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al cargar datos") }
            } finally {
                delay(500)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun verificarVencidos(alquileres: List<Alquiler>) {
        val vencidosIds = alquileres.filter { it.estaVencido && it.estado == EstadoAlquiler.ACTIVO }
            .map { it.id }
        
        if (vencidosIds.isEmpty()) return

        viewModelScope.launch {
            val workspaceId = workspaceManager.getWorkspaceId() ?: return@launch
            alquilerRepository.updateAlquileresEstadoBatch(workspaceId, vencidosIds, EstadoAlquiler.VENCIDO).collect()
            
            // Actualizar estado localmente
            val updatedList = _allAlquileres.value.map { alq ->
                if (vencidosIds.contains(alq.id)) alq.copy(estado = EstadoAlquiler.VENCIDO) else alq
            }
            _allAlquileres.value = updatedList
        }
    }

    fun searchAlquileres(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun filterByEstado(estado: EstadoAlquiler?) {
        _selectedEstado.value = estado
    }

    private fun filterAlquileres(
        alquileres: List<Alquiler>, 
        query: String, 
        estado: EstadoAlquiler?
    ): List<Alquiler> {
        return alquileres.filter { alquiler ->
            val matchQuery = query.isBlank() || 
                alquiler.clienteNombre.contains(query, ignoreCase = true) ||
                alquiler.itemNombre.contains(query, ignoreCase = true) ||
                alquiler.itemCodigo.contains(query, ignoreCase = true)
            
            val matchEstado = estado == null || 
                (estado == EstadoAlquiler.VENCIDO && alquiler.estaVencido) ||
                (estado != EstadoAlquiler.VENCIDO && alquiler.estado == estado)
                
            matchQuery && matchEstado
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        observeJob?.cancel()
    }
}

data class AlquileresUiState(
    val alquileres: List<Alquiler> = emptyList(),
    val filteredAlquileres: List<Alquiler> = emptyList(),
    val searchQuery: String = "",
    val selectedEstado: EstadoAlquiler? = null,
    val userPlan: com.raymi.app.domain.model.UserPlan? = null,
    val hasMore: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
