package com.raymi.app.presentation.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.core.workspace.WorkspaceManager
import com.raymi.app.domain.model.Categoria
import com.raymi.app.domain.model.Item
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.usecase.categoria.GetCategoriasUseCase
import com.raymi.app.domain.usecase.item.GetItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la gestión de ítems (productos/servicios de alquiler).
 * Diseño Senior: Soporta filtrado por categorías y búsqueda multi-campo.
 */
@HiltViewModel
class ItemsViewModel @Inject constructor(
    private val getItemsUseCase: GetItemsUseCase,
    private val getCategoriasUseCase: GetCategoriasUseCase,
    private val workspaceManager: WorkspaceManager
) : ViewModel() {

    private var loadJob: Job? = null
    
    private val _uiState = MutableStateFlow(ItemsUiState())
    val uiState: StateFlow<ItemsUiState> = _uiState.asStateFlow()

    init {
        cargarDatos()
    }

    fun cargarDatos() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            workspaceManager.currentWorkspace.collectLatest { workspace ->
                if (workspace != null) {
                    val workspaceId = workspace.id
                    
                    // QA Fix: Corrutinas paralelas para flujos continuos
                    launch {
                        getCategoriasUseCase(workspaceId).collect { result ->
                            if (result is Resource.Success) {
                                _uiState.update { it.copy(categorias = result.data ?: emptyList()) }
                            }
                        }
                    }

                    launch {
                        cargarItems(workspaceId)
                    }
                } else {
                    _uiState.update { it.copy(error = "Negocio no identificado") }
                }
            }
        }
    }

    private suspend fun cargarItems(workspaceId: String) {
        getItemsUseCase(workspaceId).collect { result ->
            when (result) {
                is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                is Resource.Success -> {
                    val data = result.data ?: emptyList()
                    _uiState.update { 
                        it.copy(
                            items = data,
                            itemsFiltrados = aplicarFiltros(data, it.queryBusqueda, it.categoriaFiltro),
                            isLoading = false 
                        )
                    }
                }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }

    fun buscar(query: String) {
        _uiState.update { 
            it.copy(
                queryBusqueda = query,
                itemsFiltrados = aplicarFiltros(it.items, query, it.categoriaFiltro)
            )
        }
    }

    fun filtrarPorCategoria(categoria: Categoria?) {
        _uiState.update { 
            it.copy(
                categoriaFiltro = categoria,
                itemsFiltrados = aplicarFiltros(it.items, it.queryBusqueda, categoria)
            )
        }
    }

    private fun aplicarFiltros(items: List<Item>, query: String, categoria: Categoria?): List<Item> {
        return items.filter { item ->
            val matchQuery = query.isBlank() || 
                item.nombre.contains(query, ignoreCase = true) || 
                item.codigo.contains(query, ignoreCase = true)
            
            val matchCat = categoria == null || item.categoriaId == categoria.id
            
            matchQuery && matchCat
        }
    }

    fun limpiarError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class ItemsUiState(
    val items: List<Item> = emptyList(),
    val itemsFiltrados: List<Item> = emptyList(),
    val categorias: List<Categoria> = emptyList(),
    val categoriaFiltro: Categoria? = null,
    val queryBusqueda: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)
