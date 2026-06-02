package com.raymi.app.presentation.dashboard

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.core.workspace.WorkspaceManager
import com.raymi.app.domain.model.*
import com.raymi.app.domain.repository.UserPlanRepository
import com.raymi.app.domain.usecase.workspace.UpdateWorkspaceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val generarPdfResumenFinancieroUseCase: com.raymi.app.domain.usecase.pdf.GenerarPdfResumenFinancieroUseCase,
    private val getWorkspaceStatsUseCase: com.raymi.app.domain.usecase.workspace.GetWorkspaceStatsUseCase, // OPTIMIZACIÓN: Inyectar caso de uso de stats
    private val updateWorkspaceUseCase: UpdateWorkspaceUseCase,
    private val userPlanRepository: UserPlanRepository,
    private val alquilerRepository: com.raymi.app.domain.repository.AlquilerRepository,
    private val itemRepository: com.raymi.app.domain.repository.ItemRepository,
    private val clienteRepository: com.raymi.app.domain.repository.ClienteRepository,
    private val adManager: com.raymi.app.core.ads.AdManager,
    private val workspaceManager: WorkspaceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun debeMostrarAnuncios(): Boolean = adManager.debeMostrarAnuncios(_uiState.value.currentPlan)

    init {
        observeWorkspace()
        viewModelScope.launch {
            workspaceManager.currentWorkspace.filterNotNull().first().let {
                refreshData()
            }
        }
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

    /**
     * Carga rápida de estadísticas desde el documento metadata/stats.
     * OPTIMIZACIÓN: Reduce de ~200 lecturas a solo 1 lectura Firestore por refresh.
     */
    fun refreshData(forceRefresh: Boolean = false) {
        if (forceRefresh) {
            performFullAudit()
        } else {
            val workspaceId = workspaceManager.getWorkspaceId() ?: return
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                getWorkspaceStatsUseCase(workspaceId, false).collect { result ->
                    if (result is Resource.Success) {
                        handleStatsResult(result.data ?: emptyMap())
                        _uiState.update { it.copy(isLoading = false) }
                    } else if (result is Resource.Error) {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }

    /**
     * AUDITORÍA PROFUNDA: Recalcula todas las estadísticas leyendo todas las colecciones.
     * Solo debe llamarse cuando el usuario pide explícitamente un "Refresco Total".
     */
    fun performFullAudit() {
        val workspaceId = workspaceManager.getWorkspaceId() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // 1. Invalidar caches
            itemRepository.invalidateCache(workspaceId)
            
            // 2. Fetch Alquileres y Pagos (Heavy)
            val resAlq = alquilerRepository.getAlquileresOnce(workspaceId)
            val alquileres = (resAlq as? Resource.Success)?.data ?: emptyList()
            val ids = alquileres.map { it.id }
            
            val pagosResult = alquilerRepository.getPagosDeAlquileres(workspaceId, ids)
            val allPagos = (pagosResult as? Resource.Success)?.data ?: emptyList()

            // 3. Fetch Items reales
            val resItems = itemRepository.getItemsByWorkspaceOnce(workspaceId, 500)
            val items = (resItems as? Resource.Success)?.data ?: emptyList()
            
            // 4. Fetch Clientes reales
            val resClientes = clienteRepository.getClientesOnce(workspaceId)
            val clientes = (resClientes as? Resource.Success)?.data ?: emptyList()

            val cal = Calendar.getInstance()
            val anio = cal.get(Calendar.YEAR)
            val mes = cal.get(Calendar.MONTH)
            val diaAnio = cal.get(Calendar.DAY_OF_YEAR)
            
            // AUDITORÍA FINANCIERA
            val ingresosEsteMes = allPagos.filter {
                val c = Calendar.getInstance().apply { time = it.fecha.toDate() }
                c.get(Calendar.YEAR) == anio && c.get(Calendar.MONTH) == mes
            }.sumOf { it.monto }

            val totalRecaudado = allPagos.sumOf { it.monto }
            val saldoPendienteTotal = alquileres.filter { it.estado != EstadoAlquiler.DEVUELTO && it.estado != EstadoAlquiler.CANCELADO }
                .sumOf { it.saldoPendienteReal }

            // AUDITORÍA OPERATIVA
            val entregasHoyCount = alquileres.count { 
                val c = Calendar.getInstance().apply { time = it.fechaInicio.toDate() }
                c.get(Calendar.YEAR) == anio && c.get(Calendar.DAY_OF_YEAR) == diaAnio 
            }
            val retornosHoyCount = alquileres.count {
                val c = Calendar.getInstance().apply { time = it.fechaFinPrevista.toDate() }
                c.get(Calendar.YEAR) == anio && c.get(Calendar.DAY_OF_YEAR) == diaAnio
            }

            val repairData = mapOf(
                "totalIngresos" to totalRecaudado,
                "totalSaldoPendiente" to saldoPendienteTotal,
                "totalClientes" to clientes.size.toLong(),
                "totalItems" to items.size.toLong(),
                "alquileresActivos" to alquileres.count { it.estado == EstadoAlquiler.ACTIVO || it.estado == EstadoAlquiler.VENCIDO }.toLong(),
                "ingresos_${anio}_$mes" to ingresosEsteMes,
                "operaciones_${anio}_$diaAnio" to mapOf("entregas" to entregasHoyCount, "devoluciones" to retornosHoyCount)
            )
            
            updateWorkspaceUseCase.updateStats(workspaceId, repairData)
            handleStatsResult(repairData)
            _uiState.update { it.copy(isLoading = false, successMessage = "Estadísticas sincronizadas") }
        }
    }

    private fun handleStatsResult(data: Map<String, Any>) {
        val cal = Calendar.getInstance()
        val anio = cal.get(Calendar.YEAR)
        val mes = cal.get(Calendar.MONTH)
        val diaAnio = cal.get(Calendar.DAY_OF_YEAR)

        val mesKey = "ingresos_${anio}_$mes"
        cal.add(Calendar.MONTH, -1)
        val mesAntKey = "ingresos_${cal.get(Calendar.YEAR)}_${cal.get(Calendar.MONTH)}"
        
        val hoyKey = "operaciones_${anio}_$diaAnio"
        @Suppress("UNCHECKED_CAST")
        val hoyData = data[hoyKey] as? Map<String, Any> ?: emptyMap()

        val ingresosActual = (data[mesKey] as? Number)?.toDouble() ?: 0.0
        val ingresosAnterior = (data[mesAntKey] as? Number)?.toDouble() ?: 0.0
        val variacion = if (ingresosAnterior > 0) ((ingresosActual - ingresosAnterior) / ingresosAnterior) * 100 else 0.0

        updateEstadisticas {
            copy(
                totalClientes = (data["totalClientes"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
                totalItems = (data["totalItems"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
                alquileresActivos = (data["alquileresActivos"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
                alquileresVencidos = (data["alquileresVencidos"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
                itemsDisponibles = (data["totalItems"] as? Number)?.toInt()?.minus((data["alquiladosActuales"] as? Number)?.toInt() ?: 0)?.coerceAtLeast(0) ?: 0,
                ingresosTotales = (data["totalIngresos"] as? Number)?.toDouble()?.coerceAtLeast(0.0) ?: 0.0,
                montoPendienteTotal = (data["totalSaldoPendiente"] as? Number)?.toDouble()?.coerceAtLeast(0.0) ?: 0.0,
                ingresosMes = ingresosActual,
                entregasHoy = (hoyData["entregas"] as? Number)?.toInt() ?: 0,
                devolucionesHoy = (hoyData["devoluciones"] as? Number)?.toInt() ?: 0
            )
        }
        _uiState.update { it.copy(variacionMensualPct = variacion) }
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

    private fun updateEstadisticas(update: Estadisticas.() -> Estadisticas) {
        _uiState.update { it.copy(estadisticas = it.estadisticas.update()) }
    }

    fun exportarResumenFinancieroPdf() {
        val workspace = workspaceManager.currentWorkspace.value ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isExportingPdf = true) }
            val result = alquilerRepository.getAlquileresOnce(workspace.id)
            if (result is Resource.Success) {
                val alquileres = result.data ?: emptyList()
                val ids = alquileres.map { it.id }
                
                // Fetch pagos para el reporte
                val pagosResult = alquilerRepository.getPagosDeAlquileres(workspace.id, ids)
                val allPagos = (pagosResult as? Resource.Success)?.data ?: emptyList()
                
                val anioActual = Calendar.getInstance().get(Calendar.YEAR)
                generarPdfResumenFinancieroUseCase.generarPdf(alquileres, allPagos, anioActual).collect { pdfResult ->
                    if (pdfResult is Resource.Success) {
                        _uiState.update { it.copy(isExportingPdf = false, successMessage = "Resumen generado", pdfResumenUri = pdfResult.data) }
                    } else if (pdfResult is Resource.Error) {
                        _uiState.update { it.copy(isExportingPdf = false, error = pdfResult.message) }
                    }
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
                if (result is Resource.Success) workspaceManager.setWorkspace(updated)
            }
        }
    }

    fun clearMessages() = _uiState.update { it.copy(error = null, successMessage = null) }

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
