package com.raymi.app.presentation.historial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.core.workspace.WorkspaceManager
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.EstadoAlquiler
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.usecase.alquiler.GetAlquileresUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para el Historial de Movimientos.
 * Diseño Senior: Filtros avanzados y cálculo de métricas históricas.
 */
@HiltViewModel
class HistorialViewModel @Inject constructor(
    private val getAlquileresUseCase: GetAlquileresUseCase,
    private val workspaceManager: WorkspaceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistorialUiState())
    val uiState: StateFlow<HistorialUiState> = _uiState.asStateFlow()

    init {
        cargarHistorial()
    }

    fun cargarHistorial() {
        viewModelScope.launch {
            try {
                val workspaceId = workspaceManager.getWorkspaceId() ?: return@launch
                getAlquileresUseCase(workspaceId).collect { result ->
                    when (result) {
                        is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                        is Resource.Success -> {
                            val todos = result.data ?: emptyList()
                            val concluidos = todos.filter { 
                                it.estado == EstadoAlquiler.DEVUELTO || it.estado == EstadoAlquiler.CANCELADO 
                            }.sortedByDescending { it.updatedAt }
                            
                            _uiState.update { it.copy(
                                allAlquileres = concluidos,
                                filteredAlquileres = concluidos,
                                totalRecaudado = concluidos.sumOf { it.adelanto },
                                isLoading = false 
                            ) }
                        }
                        is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(error = "No se pudo cargar el historial del negocio") }
            }
        }
    }

    fun filtrar(query: String) {
        _uiState.update { state ->
            val filtrados = state.allAlquileres.filter { 
                it.clienteNombre.contains(query, ignoreCase = true) || 
                it.itemNombre.contains(query, ignoreCase = true)
            }
            state.copy(query = query, filteredAlquileres = filtrados)
        }
    }
}

data class HistorialUiState(
    val allAlquileres: List<Alquiler> = emptyList(),
    val filteredAlquileres: List<Alquiler> = emptyList(),
    val query: String = "",
    val totalRecaudado: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null
)
