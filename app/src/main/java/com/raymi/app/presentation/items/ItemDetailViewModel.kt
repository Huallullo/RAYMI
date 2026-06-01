package com.raymi.app.presentation.items

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.core.workspace.WorkspaceManager
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.Item
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.ItemRepository
import com.raymi.app.domain.usecase.item.DeleteItemUseCase
import com.raymi.app.domain.usecase.alquiler.GetAlquileresByItemUseCase
import com.raymi.app.data.remote.StorageDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val getAlquileresByItemUseCase: GetAlquileresByItemUseCase,
    private val deleteItemUseCase: DeleteItemUseCase,
    private val storageDataSource: StorageDataSource,
    private val workspaceManager: WorkspaceManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ItemDetailUiState())
    val uiState: StateFlow<ItemDetailUiState> = _uiState.asStateFlow()

    private val itemId: String = savedStateHandle["itemId"] ?: ""

    init {
        if (itemId.isNotBlank()) loadItem(itemId)
    }

    fun loadItem(itemId: String) {
        viewModelScope.launch {
            val workspaceId = workspaceManager.getWorkspaceId() ?: return@launch
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            itemRepository.getItemById(workspaceId, itemId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update { it.copy(item = result.data, isLoading = false) }
                        cargarHistorial(workspaceId, itemId)
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(error = result.message, isLoading = false) }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun cargarHistorial(workspaceId: String, itemId: String) {
        viewModelScope.launch {
            getAlquileresByItemUseCase(workspaceId, itemId).collect { result ->
                if (result is Resource.Success) {
                    _uiState.update { it.copy(historial = result.data ?: emptyList()) }
                }
            }
        }
    }

    fun eliminarItem(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val workspaceId = workspaceManager.getWorkspaceId() ?: return@launch
            val item = _uiState.value.item ?: return@launch
            
            // 1. Borrar imagen si existe
            item.imagenUrl?.let { url ->
                storageDataSource.getPathFromUrl(url)?.let { path ->
                    storageDataSource.deleteFile(path)
                }
            }
            
            // 2. Borrar documento
            deleteItemUseCase(workspaceId, itemId, item.codigo).collect { result ->
                if (result is Resource.Success) onSuccess()
                else if (result is Resource.Error) _uiState.update { it.copy(error = result.message, isLoading = false) }
            }
        }
    }

    fun clearMessages() = _uiState.update { it.copy(error = null) }
}

data class ItemDetailUiState(
    val item: Item? = null,
    val historial: List<Alquiler> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
