package com.raymi.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.core.workspace.WorkspaceManager
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.Workspace
import com.raymi.app.domain.usecase.workspace.UpdateWorkspaceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BusinessSettingsViewModel @Inject constructor(
    private val workspaceManager: WorkspaceManager,
    private val updateWorkspaceUseCase: UpdateWorkspaceUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusinessSettingsUiState())
    val uiState: StateFlow<BusinessSettingsUiState> = _uiState.asStateFlow()

    init {
        // Cargamos los datos actuales del negocio desde el manager
        workspaceManager.currentWorkspace.value?.let { workspace ->
            _uiState.update { it.copy(
                nombre = workspace.nombre,
                descripcion = workspace.descripcion,
                moneda = workspace.moneda,
                tipoNegocio = workspace.tipoNegocio
            ) }
        }
    }

    fun onNombreChange(v: String) = _uiState.update { it.copy(nombre = v) }
    fun onDescripcionChange(v: String) = _uiState.update { it.copy(descripcion = v) }
    fun onMonedaChange(v: String) = _uiState.update { it.copy(moneda = v) }

    fun guardarCambios() {
        val current = workspaceManager.currentWorkspace.value ?: return
        val updated = current.copy(
            nombre = _uiState.value.nombre,
            descripcion = _uiState.value.descripcion,
            moneda = _uiState.value.moneda
        )

        viewModelScope.launch {
            updateWorkspaceUseCase(updated).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Resource.Success -> {
                        workspaceManager.setWorkspace(updated)
                        _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    }
                    is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }
}

data class BusinessSettingsUiState(
    val nombre: String = "",
    val descripcion: String = "",
    val moneda: String = "PEN",
    val tipoNegocio: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)
