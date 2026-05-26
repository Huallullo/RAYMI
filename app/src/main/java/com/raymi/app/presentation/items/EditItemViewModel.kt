package com.raymi.app.presentation.items

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.core.workspace.WorkspaceManager
import com.raymi.app.domain.model.Categoria
import com.raymi.app.domain.model.Item
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.usecase.categoria.GetCategoriasUseCase
import com.raymi.app.domain.usecase.item.GetItemsUseCase
import com.raymi.app.domain.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para editar la información de un producto existente.
 * Permite actualizar categorías, precios y atributos dinámicos.
 */
@HiltViewModel
class EditItemViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val getCategoriasUseCase: GetCategoriasUseCase,
    private val workspaceManager: WorkspaceManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val itemId: String = savedStateHandle["itemId"] ?: ""
    
    private val _uiState = MutableStateFlow(EditItemUiState())
    val uiState: StateFlow<EditItemUiState> = _uiState.asStateFlow()

    init {
        cargarDatos()
    }

    private fun cargarDatos() {
        viewModelScope.launch {
            try {
                val workspaceId = workspaceManager.getWorkspaceId()
                if (workspaceId == null) {
                    _uiState.update { it.copy(error = "Negocio no identificado", isLoading = false) }
                    return@launch
                }
                
                // 1. Cargar Categorías
                launch {
                    getCategoriasUseCase(workspaceId).collect { result ->
                        if (result is Resource.Success) {
                            _uiState.update { it.copy(categorias = result.data ?: emptyList()) }
                        }
                    }
                }

                // 2. Cargar el Ítem a editar
                itemRepository.getItemById(workspaceId, itemId).collect { result ->
                    when (result) {
                        is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                        is Resource.Success -> {
                            val item = result.data ?: return@collect
                            _uiState.update { state ->
                                state.copy(
                                    itemOriginal = item,
                                    nombre = item.nombre,
                                    codigo = item.codigo,
                                    precio = item.precio.toString(),
                                    cantidad = item.cantidad,
                                    categoriaSeleccionada = state.categorias.find { it.id == item.categoriaId },
                                    atributos = item.atributos,
                                    isLoading = false
                                )
                            }
                        }
                        is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudo identificar el negocio activo") }
            }
        }
    }

    fun onNombreChange(v: String) = _uiState.update { it.copy(nombre = v) }
    fun onCodigoChange(v: String) = _uiState.update { it.copy(codigo = v) }
    fun onPrecioChange(v: String) = _uiState.update { it.copy(precio = v) }
    fun onCantidadChange(v: Int) = _uiState.update { it.copy(cantidad = v) }
    fun onCategoriaChange(v: Categoria) = _uiState.update { it.copy(categoriaSeleccionada = v) }
    
    fun onAtributoChange(clave: String, valor: String) {
        val nuevos = _uiState.value.atributos.toMutableMap()
        nuevos[clave] = valor
        _uiState.update { it.copy(atributos = nuevos) }
    }

    fun eliminarAtributo(clave: String) {
        val nuevos = _uiState.value.atributos.toMutableMap()
        nuevos.remove(clave)
        _uiState.update { it.copy(atributos = nuevos) }
    }

    fun actualizarItem() {
        val state = _uiState.value
        val original = state.itemOriginal ?: return

        val itemActualizado = original.copy(
            nombre = state.nombre,
            codigo = state.codigo,
            precio = state.precio.toDoubleOrNull() ?: 0.0,
            cantidad = state.cantidad,
            categoriaId = state.categoriaSeleccionada?.id ?: "",
            atributos = state.atributos
        )

        viewModelScope.launch {
            itemRepository.updateItem(itemActualizado).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isSaving = true) }
                    is Resource.Success -> _uiState.update { it.copy(isSaving = false, isSuccess = true) }
                    is Resource.Error -> _uiState.update { it.copy(isSaving = false, error = result.message) }
                }
            }
        }
    }
}

data class EditItemUiState(
    val itemOriginal: Item? = null,
    val nombre: String = "",
    val codigo: String = "",
    val precio: String = "",
    val cantidad: Int = 1,
    val categorias: List<Categoria> = emptyList(),
    val categoriaSeleccionada: Categoria? = null,
    val atributos: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)
