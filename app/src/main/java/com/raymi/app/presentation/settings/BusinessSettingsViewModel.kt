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
        workspaceManager.currentWorkspace
            .filterNotNull()
            .onEach { workspace ->
                _uiState.update { it.copy(
                    nombre = workspace.nombre,
                    nombreComercial = workspace.nombreComercial,
                    ruc = workspace.ruc,
                    direccion = workspace.direccion,
                    telefono = workspace.telefono,
                    descripcion = workspace.descripcion,
                    moneda = workspace.moneda,
                    tipoNegocio = workspace.tipoNegocio,
                    serieTicket = workspace.serieTicket,
                    serieBoleta = workspace.serieBoleta,
                    serieFactura = workspace.serieFactura,
                    terminosCondiciones = workspace.terminosCondiciones,
                    politicaPenalidades = workspace.politicaPenalidades
                ) }
            }
            .launchIn(viewModelScope)
    }

    fun onNombreChange(v: String) = _uiState.update { it.copy(nombre = v) }
    fun onNombreComercialChange(v: String) = _uiState.update { it.copy(nombreComercial = v) }
    fun onRucChange(v: String) = _uiState.update { it.copy(ruc = v) }
    fun onDireccionChange(v: String) = _uiState.update { it.copy(direccion = v) }
    fun onTelefonoChange(v: String) = _uiState.update { it.copy(telefono = v) }
    fun onDescripcionChange(v: String) = _uiState.update { it.copy(descripcion = v) }
    fun onMonedaChange(v: String) = _uiState.update { it.copy(moneda = v) }
    fun onSerieTicketChange(v: String) = _uiState.update { it.copy(serieTicket = v) }
    fun onSerieBoletaChange(v: String) = _uiState.update { it.copy(serieBoleta = v) }
    fun onSerieFacturaChange(v: String) = _uiState.update { it.copy(serieFactura = v) }
    fun onTerminosChange(v: String) = _uiState.update { it.copy(terminosCondiciones = v) }
    fun onPoliticaChange(v: String) = _uiState.update { it.copy(politicaPenalidades = v) }

    fun guardarCambios() {
        val current = workspaceManager.currentWorkspace.value ?: return
        val updated = current.copy(
            nombre = _uiState.value.nombre,
            nombreComercial = _uiState.value.nombreComercial,
            ruc = _uiState.value.ruc,
            direccion = _uiState.value.direccion,
            telefono = _uiState.value.telefono,
            descripcion = _uiState.value.descripcion,
            moneda = _uiState.value.moneda,
            serieTicket = _uiState.value.serieTicket,
            serieBoleta = _uiState.value.serieBoleta,
            serieFactura = _uiState.value.serieFactura,
            terminosCondiciones = _uiState.value.terminosCondiciones,
            politicaPenalidades = _uiState.value.politicaPenalidades
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
    val nombreComercial: String = "",
    val ruc: String = "",
    val direccion: String = "",
    val telefono: String = "",
    val descripcion: String = "",
    val moneda: String = "PEN",
    val tipoNegocio: String = "",
    val serieTicket: String = "T001",
    val serieBoleta: String = "B001",
    val serieFactura: String = "F001",
    val terminosCondiciones: String = "",
    val politicaPenalidades: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)
