package com.raymi.app.presentation.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.core.workspace.WorkspaceManager
import com.raymi.app.domain.model.*
import com.raymi.app.domain.usecase.categoria.GetCategoriasUseCase
import com.raymi.app.domain.usecase.item.GetItemsByWorkspaceOnceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * ViewModel para la gestión de ítems optimizado para SaaS.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ItemsViewModel @Inject constructor(
    private val itemRepository: com.raymi.app.domain.repository.ItemRepository,
    private val getItemsByWorkspaceOnceUseCase: GetItemsByWorkspaceOnceUseCase,
    private val getCategoriasUseCase: GetCategoriasUseCase,
    private val userPlanRepository: com.raymi.app.domain.repository.UserPlanRepository,
    private val auth: com.google.firebase.auth.FirebaseAuth,
    private val adManager: com.raymi.app.core.ads.AdManager,
    private val workspaceManager: WorkspaceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ItemsUiState(isLoading = true))
    val uiState: StateFlow<ItemsUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategoria = MutableStateFlow<Categoria?>(null)
    private val _limit = MutableStateFlow(100L)
    private val _allItems = MutableStateFlow<List<Item>>(emptyList())
    private val _allCategorias = MutableStateFlow<List<Categoria>>(emptyList())

    init {
        loadUserPlan()
        
        combine(_allItems, _allCategorias, _searchQuery, _selectedCategoria) { items, cats, query, cat ->
            Quad(items, cats, query, cat)
        }.onEach { (items, cats, query, cat) ->
            _uiState.update { it.copy(
                itemsFiltrados = aplicarFiltros(items, query, cat),
                categorias = cats,
                queryBusqueda = query,
                categoriaFiltro = cat,
                isLoading = false
            ) }
        }.launchIn(viewModelScope)

        refreshItems()
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    private fun loadUserPlan() {
        viewModelScope.launch {
            auth.uid?.let { uid ->
                userPlanRepository.getUserPlan(uid).collect { result ->
                    if (result is Resource.Success) {
                        _uiState.update { it.copy(userPlan = result.data) }
                    }
                }
            }
        }
    }

    fun refreshItems() {
        viewModelScope.launch {
            val workspaceId = workspaceManager.getWorkspaceId() ?: return@launch
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // 1. Cargar Categorías (Snapshot - No bloqueante)
                launch {
                    getCategoriasUseCase(workspaceId)
                        .filter { it !is Resource.Loading }
                        .take(1)
                        .collect { res ->
                            if (res is Resource.Success) {
                                val cats = res.data ?: emptyList()
                                _allCategorias.value = cats
                                _uiState.update { it.copy(categorias = cats) }
                            }
                        }
                }

                itemRepository.invalidateCache(workspaceId)

                // 2. Cargar Ítems
                val res = getItemsByWorkspaceOnceUseCase(workspaceId, _limit.value)
                if (res is Resource.Success) {
                    _allItems.value = res.data ?: emptyList()
                } else {
                    _uiState.update { it.copy(isLoading = false, error = (res as? Resource.Error)?.message) }
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
        _limit.value += 100
        refreshItems()
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

    fun limpiarError() { _uiState.update { it.copy(error = null) } }
}

data class ItemsUiState(
    val items: List<Item> = emptyList(),
    val itemsFiltrados: List<Item> = emptyList(),
    val categorias: List<Categoria> = emptyList(),
    val categoriaFiltro: Categoria? = null,
    val queryBusqueda: String = "",
    val userPlan: UserPlan? = null,
    val hasMore: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)
