package com.raymi.app.presentation.items.mantenimiento

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.core.workspace.WorkspaceManager
import com.raymi.app.domain.model.Mantenimiento
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.MantenimientoRepository
import com.raymi.app.domain.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MantenimientoViewModel @Inject constructor(
    private val maintenanceRepository: MantenimientoRepository,
    private val itemRepository: ItemRepository,
    private val workspaceManager: WorkspaceManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val itemId: String = savedStateHandle["itemId"] ?: ""
    
    private val _uiState = MutableStateFlow(MantenimientoUiState())
    val uiState: StateFlow<MantenimientoUiState> = _uiState.asStateFlow()

    init {
        loadMantenimientos()
    }

    fun loadMantenimientos() {
        viewModelScope.launch {
            val workspaceId = workspaceManager.getWorkspaceId() ?: return@launch
            maintenanceRepository.getMantenimientosByItem(workspaceId, itemId).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Resource.Success -> _uiState.update { it.copy(isLoading = false, mantenimientos = result.data ?: emptyList()) }
                    is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun addMantenimiento(mantenimiento: Mantenimiento) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val workspaceId = workspaceManager.getWorkspaceId() ?: return@launch
            
            maintenanceRepository.addMantenimiento(mantenimiento.copy(itemId = itemId, workspaceId = workspaceId)).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        if (mantenimiento.estadoFinal == "MANTENIMIENTO") {
                            actualizarEstadoItem("MANTENIMIENTO")
                        } else if (mantenimiento.estadoFinal == "OPERATIVO") {
                            actualizarEstadoItem("DISPONIBLE")
                        }
                        _uiState.update { it.copy(isProcessing = false, successMessage = "Mantenimiento registrado") }
                        loadMantenimientos()
                    }
                    is Resource.Error -> _uiState.update { it.copy(isProcessing = false, error = result.message) }
                    else -> {}
                }
            }
        }
    }

    private fun actualizarEstadoItem(nuevoEstado: String) {
        viewModelScope.launch {
            val workspaceId = workspaceManager.getWorkspaceId() ?: return@launch
            // ✅ FIX PROBLEM 9: Usar actualización parcial en lugar de lectura completa
            itemRepository.updateEstadoItem(workspaceId, itemId, nuevoEstado)
        }
    }

    fun clearMessages() = _uiState.update { it.copy(error = null, successMessage = null) }
}

data class MantenimientoUiState(
    val mantenimientos: List<Mantenimiento> = emptyList(),
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
