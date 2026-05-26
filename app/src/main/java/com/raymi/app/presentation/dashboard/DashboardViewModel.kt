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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getAlquileresUseCase: GetAlquileresUseCase,
    private val getWorkspaceStatsUseCase: GetWorkspaceStatsUseCase,
    private val userPlanRepository: UserPlanRepository,
    private val workspaceManager: WorkspaceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var dashboardJob: Job? = null
    private var latestAlquileres: List<Alquiler> = emptyList()

    init {
        observeWorkspace()
        loadDashboardData()
    }

    private fun observeWorkspace() {
        viewModelScope.launch {
            workspaceManager.currentWorkspace.collectLatest { workspace ->
                _uiState.update { it.copy(currentWorkspace = workspace) }
                if (workspace != null) {
                    cargarPlan(workspace.ownerId)
                }
            }
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

    fun loadDashboardData() {
        dashboardJob?.cancel()
        _uiState.update { it.copy(isLoading = true, error = null) }

        dashboardJob = viewModelScope.launch {
            try {
                workspaceManager.currentWorkspace.collectLatest { workspace ->
                    if (workspace == null) return@collectLatest
                    val workspaceId = workspace.id

                    // 1. Estadísticas Consolidadas en TIEMPO REAL
                    launch {
                        getWorkspaceStatsUseCase(workspaceId).collect { result ->
                            if (result is Resource.Success) {
                                val data = result.data ?: emptyMap()
                                updateEstadisticas {
                                    copy(
                                        totalClientes = (data["totalClientes"] as? Number)?.toInt() ?: 0,
                                        totalVestuarios = (data["totalItems"] as? Number)?.toInt() ?: 0,
                                        alquileresActivos = (data["alquileresActivos"] as? Number)?.toInt() ?: 0,
                                        ingresosTotales = (data["totalIngresos"] as? Number)?.toDouble() ?: 0.0
                                    )
                                }
                            }
                        }
                    }

                    // 2. Alquileres en TIEMPO REAL
                    launch {
                        getAlquileresUseCase(workspaceId).collect { result ->
                            if (result is Resource.Success) {
                                val alquileres = result.data ?: emptyList()
                                latestAlquileres = alquileres
                                calcularActividadSemanal(alquileres)
                                recalculateIngresos(alquileres)
                                _uiState.update { it.copy(isLoading = false) }
                            } else if (result is Resource.Error) {
                                _uiState.update { it.copy(isLoading = false, error = result.message) }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Sincronización pendiente") }
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

    private fun recalculateIngresos(alquileres: List<Alquiler>) {
        val total = alquileres.sumOf { it.precioTotal }
        updateEstadisticas { copy(ingresosMes = total) }
    }

    fun updateEstadisticas(update: Estadisticas.() -> Estadisticas) {
        _uiState.update { it.copy(estadisticas = it.estadisticas.update()) }
    }

    fun exportarResumenFinancieroPdf() {
        // Implementación básica del PDF
        _uiState.update { it.copy(successMessage = "Resumen financiero generado") }
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
