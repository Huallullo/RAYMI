package com.raymi.app.presentation.items

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.core.workspace.WorkspaceManager
import com.raymi.app.domain.model.Categoria
import com.raymi.app.domain.model.Item
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.ItemRepository
import com.raymi.app.domain.usecase.categoria.GetCategoriasUseCase
import com.raymi.app.data.remote.StorageDataSource
import com.raymi.app.domain.usecase.item.DeleteItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel para editar ítems existentes con optimización de Storage.
 */
@HiltViewModel
class EditItemViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val deleteItemUseCase: DeleteItemUseCase,
    private val getCategoriasUseCase: GetCategoriasUseCase,
    private val storageDataSource: StorageDataSource,
    private val workspaceManager: WorkspaceManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val itemId: String = savedStateHandle["itemId"] ?: ""

    private val _uiState = MutableStateFlow(EditItemUiState())
    val uiState: StateFlow<EditItemUiState> = _uiState.asStateFlow()

    init {
        cargarDatos()
    }

    fun cargarDatos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val workspaceId = workspaceManager.getWorkspaceId() ?: return@launch

            // ✅ BUG 3 FIX: Use combine to ensure both categorias and item are loaded before resolving selection
            combine(
                getCategoriasUseCase(workspaceId),
                itemRepository.getItemById(workspaceId, itemId)
            ) { categoriasRes, itemRes ->
                if (categoriasRes is Resource.Success && itemRes is Resource.Success) {
                    val item = itemRes.data ?: return@combine
                    val cats = categoriasRes.data ?: emptyList()
                    
                    _uiState.update { state ->
                        state.copy(
                            categorias = cats,
                            itemOriginal = item,
                            nombre = item.nombre,
                            descripcion = item.descripcion,
                            codigo = item.codigo,
                            precio = item.precio.toString(),
                            cantidad = item.cantidad,
                            atributos = item.atributos,
                            categoriaSeleccionada = cats.find { it.id == item.categoriaId },
                            isLoading = false
                        )
                    }
                } else if (categoriasRes is Resource.Error || itemRes is Resource.Error) {
                    val msg = (categoriasRes as? Resource.Error)?.message ?: (itemRes as? Resource.Error)?.message ?: "Error"
                    _uiState.update { it.copy(isLoading = false, error = msg) }
                }
            }.collect()
        }
    }

    fun onNombreChange(nombre: String) = _uiState.update { it.copy(nombre = nombre) }
    fun onDescripcionChange(descripcion: String) = _uiState.update { it.copy(descripcion = descripcion) }
    fun onCodigoChange(codigo: String) = _uiState.update { it.copy(codigo = codigo) }
    fun onPrecioChange(precio: String) = _uiState.update { it.copy(precio = precio) }
    fun onCantidadChange(cantidad: Int) = _uiState.update { it.copy(cantidad = cantidad) }
    fun onCategoriaChange(categoria: Categoria) = _uiState.update { it.copy(categoriaSeleccionada = categoria) }
    fun onImageSelected(uri: Uri?) = _uiState.update { it.copy(newImageUri = uri) }

    fun onAtributoChange(clave: String, valor: String) {
        val nuevosAtributos = _uiState.value.atributos.toMutableMap()
        nuevosAtributos[clave] = valor
        _uiState.update { it.copy(atributos = nuevosAtributos) }
    }

    fun eliminarAtributo(clave: String) {
        val nuevosAtributos = _uiState.value.atributos.toMutableMap()
        nuevosAtributos.remove(clave)
        _uiState.update { it.copy(atributos = nuevosAtributos) }
    }

    fun actualizarItem() {
        val state = _uiState.value
        val original = state.itemOriginal ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                var imageUrl = original.imagenUrl
                state.newImageUri?.let { uri ->
                    val workspaceId = workspaceManager.getWorkspaceId() ?: throw Exception("Sin sesión")
                    val userId = workspaceManager.currentWorkspace.value?.ownerId ?: throw Exception("Propietario del negocio no identificado")
                    
                    // 1. Borrar imagen anterior si existe (Ahorro de espacio)
                    original.imagenUrl?.let { oldUrl ->
                        if (oldUrl.isNotBlank()) {
                            storageDataSource.getPathFromUrl(oldUrl)?.let { path ->
                                storageDataSource.deleteFile(path)
                            }
                        }
                    }

                    // 2. Subir nueva imagen (Auto-comprimida en StorageDataSource)
                    val path = "users/$userId/negocios/$workspaceId/items/${UUID.randomUUID()}.webp"
                    imageUrl = storageDataSource.uploadFile(path, uri)
                }

                val updatedItem = original.copy(
                    nombre = state.nombre,
                    descripcion = state.descripcion,
                    precio = state.precio.toDoubleOrNull() ?: original.precio,
                    cantidad = state.cantidad,
                    categoriaId = state.categoriaSeleccionada?.id ?: original.categoriaId,
                    atributos = state.atributos,
                    imagenUrl = imageUrl
                )

                itemRepository.updateItem(updatedItem).collect { result ->
                    if (result is Resource.Success) {
                        _uiState.update { it.copy(isSaving = false, isSuccess = true) }
                    } else if (result is Resource.Error) {
                        _uiState.update { it.copy(isSaving = false, error = result.message) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.localizedMessage) }
            }
        }
    }
}

data class EditItemUiState(
    val itemOriginal: Item? = null,
    val nombre: String = "",
    val descripcion: String = "",
    val codigo: String = "",
    val precio: String = "",
    val cantidad: Int = 1,
    val categorias: List<Categoria> = emptyList(),
    val categoriaSeleccionada: Categoria? = null,
    val atributos: Map<String, String> = emptyMap(),
    val newImageUri: Uri? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)
