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
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la gestión de alquileres bajo arquitectura SaaS.
 * Maneja la lista de contratos, búsquedas y filtrado por estado.
 */
@HiltViewModel
class AlquileresViewModel @Inject constructor(
    private val getAlquileresUseCase: GetAlquileresUseCase,
    private val workspaceManager: WorkspaceManager
) : ViewModel() {

    private var observeJob: Job? = null
    
    private val _uiState = MutableStateFlow(AlquileresUiState())
    val uiState: StateFlow<AlquileresUiState> = _uiState.asStateFlow()

    init {
        loadAlquileres()
    }

    /**
     * Carga los alquileres del negocio actual.
     */
    fun loadAlquileres() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            try {
                val workspaceId = workspaceManager.getWorkspaceId()
                if (workspaceId == null) {
                    _uiState.update { it.copy(isLoading = false, alquileres = emptyList(), filteredAlquileres = emptyList()) }
                    return@launch
                }
                
                getAlquileresUseCase(workspaceId).collect { result ->
                    when (result) {
                        is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                        is Resource.Success -> {
                            val data = result.data ?: emptyList()
                            _uiState.update { 
                                it.copy(
                                    alquileres = data,
                                    filteredAlquileres = filterAlquileres(data, it.searchQuery, it.selectedEstado),
                                    isLoading = false 
                                )
                            }
                        }
                        is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "No se pudo identificar el negocio activo") }
            }
        }
    }

    fun searchAlquileres(query: String) {
        _uiState.update { 
            it.copy(
                searchQuery = query,
                filteredAlquileres = filterAlquileres(it.alquileres, query, it.selectedEstado)
            )
        }
    }

    fun filterByEstado(estado: EstadoAlquiler?) {
        _uiState.update { 
            it.copy(
                selectedEstado = estado,
                filteredAlquileres = filterAlquileres(it.alquileres, it.searchQuery, estado)
            )
        }
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
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
