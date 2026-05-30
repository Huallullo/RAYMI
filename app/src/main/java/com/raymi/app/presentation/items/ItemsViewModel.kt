package com.raymi.app.presentation.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.core.workspace.WorkspaceManager
import com.raymi.app.domain.model.Categoria
import com.raymi.app.domain.model.Item
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.UserPlan
import com.raymi.app.domain.usecase.categoria.GetCategoriasUseCase
import com.raymi.app.domain.usecase.item.GetItemsByWorkspaceOnceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la gestión de ítems optimizado para SaaS.
 * Usa Snapshots para reducir lecturas de Firestore y filtrado local para fluidez.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ItemsViewModel @Inject constructor(
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

    private var allItems = emptyList<Item>()

    init {
        refreshItems()
        loadUserPlan()
        
        // Búsqueda y filtrado local (Reactividad instantánea sin costo adicional)
        _searchQuery.combine(_selectedCategoria) { query, cat ->
            aplicarFiltros(allItems, query, cat)
        }.onEach { filtrados ->
            _uiState.update { it.copy(itemsFiltrados = filtrados) }
        }.launchIn(viewModelScope)
    }

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
            
            // 1. Cargar Categorías (Liviano)
            launch {
                getCategoriasUseCase(workspaceId).collect { res ->
                    _uiState.update { it.copy(categorias = res.data ?: emptyList()) }
                }
            }

            // 2. Cargar Ítems (Snapshot - 1 sola lectura por ítem)
            val res = getItemsByWorkspaceOnceUseCase(workspaceId, _limit.value)
            if (res is Resource.Success) {
                allItems = res.data ?: emptyList()
                _uiState.update { 
                    it.copy(
                        items = allItems,
                        itemsFiltrados = aplicarFiltros(allItems, _searchQuery.value, _selectedCategoria.value),
                        isLoading = false,
                        hasMore = allItems.size >= _limit.value.toInt()
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = res.message) }
            }
        }
    }

    fun debeMostrarAnuncios(plan: UserPlan?): Boolean = adManager.debeMostrarAnuncios(plan)

    fun buscar(query: String) {
        _searchQuery.value = query
    }

    fun filtrarPorCategoria(categoria: Categoria?) {
        _selectedCategoria.value = categoria
    }

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
