package com.raymi.app.presentation.dashboard

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.core.workspace.WorkspaceManager
import com.raymi.app.domain.model.*
import com.raymi.app.domain.repository.UserPlanRepository
import com.raymi.app.domain.usecase.alquiler.GetAlquileresUseCase
import com.raymi.app.domain.usecase.workspace.GetWorkspaceStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

import com.raymi.app.domain.usecase.workspace.UpdateWorkspaceUseCase

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getAlquileresUseCase: GetAlquileresUseCase,
    private val getWorkspaceStatsUseCase: GetWorkspaceStatsUseCase,
    private val generarPdfResumenFinancieroUseCase: com.raymi.app.domain.usecase.pdf.GenerarPdfResumenFinancieroUseCase,
    private val updateWorkspaceUseCase: UpdateWorkspaceUseCase,
    private val userPlanRepository: UserPlanRepository,
    private val workspaceManager: WorkspaceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeWorkspace()
        observeDashboardData()
    }

    private fun observeWorkspace() {
        workspaceManager.currentWorkspace
            .onEach { workspace ->
                _uiState.update { it.copy(currentWorkspace = workspace) }
                if (workspace != null) {
                    cargarPlan(workspace.ownerId)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeDashboardData() {
        workspaceManager.currentWorkspace
            .filterNotNull()
            .flatMapLatest { workspace ->
                combine(
                    getWorkspaceStatsUseCase(workspace.id),
                    getAlquileresUseCase(workspace.id)
                ) { statsResult, alquileresResult ->
                    Pair(statsResult, alquileresResult)
                }
            }
            .onEach { (statsResult, alquileresResult) ->
                handleDashboardResults(statsResult, alquileresResult)
            }
            .catch { e ->
                _uiState.update { it.copy(isLoading = false, error = "Error de sincronización: ${e.message}") }
            }
            .launchIn(viewModelScope)
    }

    private fun handleDashboardResults(
        statsResult: Resource<Map<String, Any>>,
        alquileresResult: Resource<List<Alquiler>>
    ) {
        // Actualizar Estadísticas base desde metadata
        if (statsResult is Resource.Success) {
            val data = statsResult.data ?: emptyMap()
            updateEstadisticas {
                copy(
                    totalClientes = (data["totalClientes"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
                    totalItems = (data["totalItems"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
                    alquileresActivos = (data["alquileresActivos"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
                    ingresosTotales = (data["totalIngresos"] as? Number)?.toDouble()?.coerceAtLeast(0.0) ?: 0.0
                )
            }
        }

        // Actualizar Alquileres y Métricas Calculadas
        when (alquileresResult) {
            is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
            is Resource.Success -> {
                val alquileres = alquileresResult.data ?: emptyList()
                calcularActividadSemanal(alquileres)
                calcularOperacionesHoy(alquileres)
                calcularIngresosMensuales(alquileres)
                _uiState.update { it.copy(isLoading = false) }
            }
            is Resource.Error -> {
                _uiState.update { it.copy(isLoading = false, error = alquileresResult.message) }
            }
        }
    }

    private fun calcularIngresosMensuales(alquileres: List<Alquiler>) {
        val cal = java.util.Calendar.getInstance()
        val mesActual = cal.get(java.util.Calendar.MONTH)
        val anioActual = cal.get(java.util.Calendar.YEAR)
        
        cal.add(java.util.Calendar.MONTH, -1)
        val mesAnterior = cal.get(java.util.Calendar.MONTH)
        val anioAnterior = cal.get(java.util.Calendar.YEAR)

        var totalActual = 0.0
        var totalAnterior = 0.0

        alquileres.forEach { alq ->
            val creationCal = java.util.Calendar.getInstance()
            creationCal.time = alq.createdAt.toDate()
            
            if (creationCal.get(java.util.Calendar.MONTH) == mesActual && creationCal.get(java.util.Calendar.YEAR) == anioActual) {
                totalActual += alq.adelanto
            } else if (creationCal.get(java.util.Calendar.MONTH) == mesAnterior && creationCal.get(java.util.Calendar.YEAR) == anioAnterior) {
                totalAnterior += alq.adelanto
            }
        }

        val variacion = if (totalAnterior > 0) ((totalActual - totalAnterior) / totalAnterior) * 100 else 0.0
        
        updateEstadisticas { copy(ingresosMes = totalActual) }
        _uiState.update { it.copy(variacionMensualPct = variacion) }
    }

    private fun calcularOperacionesHoy(alquileres: List<Alquiler>) {
        val hoy = java.util.Calendar.getInstance()
        val hoyStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(hoy.time)
        
        val entregas = alquileres.count { it.fechaInicioFormatted == hoyStr && it.estado != EstadoAlquiler.CANCELADO }
        val devoluciones = alquileres.count { it.fechaFinFormatted == hoyStr && it.estado == EstadoAlquiler.ACTIVO }
        val pendientes = alquileres.filter { it.saldo > 0.01 && it.estado != EstadoAlquiler.CANCELADO && it.estado != EstadoAlquiler.DEVUELTO }
        val vencidos = alquileres.count { it.estaVencido }

        updateEstadisticas {
            copy(
                entregasHoy = entregas,
                devolucionesHoy = devoluciones,
                pagosPendientesCount = pendientes.size,
                montoPendienteTotal = pendientes.sumOf { it.saldo },
                alquileresVencidos = vencidos
            )
        }
    }

    private fun cargarPlan(ownerId: String) {
        viewModelScope.launch {
            userPlanRepository.getUserPlan(ownerId).collect { result ->
                if (result is Resource.Success) {
                    _uiState.update { it.copy(currentPlan = result.data) }
                }
            }
        }
    }

    private fun calcularActividadSemanal(alquileres: List<Alquiler>) {
        val diasSemana = listOf("Dom", "Lun", "Mar", "Mie", "Jue", "Vie", "Sab")
        val actividad = mutableMapOf("Lun" to 0, "Mar" to 0, "Mie" to 0, "Jue" to 0, "Vie" to 0, "Sab" to 0, "Dom" to 0)
        
        val calendar = java.util.Calendar.getInstance()
        alquileres.forEach { alquiler ->
            calendar.time = alquiler.createdAt.toDate()
            val diaIdx = calendar.get(java.util.Calendar.DAY_OF_WEEK) - 1
            val diaNombre = diasSemana[diaIdx]
            actividad[diaNombre] = (actividad[diaNombre] ?: 0) + 1
        }

        _uiState.update { it.copy(actividadSemanal = actividad) }
    }

    private fun updateEstadisticas(update: Estadisticas.() -> Estadisticas) {
        _uiState.update { it.copy(estadisticas = it.estadisticas.update()) }
    }

    fun exportarResumenFinancieroPdf() {
        val workspace = workspaceManager.currentWorkspace.value ?: return
        val anioActual = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        
        viewModelScope.launch {
            _uiState.update { it.copy(isExportingPdf = true) }
            getAlquileresUseCase(workspace.id).collect { result ->
                if (result is Resource.Success) {
                    val alquileres = result.data ?: emptyList()
                    generarPdfResumenFinancieroUseCase.generarPdf(alquileres, anioActual).collect { pdfResult ->
                        when (pdfResult) {
                            is Resource.Loading -> { }
                            is Resource.Success -> {
                                _uiState.update { it.copy(
                                    isExportingPdf = false,
                                    successMessage = "Resumen financiero generado",
                                    pdfResumenUri = pdfResult.data
                                ) }
                            }
                            is Resource.Error -> {
                                _uiState.update { it.copy(isExportingPdf = false, error = pdfResult.message) }
                            }
                        }
                    }
                } else if (result is Resource.Error) {
                    _uiState.update { it.copy(isExportingPdf = false, error = result.message) }
                }
            }
        }
    }

    fun cambiarIdioma(lang: String) {
        val workspace = _uiState.value.currentWorkspace ?: return
        if (workspace.idioma == lang) return

        val updated = workspace.copy(idioma = lang)
        viewModelScope.launch {
            updateWorkspaceUseCase(updated).collect { result ->
                if (result is Resource.Success) {
                    workspaceManager.setWorkspace(updated)
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }

    data class DashboardUiState(
        val currentWorkspace: Workspace? = null,
        val currentPlan: UserPlan? = null,
        val estadisticas: Estadisticas = Estadisticas(),
        val actividadSemanal: Map<String, Int> = emptyMap(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val variacionMensualPct: Double = 0.0,
        val isExportingPdf: Boolean = false,
        val successMessage: String? = null,
        val pdfResumenUri: Uri? = null
    )
}
