package com.raymi.app.presentation.vestuarios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.domain.model.EstadoVestuario
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.Vestuario
import com.raymi.app.domain.usecase.vestuario.AddVestuarioUseCase
import com.raymi.app.domain.usecase.vestuario.GetVestuariosUseCase
import com.raymi.app.domain.usecase.vestuario.UpdateVestuarioUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la gestión de vestuarios
 * Maneja la lista de vestuarios, búsqueda y filtros
 */
@HiltViewModel
class VestuariosViewModel @Inject constructor(
    private val getVestuariosUseCase: GetVestuariosUseCase,
    private val addVestuarioUseCase: AddVestuarioUseCase,
    private val updateVestuarioUseCase: UpdateVestuarioUseCase
) : ViewModel() {

    // ========== ESTADOS UI ==========

    private val _uiState = MutableStateFlow(VestuariosUiState())
    val uiState: StateFlow<VestuariosUiState> = _uiState.asStateFlow()

    init {
        loadVestuarios()
    }

    // ========== ACCIONES ==========

    /**
     * Carga la lista de vestuarios
     */
    fun loadVestuarios() {
        viewModelScope.launch {
            getVestuariosUseCase().collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }

                    is Resource.Success -> {
                        val vestuarios = result.data ?: emptyList()
                        _uiState.value = _uiState.value.copy(
                            vestuarios = vestuarios,
                            filteredVestuarios = filterVestuarios(vestuarios),
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

    /**
     * Busca vestuarios por texto
     */
    fun searchVestuarios(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        _uiState.value = _uiState.value.copy(
            filteredVestuarios = filterVestuarios(_uiState.value.vestuarios)
        )
    }

    /**
     * Filtra por estado
     */
    fun filterByEstado(estado: EstadoVestuario?) {
        _uiState.value = _uiState.value.copy(selectedEstado = estado)
        _uiState.value = _uiState.value.copy(
            filteredVestuarios = filterVestuarios(_uiState.value.vestuarios)
        )
    }

    /**
     * Aplica los filtros actuales
     */
    private fun filterVestuarios(vestuarios: List<Vestuario>): List<Vestuario> {
        var filtered = vestuarios

        // Filtrar por búsqueda
        if (_uiState.value.searchQuery.isNotBlank()) {
            val query = _uiState.value.searchQuery
            filtered = filtered.filter { vestuario ->
                vestuario.codigo.contains(query, ignoreCase = true) ||
                        vestuario.danza.contains(query, ignoreCase = true) ||
                        vestuario.departamento.contains(query, ignoreCase = true) ||
                        vestuario.descripcion.contains(query, ignoreCase = true)
            }
        }

        // Filtrar por estado
        _uiState.value.selectedEstado?.let { estado ->
            filtered = filtered.filter { it.estado == estado }
        }

        return filtered
    }

    /**
     * Muestra el diálogo para agregar vestuario
     */
    fun showAddVestuarioDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true)
    }

    /**
     * Oculta el diálogo de agregar vestuario
     */
    fun hideAddVestuarioDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false)
    }

    /**
     * Muestra el diálogo para editar vestuario
     */
    fun showEditVestuarioDialog(vestuario: Vestuario) {
        _uiState.value = _uiState.value.copy(
            showEditDialog = true,
            selectedVestuario = vestuario
        )
    }

    /**
     * Oculta el diálogo de editar vestuario
     */
    fun hideEditVestuarioDialog() {
        _uiState.value = _uiState.value.copy(
            showEditDialog = false,
            selectedVestuario = null
        )
    }

    /**
     * Agrega un nuevo vestuario
     */
    fun addVestuario(vestuario: Vestuario) {
        viewModelScope.launch {
            addVestuarioUseCase(vestuario).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isSaving = true)
                    }

                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            showAddDialog = false,
                            successMessage = "Vestuario agregado correctamente"
                        )
                        loadVestuarios()
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

    /**
     * Actualiza un vestuario existente
     */
    fun updateVestuario(vestuario: Vestuario) {
        viewModelScope.launch {
            updateVestuarioUseCase(vestuario).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isSaving = true)
                    }

                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            showEditDialog = false,
                            selectedVestuario = null,
                            successMessage = "Vestuario actualizado correctamente"
                        )
                        loadVestuarios()
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

    /**
     * Limpia los mensajes
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            error = null,
            successMessage = null
        )
    }
}

/**
 * Estado UI para la pantalla de vestuarios
 */
data class VestuariosUiState(
    val vestuarios: List<Vestuario> = emptyList(),
    val filteredVestuarios: List<Vestuario> = emptyList(),
    val searchQuery: String = "",
    val selectedEstado: EstadoVestuario? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val selectedVestuario: Vestuario? = null,
    val error: String? = null,
    val successMessage: String? = null
)