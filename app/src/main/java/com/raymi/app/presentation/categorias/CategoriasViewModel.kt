package com.raymi.app.presentation.categorias

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.core.workspace.WorkspaceManager
import com.raymi.app.domain.model.Categoria
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.usecase.categoria.AddCategoriaUseCase
import com.raymi.app.domain.usecase.categoria.GetCategoriasUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriasViewModel @Inject constructor(
    private val getCategoriasUseCase: GetCategoriasUseCase,
    private val addCategoriaUseCase: AddCategoriaUseCase,
    private val workspaceManager: WorkspaceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriasUiState())
    val uiState: StateFlow<CategoriasUiState> = _uiState.asStateFlow()

    init {
        cargarCategorias()
    }

    fun cargarCategorias() {
        viewModelScope.launch {
            try {
                val workspaceId = workspaceManager.getWorkspaceId()
                if (workspaceId == null) {
                    _uiState.update { it.copy(isLoading = false, categorias = emptyList()) }
                    return@launch
                }
                
                getCategoriasUseCase(workspaceId).collect { result ->
                    when (result) {
                        is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                        is Resource.Success -> _uiState.update { it.copy(categorias = result.data ?: emptyList(), isLoading = false) }
                        is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "No hay negocio seleccionado") }
            }
        }
    }

    fun agregarCategoria(nombre: String) {
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isBlank()) {
            _uiState.update { it.copy(error = "El nombre es obligatorio") }
            return
        }
        
        viewModelScope.launch {
            try {
                val workspaceId = workspaceManager.getWorkspaceId()
                if (workspaceId == null) {
                    _uiState.update { it.copy(error = "No hay negocio seleccionado") }
                    return@launch
                }

                val nueva = Categoria(
                    workspaceId = workspaceId,
                    nombre = nombreLimpio.split(" ").joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } },
                    activa = true
                )
                addCategoriaUseCase(nueva).collect { result ->
                    when (result) {
                        is Resource.Loading -> _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }
                        is Resource.Success -> _uiState.update { it.copy(isLoading = false, successMessage = "Categoría creada con éxito") }
                        is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    fun clearMessages() = _uiState.update { it.copy(error = null) }
}

data class CategoriasUiState(
    val categorias: List<Categoria> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
