package com.raymi.app.presentation.dashboard

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.Estadisticas
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.usecase.alquiler.GetAlquileresUseCase
import com.raymi.app.domain.usecase.cliente.GetClientesUseCase
import com.raymi.app.domain.usecase.notifications.EnviarMensajeUseCase
import com.raymi.app.domain.usecase.pdf.GenerarPdfResumenFinancieroUseCase
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
    private val getAlquileresUseCase: GetAlquileresUseCase,
    private val generarPdfResumenFinancieroUseCase: GenerarPdfResumenFinancieroUseCase,
    private val enviarMensajeUseCase: EnviarMensajeUseCase
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
            launch {
                getClientesUseCase().collect { result ->
                    if (result is Resource.Success) {
                        updateEstadisticas { copy(totalClientes = result.data?.size ?: 0) }
                    } else if (result is Resource.Error) {
                        _uiState.value = _uiState.value.copy(error = result.message)
                    }
                }
            }

            launch {
                getVestuariosUseCase().collect { result ->
                    if (result is Resource.Success) {
                        val vestuarios = result.data ?: emptyList()
                        updateEstadisticas {
                            copy(
                                totalVestuarios      = vestuarios.size,
                                vestuariosDisponibles = vestuarios.count { it.estado.name == "DISPONIBLE" }
                            )
                        }
                    } else if (result is Resource.Error) {
                        _uiState.value = _uiState.value.copy(error = result.message)
                    }
                }
            }

            launch {
                getAlquileresUseCase().collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            val alquileres = result.data ?: emptyList()
                            latestAlquileres = alquileres

                            updateEstadisticas {
                                copy(
                                    alquileresActivos  = alquileres.count { it.estado.name == "ACTIVO" },
                                    alquileresVencidos = alquileres.count { it.estaVencido }
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
        val selectedYear  = _uiState.value.selectedYear

        // ✅ FIX: se usa precioTotal (ingreso real del período), no adelanto
        val ingresosMes     = ingresoPeriodo(latestAlquileres, selectedMonth, selectedYear)
        // Acumulado total: suma de todos los precios cobrados históricamente
        val ingresosTotales = latestAlquileres.sumOf { it.precioTotal }

        val (prevMonth, prevYear) = previousMonthYear(selectedMonth, selectedYear)
        val ingresosMesAnterior   = ingresoPeriodo(latestAlquileres, prevMonth, prevYear)

        val variacionPct = when {
            ingresosMesAnterior > 0.0 ->
                ((ingresosMes - ingresosMesAnterior) / ingresosMesAnterior) * 100.0
            ingresosMes > 0.0 -> 100.0
            else -> 0.0
        }

        updateEstadisticas {
            copy(
                ingresosMes     = ingresosMes,
                ingresosTotales = ingresosTotales
            )
        }

        _uiState.value = _uiState.value.copy(
            ingresoMesAnterior   = ingresosMesAnterior,
            variacionMensualPct  = variacionPct
        )
    }

    /**
     * Suma el precioTotal de los alquileres creados en el mes/año indicado.
     * Usar precioTotal (precio acordado) es más representativo del ingreso del período.
     * Si se prefiere "dinero recibido", usar adelanto en su lugar.
     */
    private fun ingresoPeriodo(alquileres: List<Alquiler>, month: Int, year: Int): Double {
        return alquileres
            .filter { alquiler ->
                val cal = Calendar.getInstance().apply { time = alquiler.createdAt.toDate() }
                cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year
            }
            .sumOf { it.precioTotal }   // ✅ era it.adelanto (incorrecto)
    }

    private fun previousMonthYear(month: Int, year: Int): Pair<Int, Int> =
        if (month == 0) 11 to (year - 1) else (month - 1) to year

    private fun updateEstadisticas(update: Estadisticas.() -> Estadisticas) {
        _uiState.value = _uiState.value.copy(
            estadisticas = _uiState.value.estadisticas.update()
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    fun exportarResumenFinancieroPdf() {
        viewModelScope.launch {
            generarPdfResumenFinancieroUseCase
                .generarPdf(latestAlquileres, _uiState.value.selectedYear)
                .collect { result ->
                    when (result) {
                        is Resource.Loading -> _uiState.value = _uiState.value.copy(isExportingPdf = true)
                        is Resource.Success -> _uiState.value = _uiState.value.copy(
                            isExportingPdf = false,
                            successMessage = "PDF generado en Descargas",
                            pdfResumenUri = result.data
                        )
                        is Resource.Error -> _uiState.value = _uiState.value.copy(
                            isExportingPdf = false,
                            error = result.message
                        )
                    }
                }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }

    fun compartirResumenFinancieroPorWhatsApp() {
        val pdfUri = _uiState.value.pdfResumenUri ?: run {
            _uiState.value = _uiState.value.copy(error = "Primero genera el PDF del resumen")
            return
        }

        viewModelScope.launch {
            enviarMensajeUseCase.compartirPdfPorWhatsApp(
                pdfUri = pdfUri,
                mensaje = "Resumen financiero ${_uiState.value.selectedYear} - RAYMI"
            ).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.value = _uiState.value.copy(isExportingPdf = true)
                    is Resource.Success -> _uiState.value = _uiState.value.copy(
                        isExportingPdf = false,
                        successMessage = result.data ?: "Compartido por WhatsApp"
                    )
                    is Resource.Error -> _uiState.value = _uiState.value.copy(
                        isExportingPdf = false,
                        error = result.message
                    )
                }
            }
        }
    }
    override fun onCleared() {
        dashboardJob?.cancel()
        super.onCleared()
    }

    data class DashboardUiState(
        val estadisticas        : Estadisticas = Estadisticas(),
        val isLoading           : Boolean      = false,
        val error               : String?      = null,
        val selectedMonth       : Int          = Calendar.getInstance().get(Calendar.MONTH),
        val selectedYear        : Int          = Calendar.getInstance().get(Calendar.YEAR),
        val ingresoMesAnterior  : Double       = 0.0,
        val variacionMensualPct : Double       = 0.0,
        val isExportingPdf      : Boolean      = false,
        val successMessage      : String?      = null,
        val pdfResumenUri       : Uri?         = null
    )
}
