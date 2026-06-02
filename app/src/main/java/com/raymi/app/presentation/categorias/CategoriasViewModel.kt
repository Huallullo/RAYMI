package com.raymi.app.presentation.categorias

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.core.workspace.WorkspaceManager
import com.raymi.app.domain.model.Categoria
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.usecase.categoria.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriasViewModel @Inject constructor(
    private val getCategoriasUseCase: GetCategoriasUseCase,
    private val addCategoriaUseCase: AddCategoriaUseCase,
    private val updateCategoriaUseCase: UpdateCategoriaUseCase,
    private val deleteCategoriaUseCase: DeleteCategoriaUseCase,
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

        // Validación de unicidad local (QA Fix)
        if (_uiState.value.categorias.any { it.nombre.equals(nombreLimpio, ignoreCase = true) }) {
            _uiState.update { it.copy(error = "Ya existe una categoría con el nombre '$nombreLimpio'") }
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
                    nombre = formanteatNombre(nombreLimpio),
                    activa = true
                )
                addCategoriaUseCase(nueva).collect { result ->
                    handleResourceResult(result, "Categoría creada con éxito")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    fun editarCategoria(categoria: Categoria, nuevoNombre: String) {
        val nombreLimpio = nuevoNombre.trim()
        if (nombreLimpio.isBlank() || nombreLimpio == categoria.nombre) return

        if (_uiState.value.categorias.any { it.id != categoria.id && it.nombre.equals(nombreLimpio, ignoreCase = true) }) {
            _uiState.update { it.copy(error = "Ya existe otra categoría con el nombre '$nombreLimpio'") }
            return
        }

        viewModelScope.launch {
            updateCategoriaUseCase(categoria.copy(nombre = formanteatNombre(nombreLimpio))).collect { result ->
                handleResourceResult(result, "Categoría actualizada")
            }
        }
    }

    fun eliminarCategoria(categoria: Categoria) {
        viewModelScope.launch {
            deleteCategoriaUseCase(categoria.workspaceId, categoria.id).collect { result ->
                handleResourceResult(result, "Categoría eliminada")
            }
        }
    }

    private fun <T> handleResourceResult(result: Resource<T>, successMsg: String) {
        when (result) {
            is Resource.Loading -> _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            is Resource.Success -> {
                // ✅ COSTO 3 FIX: Removed cargarCategorias() redundant call. 
                // The real-time listener (if configured) or the invalidated cache refresh will handle it.
                _uiState.update { it.copy(isLoading = false, successMessage = successMsg) }
            }
            is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
        }
    }

    private fun formanteatNombre(nombre: String): String {
        return nombre.split(" ").filter { it.isNotBlank() }
            .joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }
    }

    fun clearMessages() = _uiState.update { it.copy(error = null, successMessage = null) }
}

data class CategoriasUiState(
    val categorias: List<Categoria> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
