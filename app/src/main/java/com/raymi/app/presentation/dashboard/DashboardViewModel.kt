package com.raymi.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.Estadisticas
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.usecase.alquiler.GetAlquileresUseCase
import com.raymi.app.domain.usecase.cliente.GetClientesUseCase
import com.raymi.app.domain.usecase.vestuario.GetVestuariosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getClientesUseCase: GetClientesUseCase,
    private val getVestuariosUseCase: GetVestuariosUseCase,
    private val getAlquileresUseCase: GetAlquileresUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var dashboardJob: Job? = null
    private var latestAlquileres: List<Alquiler> = emptyList()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        dashboardJob?.cancel()
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        dashboardJob = viewModelScope.launch {
            // Clientes
            launch {
                getClientesUseCase().collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            val totalClientes = result.data?.size ?: 0
                            updateEstadisticas { copy(totalClientes = totalClientes) }
                        }
                        is Resource.Error -> {
                            _uiState.value = _uiState.value.copy(error = result.message)
                        }
                        is Resource.Loading -> {}
                    }
                }
            }

            // Vestuarios
            launch {
                getVestuariosUseCase().collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            val vestuarios = result.data ?: emptyList()
                            val totalVestuarios = vestuarios.size
                            val disponibles = vestuarios.count { it.estado.name == "DISPONIBLE" }

                            updateEstadisticas {
                                copy(
                                    totalVestuarios = totalVestuarios,
                                    vestuariosDisponibles = disponibles
                                )
                            }
                        }
                        is Resource.Error -> {
                            _uiState.value = _uiState.value.copy(error = result.message)
                        }
                        is Resource.Loading -> {}
                    }
                }
            }

            // Alquileres
            launch {
                getAlquileresUseCase().collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            val alquileres = result.data ?: emptyList()
                            latestAlquileres = alquileres

                            val activos = alquileres.count { it.estado.name == "ACTIVO" }
                            val vencidos = alquileres.count { it.estaVencido }

                            updateEstadisticas {
                                copy(
                                    alquileresActivos = activos,
                                    alquileresVencidos = vencidos
                                )
                            }

                            recalculateIngresos()
                            _uiState.value = _uiState.value.copy(isLoading = false)
                        }
                        is Resource.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                        is Resource.Loading -> {}
                    }
                }
            }
        }
    }

    fun onMonthSelected(month: Int) {
        _uiState.value = _uiState.value.copy(selectedMonth = month)
        recalculateIngresos()
    }

    fun onYearSelected(year: Int) {
        _uiState.value = _uiState.value.copy(selectedYear = year)
        recalculateIngresos()
    }

    private fun recalculateIngresos() {
        val selectedMonth = _uiState.value.selectedMonth
        val selectedYear = _uiState.value.selectedYear

        val ingresosMes = ingresoPeriodo(latestAlquileres, selectedMonth, selectedYear)
        val ingresosTotales = latestAlquileres.sumOf { it.adelanto }

        val (prevMonth, prevYear) = previousMonthYear(selectedMonth, selectedYear)
        val ingresosMesAnterior = ingresoPeriodo(latestAlquileres, prevMonth, prevYear)

        val variacionPct = if (ingresosMesAnterior > 0.0) {
            ((ingresosMes - ingresosMesAnterior) / ingresosMesAnterior) * 100.0
        } else {
            if (ingresosMes > 0.0) 100.0 else 0.0
        }

        updateEstadisticas {
            copy(
                ingresosMes = ingresosMes,
                ingresosTotales = ingresosTotales
            )
        }

        _uiState.value = _uiState.value.copy(
            ingresoMesAnterior = ingresosMesAnterior,
            variacionMensualPct = variacionPct
        )
    }

    private fun ingresoPeriodo(
        alquileres: List<Alquiler>,
        month: Int,
        year: Int
    ): Double {
        return alquileres
            .filter { alquiler ->
                val cal = Calendar.getInstance().apply { time = alquiler.createdAt.toDate() }
                cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year
            }
            .sumOf { it.adelanto }
    }

    private fun previousMonthYear(month: Int, year: Int): Pair<Int, Int> {
        return if (month == 0) 11 to (year - 1) else (month - 1) to year
    }

    private fun updateEstadisticas(update: Estadisticas.() -> Estadisticas) {
        _uiState.value = _uiState.value.copy(
            estadisticas = _uiState.value.estadisticas.update()
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        dashboardJob?.cancel()
        super.onCleared()
    }

    data class DashboardUiState(
        val estadisticas: Estadisticas = Estadisticas(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
        val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
        val ingresoMesAnterior: Double = 0.0,
        val variacionMensualPct: Double = 0.0
    )
}