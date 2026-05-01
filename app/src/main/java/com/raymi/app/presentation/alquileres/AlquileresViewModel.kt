package com.raymi.app.presentation.alquileres

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.EstadoAlquiler
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.usecase.alquiler.GetAlquileresUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la gestión de alquileres
 * Maneja la lista de alquileres y filtros
 */
@HiltViewModel
class AlquileresViewModel @Inject constructor(
    private val getAlquileresUseCase: GetAlquileresUseCase,

) : ViewModel() {
    companion object {
        private const val ALQUILERES_PAGE_SIZE = 50
    }
    private var observeJob: Job? = null
    // ========== ESTADOS UI ==========

    private val _uiState = MutableStateFlow(AlquileresUiState())
    val uiState: StateFlow<AlquileresUiState> = _uiState.asStateFlow()

    init {
        loadAlquileres()
    }

    // ========== ACCIONES ==========

    /**
     * Carga la lista de alquileres
     */
    fun loadAlquileres() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            getAlquileresUseCase().collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }

                    is Resource.Success -> {
                        val alquileres = result.data ?: emptyList()
                        val filtered = filterAlquileres(alquileres)
                        _uiState.value = _uiState.value.copy(
                            alquileres = alquileres,
                            filteredAlquileres = filtered,
                            visibleAlquileres = filtered.take(_uiState.value.visibleLimit),
                            hasMoreAlquileres = filtered.size > _uiState.value.visibleLimit,
                            isLoading = false,
                            error = null
                        )
                    }

                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Busca alquileres por texto
     */
    fun searchAlquileres(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        _uiState.value = _uiState.value.copy(visibleLimit = ALQUILERES_PAGE_SIZE)
        _uiState.value = _uiState.value.copy(
            filteredAlquileres = filterAlquileres(_uiState.value.alquileres)
        )
        val filtered = _uiState.value.filteredAlquileres
        _uiState.value = _uiState.value.copy(
            visibleAlquileres = filtered.take(_uiState.value.visibleLimit),
            hasMoreAlquileres = filtered.size > _uiState.value.visibleLimit
        )
    }

    /**
     * Filtra por estado
     */
    fun filterByEstado(estado: EstadoAlquiler?) {
        _uiState.value = _uiState.value.copy(selectedEstado = estado)
        _uiState.value = _uiState.value.copy(visibleLimit = ALQUILERES_PAGE_SIZE)
        _uiState.value = _uiState.value.copy(
            filteredAlquileres = filterAlquileres(_uiState.value.alquileres)
        )
        val filtered = _uiState.value.filteredAlquileres
        _uiState.value = _uiState.value.copy(
            visibleAlquileres = filtered.take(_uiState.value.visibleLimit),
            hasMoreAlquileres = filtered.size > _uiState.value.visibleLimit
        )
    }

    fun loadMoreAlquileres() {
        val nextLimit = _uiState.value.visibleLimit + ALQUILERES_PAGE_SIZE
        val filtered = _uiState.value.filteredAlquileres
        _uiState.value = _uiState.value.copy(
            visibleLimit = nextLimit,
            visibleAlquileres = filtered.take(nextLimit),
            hasMoreAlquileres = filtered.size > nextLimit
        )
    }

    /**
     * Aplica los filtros actuales
     */
    private fun filterAlquileres(alquileres: List<Alquiler>): List<Alquiler> {
        var filtered = alquileres

        // Filtrar por búsqueda
        if (_uiState.value.searchQuery.isNotBlank()) {
            val query = _uiState.value.searchQuery
            filtered = filtered.filter { alquiler ->
                alquiler.clienteNombre.contains(query, ignoreCase = true) ||
                        alquiler.vestuarioNombre.contains(query, ignoreCase = true) ||
                        alquiler.vestuarioCodigo.contains(query, ignoreCase = true)
            }
        }

        // Filtrar por estado
        _uiState.value.selectedEstado?.let { estado ->
            filtered = filtered.filter { alquiler ->
                when (estado) {
                    EstadoAlquiler.VENCIDO -> {
                        alquiler.estado == EstadoAlquiler.VENCIDO ||
                                (alquiler.estado == EstadoAlquiler.ACTIVO && alquiler.estaVencido)
                    }
                    EstadoAlquiler.ACTIVO -> {
                        alquiler.estado == EstadoAlquiler.ACTIVO
                    }
                    EstadoAlquiler.DEVUELTO -> alquiler.estado == EstadoAlquiler.DEVUELTO
                    EstadoAlquiler.CANCELADO -> alquiler.estado == EstadoAlquiler.CANCELADO
                }
            }
        }

        return filtered
    }

    /**
     * Limpia los mensajes
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            error = null,
            successMessage = null
        )
    }
}

/**
 * Estado UI para la pantalla de alquileres
 */
data class AlquileresUiState(
    val alquileres: List<Alquiler> = emptyList(),
    val filteredAlquileres: List<Alquiler> = emptyList(),
    val searchQuery: String = "",
    val selectedEstado: EstadoAlquiler? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val visibleAlquileres: List<Alquiler> = emptyList(),
    val visibleLimit: Int = 50,
    val hasMoreAlquileres: Boolean = false,
)
