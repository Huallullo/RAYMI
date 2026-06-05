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
import com.google.firebase.Timestamp
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

    private var planJob: kotlinx.coroutines.Job? = null // ✅ FIX PROBLEM 6a: Job para cancelar suscripción anterior

    fun debeMostrarAnuncios(): Boolean = adManager.debeMostrarAnuncios(_uiState.value.currentPlan)

    init {
        observeWorkspace()
        viewModelScope.launch {
            workspaceManager.currentWorkspace.filterNotNull().first().let {
                refreshData()
                cargarVencimientosHoy() // ✅ MEJORA B
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
     * ✅ CORREGIDO: Uso de try-finally para asegurar que el loading desaparezca.
     */
    fun refreshData(forceRefresh: Boolean = false) {
        val workspaceId = workspaceManager.getWorkspaceId() ?: return
        
        if (forceRefresh) {
            performFullAudit()
        } else {
            viewModelScope.launch {
                try {
                    _uiState.update { it.copy(isLoading = true) }
                    // Usamos el caso de uso de stats con forzar lectura de red
                    getWorkspaceStatsUseCase(workspaceId, forceRefresh = true).collect { result ->
                        when (result) {
                            is Resource.Success -> handleStatsResult(result.data ?: emptyMap())
                            is Resource.Error -> _uiState.update { it.copy(error = result.message) }
                            else -> {}
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = "Error de conexión: ${e.localizedMessage}") }
                } finally {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    /**
     * AUDITORÍA PROFUNDA: Recalcula todo.
     * ✅ CORREGIDO: Blindaje total contra bloqueos de UI.
     */
    fun performFullAudit() {
        val workspaceId = workspaceManager.getWorkspaceId() ?: return
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                // 1. Invalidar caches
                itemRepository.invalidateCache(workspaceId)
                
                // 2. Ejecutar auditoría
                val resAlq = alquilerRepository.getAlquileresOnce(workspaceId, limit = 150)
                val alquileres = (resAlq as? Resource.Success)?.data ?: emptyList()
                val ids = alquileres.map { it.id }
                
                val pagosResult = alquilerRepository.getPagosDeAlquileres(workspaceId, ids)
                val allPagos = (pagosResult as? Resource.Success)?.data ?: emptyList()

                val resItems = itemRepository.getItemsByWorkspaceOnce(workspaceId, 200)
                val items = (resItems as? Resource.Success)?.data ?: emptyList()
                
                val resClientes = clienteRepository.getClientesOnce(workspaceId)
                val clientes = (resClientes as? Resource.Success)?.data ?: emptyList()

                val cal = Calendar.getInstance()
                val anio = cal.get(Calendar.YEAR)
                val mes = cal.get(Calendar.MONTH)
                val diaAnio = cal.get(Calendar.DAY_OF_YEAR)
                
                val ingresosEsteMes = allPagos.filter {
                    val c = Calendar.getInstance().apply { time = it.fecha.toDate() }
                    c.get(Calendar.YEAR) == anio && c.get(Calendar.MONTH) == mes
                }.sumOf { it.monto }

                val totalRecaudado = allPagos.sumOf { it.monto }
                val saldoPendienteTotal = alquileres.filter { it.estado != EstadoAlquiler.DEVUELTO && it.estado != EstadoAlquiler.CANCELADO }
                    .sumOf { it.saldoPendienteReal }

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
                _uiState.update { it.copy(
                    successMessage = "Sincronización completada",
                    ultimosAlquileres = alquileres,
                    ultimosPagos = allPagos
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Falla en auditoría: ${e.localizedMessage}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
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

        // ✅ MEJORA A: Extraer ingresos de los últimos 6 meses para la gráfica
        val historicoIngresos = mutableListOf<Pair<String, Double>>()
        val tempCal = Calendar.getInstance()
        for (i in 0..5) {
            val y = tempCal.get(Calendar.YEAR)
            val m = tempCal.get(Calendar.MONTH)
            val valIngreso = (data["ingresos_${y}_$m"] as? Number)?.toDouble() ?: 0.0
            val label = tempCal.getDisplayName(Calendar.MONTH, Calendar.SHORT, java.util.Locale.getDefault()) ?: ""
            historicoIngresos.add(label to valIngreso)
            tempCal.add(Calendar.MONTH, -1)
        }

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
        _uiState.update { it.copy(
            variacionMensualPct = variacion,
            ingresosHistoricos = historicoIngresos.reversed() 
        ) }
    }

    private fun cargarPlan(ownerId: String) {
        planJob?.cancel() // ✅ Cancelar el Job anterior si existe
        planJob = viewModelScope.launch {
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
        val currentAlquileres = _uiState.value.ultimosAlquileres
        val currentPagos = _uiState.value.ultimosPagos
        
        viewModelScope.launch {
            _uiState.update { it.copy(isExportingPdf = true) }
            
            // ✅ FIX PROBLEM 6b: Usar datos ya cargados si existen
            val alquileres: List<Alquiler>
            val pagos: List<Pago>
            
            if (currentAlquileres.isNotEmpty()) {
                alquileres = currentAlquileres
                pagos = currentPagos
            } else {
                // Solo si no hay nada en memoria (raro en dashboard), leemos
                val result = alquilerRepository.getAlquileresOnce(workspace.id)
                alquileres = (result as? Resource.Success)?.data ?: emptyList()
                val ids = alquileres.map { it.id }
                val pagosResult = alquilerRepository.getPagosDeAlquileres(workspace.id, ids)
                pagos = (pagosResult as? Resource.Success)?.data ?: emptyList()
            }
                
            val anioActual = Calendar.getInstance().get(Calendar.YEAR)
            generarPdfResumenFinancieroUseCase.generarPdf(alquileres, pagos, anioActual).collect { pdfResult ->
                if (pdfResult is Resource.Success) {
                    _uiState.update { it.copy(isExportingPdf = false, successMessage = "Resumen generado", pdfResumenUri = pdfResult.data) }
                } else if (pdfResult is Resource.Error) {
                    _uiState.update { it.copy(isExportingPdf = false, error = pdfResult.message) }
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

    /**
     * MEJORA B: Cargar alquileres que vencen hoy para mostrar en Dashboard.
     */
    fun cargarVencimientosHoy() {
        val workspaceId = workspaceManager.getWorkspaceId() ?: return
        viewModelScope.launch {
            val hoy = Calendar.getInstance()
            hoy.set(Calendar.HOUR_OF_DAY, 0); hoy.set(Calendar.MINUTE, 0); hoy.set(Calendar.SECOND, 0)
            val start = Timestamp(hoy.time)
            hoy.set(Calendar.HOUR_OF_DAY, 23); hoy.set(Calendar.MINUTE, 59); hoy.set(Calendar.SECOND, 59)
            val end = Timestamp(hoy.time)

            alquilerRepository.getAlquileresByDateRange(workspaceId, start, end).collect { result ->
                if (result is Resource.Success) {
                    _uiState.update { it.copy(vencimientosHoy = result.data ?: emptyList()) }
                }
            }
        }
    }

    fun clearMessages() = _uiState.update { it.copy(error = null, successMessage = null) }

    data class DashboardUiState(
        val currentWorkspace: Workspace? = null,
        val currentPlan: UserPlan? = null,
        val estadisticas: Estadisticas = Estadisticas(),
        val ultimosAlquileres: List<Alquiler> = emptyList(),
        val ultimosPagos: List<Pago> = emptyList(),
        val ingresosHistoricos: List<Pair<String, Double>> = emptyList(), // ✅ MEJORA A
        val vencimientosHoy: List<Alquiler> = emptyList(),                // ✅ MEJORA B
        val actividadSemanal: Map<String, Int> = emptyMap(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val variacionMensualPct: Double = 0.0,
        val isExportingPdf: Boolean = false,
        val successMessage: String? = null,
        val pdfResumenUri: Uri? = null
    )
}
