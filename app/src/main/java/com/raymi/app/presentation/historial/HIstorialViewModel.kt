package com.raymi.app.presentation.historial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.core.cache.SmartCache
import com.raymi.app.core.workspace.WorkspaceManager
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.EstadoAlquiler
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.AlquilerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistorialViewModel @Inject constructor(
    private val alquilerRepository: AlquilerRepository,
    private val itemRepository: com.raymi.app.domain.repository.ItemRepository,
    private val generarPdfInventarioUseCase: com.raymi.app.domain.usecase.pdf.GenerarPdfInventarioUseCase,
    private val workspaceManager: WorkspaceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistorialUiState())
    val uiState: StateFlow<HistorialUiState> = _uiState.asStateFlow()

    // Cache en memoria para esta sesión — historial no cambia mientras usas la app
    private val historialCache = SmartCache<List<Alquiler>>()

    init {
        cargarHistorial()
    }

    fun cargarHistorial() {
        viewModelScope.launch {
            // Si el caché es válido, úsalo sin tocar Firestore
            val cached = historialCache.get()
            if (cached != null) {
                aplicarFiltro(cached, _uiState.value.query)
                return@launch
            }

            val workspaceId = workspaceManager.getWorkspaceId() ?: return@launch
            _uiState.update { it.copy(isLoading = true) }

            try {
                // get() puntual — NO listener
                alquilerRepository.getAlquileresByEstado(workspaceId, EstadoAlquiler.DEVUELTO)
                    .filter { it !is Resource.Loading }
                    .first()
                    .let { result ->
                        if (result is Resource.Success) {
                            val devueltos = result.data ?: emptyList()
                            // También busca cancelados
                            alquilerRepository.getAlquileresByEstado(workspaceId, EstadoAlquiler.CANCELADO)
                                .filter { it !is Resource.Loading }
                                .first()
                                .let { cancelResult ->
                                    val cancelados = (cancelResult as? Resource.Success)?.data ?: emptyList()
                                    val todos = (devueltos + cancelados).sortedByDescending { it.updatedAt }
                                    historialCache.set(todos, ttlMs = 10 * 60 * 1000) // 10 min
                                    aplicarFiltro(todos, _uiState.value.query)
                                }
                        } else {
                            _uiState.update { it.copy(isLoading = false, error = result.message) }
                        }
                    }
            } catch (e: Exception) {
                android.util.Log.e("Historial", "Error: ${e.message}")
                _uiState.update { it.copy(isLoading = false, error = "Error al cargar historial: ${e.message}") }
            }
        }
    }

    private fun aplicarFiltro(todos: List<Alquiler>, query: String) {
        val filtrados = if (query.isBlank()) todos else {
            todos.filter {
                it.clienteNombre.contains(query, ignoreCase = true) ||
                it.itemNombre.contains(query, ignoreCase = true)
            }
        }
        _uiState.update {
            it.copy(
                allAlquileres = todos,
                filteredAlquileres = filtrados,
                totalRecaudado = todos.sumOf { a -> a.adelanto },
                isLoading = false
            )
        }
    }

    fun filtrar(query: String) {
        _uiState.update { it.copy(query = query) }
        aplicarFiltro(_uiState.value.allAlquileres, query)
    }

    fun exportarInventario() {
        val workspace = workspaceManager.currentWorkspace.value ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            itemRepository.getItemsByWorkspace(workspace.id).collect { result ->
                if (result is Resource.Success) {
                    val items = result.data ?: emptyList()
                    generarPdfInventarioUseCase.generarPdf(items, workspace.nombre).collect { pdfResult ->
                        if (pdfResult is Resource.Success) {
                            _uiState.update { it.copy(isLoading = false, successMessage = "Inventario exportado") }
                        } else if (pdfResult is Resource.Error) {
                            _uiState.update { it.copy(isLoading = false, error = pdfResult.message) }
                        }
                    }
                } else if (result is Resource.Error) {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun clearMessages() = _uiState.update { it.copy(error = null, successMessage = null) }
}

data class HistorialUiState(
    val allAlquileres: List<Alquiler> = emptyList(),
    val filteredAlquileres: List<Alquiler> = emptyList(),
    val query: String = "",
    val totalRecaudado: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
