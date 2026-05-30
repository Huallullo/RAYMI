package com.raymi.app.presentation.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.core.workspace.WorkspaceManager
import com.raymi.app.domain.model.Categoria
import com.raymi.app.domain.model.Item
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.UserPlan
import com.raymi.app.domain.usecase.categoria.GetCategoriasUseCase
import com.raymi.app.domain.usecase.item.GetItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * ViewModel para la gestión de ítems con flujo reactivo optimizado.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ItemsViewModel @Inject constructor(
    private val getItemsUseCase: GetItemsUseCase,
    private val getCategoriasUseCase: GetCategoriasUseCase,
    private val userPlanRepository: com.raymi.app.domain.repository.UserPlanRepository,
    private val auth: com.google.firebase.auth.FirebaseAuth,
    private val adManager: com.raymi.app.core.ads.AdManager,
    workspaceManager: WorkspaceManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategoria = MutableStateFlow<Categoria?>(null)
    private val _limit = MutableStateFlow(100L)

    fun debeMostrarAnuncios(plan: UserPlan?): Boolean = adManager.debeMostrarAnuncios(plan)

    val uiState: StateFlow<ItemsUiState> = workspaceManager.currentWorkspace
        .filterNotNull()
        .flatMapLatest { workspace ->
            val userPlanFlow = auth.uid?.let { userPlanRepository.getUserPlan(it) } ?: flowOf(Resource.Success(null))
            
            combine(
                _limit.flatMapLatest { getItemsUseCase(workspace.id, limit = it) },
                getCategoriasUseCase(workspace.id),
                userPlanFlow,
                _searchQuery,
                _selectedCategoria,
                _limit
            ) { args: Array<Any?> ->
                val itemsRes = args[0] as Resource<List<Item>>
                val catsRes = args[1] as Resource<List<Categoria>>
                val planRes = args[2] as Resource<UserPlan?>
                val query = args[3] as String
                val cat = args[4] as Categoria?
                val currentLimit = args[5] as Long

                ItemsUiState(
                    items = itemsRes.data ?: emptyList(),
                    categorias = catsRes.data ?: emptyList(),
                    itemsFiltrados = aplicarFiltros(itemsRes.data ?: emptyList(), query, cat),
                    queryBusqueda = query,
                    categoriaFiltro = cat,
                    userPlan = planRes.data,
                    hasMore = (itemsRes.data?.size ?: 0) >= currentLimit.toInt(),
                    isLoading = itemsRes is Resource.Loading || catsRes is Resource.Loading,
                    error = itemsRes.message ?: catsRes.message
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ItemsUiState(isLoading = true))

    fun buscar(query: String) {
        _searchQuery.value = query
    }

    fun filtrarPorCategoria(categoria: Categoria?) {
        _selectedCategoria.value = categoria
    }

    fun cargarMas() {
        _limit.value += 100
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

    fun limpiarError() { }
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
