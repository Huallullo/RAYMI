package com.raymi.app.presentation.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.core.workspace.WorkspaceManager
import com.raymi.app.domain.model.*
import com.raymi.app.domain.usecase.categoria.GetCategoriasUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * ViewModel para la gestión de ítems optimizado para SaaS.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class ItemsViewModel @Inject constructor(
    private val itemRepository: com.raymi.app.domain.repository.ItemRepository,
    private val getCategoriasUseCase: GetCategoriasUseCase,
    private val userSessionManager: com.raymi.app.core.session.UserSessionManager, // ✅ Centralizado
    private val adManager: com.raymi.app.core.ads.AdManager,
    private val workspaceManager: WorkspaceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ItemsUiState(isLoading = true))
    val uiState: StateFlow<ItemsUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategoria = MutableStateFlow<Categoria?>(null)
    private val _allItems = MutableStateFlow<List<Item>>(emptyList())
    private val _allCategorias = MutableStateFlow<List<Categoria>>(emptyList())
    private var lastSnapshot: Any? = null
    private val PAGE_SIZE = 20L

    init {
        observeUserSession()
        
        // Búsqueda y filtrado local REACTIVO con DEBOUNCE
        _searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .combine(_allCategorias) { query, cats -> query to cats }
            .combine(_allItems) { (query, cats), items -> Triple(items, cats, query) }
            .combine(_selectedCategoria) { (items, cats, query), cat -> Quad(items, cats, query, cat) }
            .onEach { (items, cats, query, cat) ->
                _uiState.update { it.copy(
                    itemsFiltrados = aplicarFiltros(items, query, cat),
                    categorias = cats,
                    queryBusqueda = query,
                    categoriaFiltro = cat,
                    isLoading = false
                ) }
            }
            .launchIn(viewModelScope)

        refreshItems()
    }

    private fun observeUserSession() {
        userSessionManager.userPlan
            .onEach { plan -> _uiState.update { it.copy(userPlan = plan) } }
            .launchIn(viewModelScope)
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    fun refreshItems() {
        lastSnapshot = null
        _allItems.value = emptyList()
        loadMore()
    }

    fun loadMore() {
        if (_uiState.value.isLoading) return
        
        viewModelScope.launch {
            val workspaceId = workspaceManager.getWorkspaceId() ?: return@launch
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // 1. Cargar Categorías (Solo si están vacías)
                if (_allCategorias.value.isEmpty()) {
                    launch {
                        getCategoriasUseCase(workspaceId)
                            .filter { it !is Resource.Loading }
                            .take(1)
                            .collect { res ->
                                if (res is Resource.Success) {
                                    val cats = res.data ?: emptyList()
                                    _allCategorias.value = cats
                                }
                            }
                    }
                }

                // 2. Cargar Ítems Paginados
                val res = itemRepository.getItemsByWorkspaceOnce(workspaceId, limit = PAGE_SIZE, lastSnapshot = lastSnapshot)
                if (res is Resource.Success) {
                    val newItems = res.data ?: emptyList()
                    _allItems.value = if (lastSnapshot == null) newItems else _allItems.value + newItems
                    lastSnapshot = res.cursor
                    _uiState.update { it.copy(hasMore = newItems.size >= PAGE_SIZE) }
                } else if (res is Resource.Error) {
                    _uiState.update { it.copy(error = res.message) }
                }
            } catch (e: Exception) {
                android.util.Log.e("ItemsViewModel", "Error: ${e.message}")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun debeMostrarAnuncios(plan: UserPlan?): Boolean = adManager.debeMostrarAnuncios(plan)

    fun buscar(query: String) { _searchQuery.value = query }

    fun filtrarPorCategoria(categoria: Categoria?) { _selectedCategoria.value = categoria }

    fun cargarMas() {
        loadMore()
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

    fun clearMessages() { _uiState.update { it.copy(error = null) } }
}

data class ItemsUiState(
    val itemsFiltrados: List<Item> = emptyList(),
    val categorias: List<Categoria> = emptyList(),
    val categoriaFiltro: Categoria? = null,
    val queryBusqueda: String = "",
    val userPlan: UserPlan? = null,
    val hasMore: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)
