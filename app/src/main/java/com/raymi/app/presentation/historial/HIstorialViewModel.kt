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
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class HistorialViewModel @Inject constructor(
    private val alquilerRepository: AlquilerRepository,
    private val itemRepository: com.raymi.app.domain.repository.ItemRepository,
    private val generarPdfInventarioUseCase: com.raymi.app.domain.usecase.pdf.GenerarPdfInventarioUseCase,
    private val exportService: com.raymi.app.data.remote.ExportService,
    private val sharePdfUseCase: com.raymi.app.domain.usecase.pdf.SharePdfUseCase,
    private val workspaceManager: WorkspaceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistorialUiState())
    val uiState: StateFlow<HistorialUiState> = _uiState.asStateFlow()

    private var lastSnapshot: Any? = null
    private val PAGE_SIZE = 50L

    // Cache en memoria para esta sesión — historial no cambia mientras usas la app
    private val historialCache = SmartCache<List<Alquiler>>()

    init {
        cargarHistorial()
    }

    fun cargarHistorial(refresh: Boolean = false) {
        if (refresh) {
            lastSnapshot = null
            _uiState.update { it.copy(allAlquileres = emptyList()) }
        }

        viewModelScope.launch {
            // Si es la primera carga y hay cache, úsalo (Solo si no es refresh forzado)
            if (lastSnapshot == null && !refresh) {
                val cached = historialCache.get()
                if (cached != null) {
                    aplicarFiltro(cached, _uiState.value.query)
                    return@launch
                }
            }

            val workspaceId = workspaceManager.getWorkspaceId() ?: return@launch
            _uiState.update { it.copy(isLoading = true) }

            try {
                // ✅ FEATURE 1 FIX: Paginación real en historial
                val result = alquilerRepository.getAlquileresCerrados(
                    workspaceId = workspaceId,
                    limit = PAGE_SIZE,
                    lastSnapshot = lastSnapshot
                )
                
                if (result is Resource.Success) {
                    val newItems = result.data ?: emptyList()
                    val totalList = if (lastSnapshot == null) newItems else _uiState.value.allAlquileres + newItems
                    lastSnapshot = result.cursor
                    
                    if (lastSnapshot == null && !refresh) {
                        historialCache.set(totalList, ttlMs = 60 * 60 * 1000)
                    }
                    
                    _uiState.update { it.copy(hasMore = newItems.size >= PAGE_SIZE) }
                    aplicarFiltro(totalList, _uiState.value.query)
                } else if (result is Resource.Error) {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Error al cargar historial: ${e.localizedMessage}") }
            } finally {
                delay(500)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun cargarMas() {
        if (!_uiState.value.isLoading && _uiState.value.hasMore) {
            cargarHistorial()
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
                // ✅ FEATURE 2 FIX: Sumar lo realmente pagado
                totalRecaudado = filtrados.sumOf { a -> a.precioTotal - a.saldo },
                totalTransacciones = filtrados.size,
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

    fun exportarCSV() {
        val alquileres = _uiState.value.filteredAlquileres
        if (alquileres.isEmpty()) {
            _uiState.update { it.copy(error = "No hay datos para exportar") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val uri = exportService.generarCsvAlquileres(alquileres)
            if (uri != null) {
                _uiState.update { it.copy(isLoading = false, successMessage = "CSV generado") }
                sharePdfUseCase(uri)
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Error al generar archivo") }
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
    val totalTransacciones: Int = 0,
    val hasMore: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
