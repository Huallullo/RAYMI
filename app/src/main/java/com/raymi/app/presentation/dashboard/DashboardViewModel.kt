package com.raymi.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.domain.model.Estadisticas
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.usecase.alquiler.GetAlquileresUseCase
import com.raymi.app.domain.usecase.cliente.GetClientesUseCase
import com.raymi.app.domain.usecase.vestuario.GetVestuariosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para el Dashboard
 * Maneja las estadísticas y datos generales de la aplicación
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getClientesUseCase: GetClientesUseCase,
    private val getVestuariosUseCase: GetVestuariosUseCase,
    private val getAlquileresUseCase: GetAlquileresUseCase
) : ViewModel() {

    // ========== ESTADOS UI ==========

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    // ========== ACCIONES ==========

    /**
     * Carga todos los datos del dashboard
     */
    fun loadDashboardData() {
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            // Cargar clientes
            getClientesUseCase().collect { clientesResult ->
                when (clientesResult) {
                    is Resource.Success -> {
                        val totalClientes = clientesResult.data?.size ?: 0
                        updateEstadisticas { copy(totalClientes = totalClientes) }
                    }

                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = clientesResult.message
                        )
                    }

                    is Resource.Loading -> {}
                }
            }
        }

        viewModelScope.launch {
            // Cargar vestuarios
            getVestuariosUseCase().collect { vestuariosResult ->
                when (vestuariosResult) {
                    is Resource.Success -> {
                        val vestuarios = vestuariosResult.data ?: emptyList()
                        val totalVestuarios = vestuarios.size
                        val disponibles = vestuarios.count {
                            it.estado.name == "DISPONIBLE"
                        }
                        updateEstadisticas {
                            copy(
                                totalVestuarios = totalVestuarios,
                                vestuariosDisponibles = disponibles
                            )
                        }
                    }

                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = vestuariosResult.message
                        )
                    }

                    is Resource.Loading -> {}
                }
            }
        }

        viewModelScope.launch {
            getAlquileresUseCase().collect { alquileresResult ->
                when (alquileresResult) {
                    is Resource.Success -> {
                        val alquileres = alquileresResult.data ?: emptyList()
                        val activos = alquileres.count { it.estado.name == "ACTIVO" }
                        val vencidos = alquileres.count { it.estaVencido }

                        // ✅ SOLO SUMAR LO QUE SE HA PAGADO (adelanto)
                        val ingresosTotales = alquileres.sumOf { it.adelanto }

                        // Ingresos del mes actual (solo adelantos)
                        val ingresosMes = alquileres
                            .filter {
                                val mesAlquiler = java.util.Calendar.getInstance().apply {
                                    time = it.createdAt.toDate()
                                }.get(java.util.Calendar.MONTH)
                                val mesActual = java.util.Calendar.getInstance()
                                    .get(java.util.Calendar.MONTH)
                                mesAlquiler == mesActual
                            }
                            .sumOf { it.adelanto }

                        updateEstadisticas {
                            copy(
                                alquileresActivos = activos,
                                alquileresVencidos = vencidos,
                                ingresosMes = ingresosMes,
                                ingresosTotales = ingresosTotales
                            )
                        }

                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }

                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = alquileresResult.message
                        )
                    }

                    is Resource.Loading -> {}
                }
            }
        }
    } // <<--- ESTA LLAVE DE CIERRE ESTABA EN EL LUGAR INCORRECTO

    /**
     * Actualiza las estadísticas
     */
    private fun updateEstadisticas(update: Estadisticas.() -> Estadisticas) {
        _uiState.value = _uiState.value.copy(
            estadisticas = _uiState.value.estadisticas.update()
        )
    }

    /**
     * Limpia el error
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Estado UI para el Dashboard
     */
    data class DashboardUiState(
        val estadisticas: Estadisticas = Estadisticas(),
        val isLoading: Boolean = false,
        val error: String? = null
    )
}
