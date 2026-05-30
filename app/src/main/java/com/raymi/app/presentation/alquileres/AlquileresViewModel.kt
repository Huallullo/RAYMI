package com.raymi.app.presentation.alquileres

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.core.workspace.WorkspaceManager
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.EstadoAlquiler
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.usecase.alquiler.GetAlquileresUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlquileresViewModel @Inject constructor(
    private val getAlquileresUseCase: GetAlquileresUseCase,
    private val getAlquileresOnceUseCase: com.raymi.app.domain.usecase.alquiler.GetAlquileresOnceUseCase,
    private val alquilerRepository: com.raymi.app.domain.repository.AlquilerRepository,
    private val userPlanRepository: com.raymi.app.domain.repository.UserPlanRepository,
    private val auth: com.google.firebase.auth.FirebaseAuth,
    private val adManager: com.raymi.app.core.ads.AdManager,
    private val workspaceManager: WorkspaceManager
) : ViewModel() {

    private var observeJob: Job? = null
    
    private val _uiState = MutableStateFlow(AlquileresUiState())
    val uiState: StateFlow<AlquileresUiState> = _uiState.asStateFlow()

    fun debeMostrarAnuncios(): Boolean = adManager.debeMostrarAnuncios(_uiState.value.userPlan)

    init {
        refreshAlquileres()
        loadUserPlan()
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

    /**
     * Carga los alquileres del negocio actual (Snapshot para ahorro de costos).
     */
    fun refreshAlquileres() {
        viewModelScope.launch {
            try {
                val workspaceId = workspaceManager.getWorkspaceId()
                if (workspaceId == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Negocio no seleccionado") }
                    return@launch
                }
                
                _uiState.update { it.copy(isLoading = true) }
                
                val result = getAlquileresOnceUseCase(workspaceId)
                when (result) {
                    is Resource.Success -> {
                        val data = result.data ?: emptyList()
                        _uiState.update { 
                            it.copy(
                                alquileres = data,
                                filteredAlquileres = filterAlquileres(data, it.searchQuery, it.selectedEstado),
                                isLoading = false 
                            )
                        }
                        verificarVencidos(data)
                    }
                    is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                    else -> {}
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Fallo de conexión") }
            }
        }
    }

    private fun verificarVencidos(alquileres: List<Alquiler>) {
        viewModelScope.launch {
            alquileres.filter { it.estaVencido && it.estado == EstadoAlquiler.ACTIVO }
                .forEach { alquiler ->
                    launch {
                        alquilerRepository.updateEstadoAlquiler(alquiler.id, EstadoAlquiler.VENCIDO).first()
                    }
                }
        }
    }

    fun searchAlquileres(query: String) {
        _uiState.update { 
            it.copy(
                searchQuery = query,
                filteredAlquileres = filterAlquileres(it.alquileres, query, it.selectedEstado)
            )
        }
    }

    fun filterByEstado(estado: EstadoAlquiler?) {
        _uiState.update { 
            it.copy(
                selectedEstado = estado,
                filteredAlquileres = filterAlquileres(it.alquileres, it.searchQuery, estado)
            )
        }
    }

    private fun filterAlquileres(
        alquileres: List<Alquiler>, 
        query: String, 
        estado: EstadoAlquiler?
    ): List<Alquiler> {
        return alquileres.filter { alquiler ->
            val matchQuery = query.isBlank() || 
                alquiler.clienteNombre.contains(query, ignoreCase = true) ||
                alquiler.itemNombre.contains(query, ignoreCase = true) ||
                alquiler.itemCodigo.contains(query, ignoreCase = true)
            
            val matchEstado = estado == null || 
                (estado == EstadoAlquiler.VENCIDO && alquiler.estaVencido) ||
                (estado != EstadoAlquiler.VENCIDO && alquiler.estado == estado)
                
            matchQuery && matchEstado
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        observeJob?.cancel()
    }
}

data class AlquileresUiState(
    val alquileres: List<Alquiler> = emptyList(),
    val filteredAlquileres: List<Alquiler> = emptyList(),
    val searchQuery: String = "",
    val selectedEstado: EstadoAlquiler? = null,
    val userPlan: com.raymi.app.domain.model.UserPlan? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
