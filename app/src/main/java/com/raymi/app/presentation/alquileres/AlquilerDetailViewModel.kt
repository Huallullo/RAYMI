package com.raymi.app.presentation.alquileres

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.core.workspace.WorkspaceManager
import com.raymi.app.domain.model.*
import com.raymi.app.domain.usecase.alquiler.*
import com.raymi.app.domain.usecase.pdf.GenerarPdfAlquilerUseCase
import com.raymi.app.domain.usecase.pdf.SharePdfUseCase
import com.raymi.app.domain.repository.ComprobanteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlquilerDetailViewModel @Inject constructor(
    private val getAlquilerByIdUseCase: GetAlquilerByIdUseCase,
    private val registrarDevolucionUseCase: RegistrarDevolucionUseCase,
    private val cancelarAlquilerUseCase: CancelarAlquilerUseCase,
    private val updateAlquilerUseCase: UpdateAlquilerUseCase,
    private val addPagoUseCase: AddPagoUseCase,
    private val getPagosUseCase: GetPagosUseCase,
    private val generarPdfAlquilerUseCase: GenerarPdfAlquilerUseCase,
    private val sharePdfUseCase: SharePdfUseCase,
    private val viewPdfUseCase: com.raymi.app.domain.usecase.pdf.ViewPdfUseCase,
    private val comprobanteRepository: ComprobanteRepository,
    private val workspaceManager: WorkspaceManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val alquilerId: String = savedStateHandle["alquilerId"] ?: ""

    private val _uiState = MutableStateFlow(AlquilerDetailUiState())
    val uiState: StateFlow<AlquilerDetailUiState> = _uiState.asStateFlow()

    init {
        loadAlquiler()
    }

    fun loadAlquiler() {
        viewModelScope.launch {
            workspaceManager.currentWorkspace.collectLatest { workspace ->
                if (workspace != null && alquilerId.isNotBlank()) {
                    // Cargar Alquiler
                    launch {
                        getAlquilerByIdUseCase(alquilerId).collect { result ->
                            when (result) {
                                is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                                is Resource.Success -> _uiState.update { it.copy(alquiler = result.data, isLoading = false) }
                                is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                            }
                        }
                    }
                    // Cargar Historial de Pagos
                    launch {
                        getPagosUseCase(workspace.id, alquilerId).collect { result ->
                            if (result is Resource.Success) {
                                _uiState.update { it.copy(pagos = result.data ?: emptyList()) }
                            }
                        }
                    }
                    // Cargar Comprobantes
                    launch {
                        comprobanteRepository.getComprobantesByAlquiler(workspace.id, alquilerId).collect { result ->
                            if (result is Resource.Success) {
                                _uiState.update { it.copy(comprobantes = result.data ?: emptyList()) }
                            }
                        }
                    }
                }
            }
        }
    }

    fun registrarPago(monto: Double, metodo: MetodoPago, referencia: String = "") {
        val workspaceId = workspaceManager.getWorkspaceId() ?: return
        val pago = Pago(alquilerId = alquilerId, monto = monto, metodoPago = metodo, referencia = referencia)

        viewModelScope.launch {
            addPagoUseCase(workspaceId, alquilerId, pago).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isProcessing = true) }
                    is Resource.Success -> {
                        _uiState.update { it.copy(isProcessing = false, successMessage = "Abono registrado") }
                        loadAlquiler()
                    }
                    is Resource.Error -> _uiState.update { it.copy(isProcessing = false, error = result.message) }
                }
            }
        }
    }

    fun registrarDevolucion(penalidad: Double = 0.0, observaciones: String = "", montoGarantiaRetenida: Double = 0.0) {
        viewModelScope.launch {
            registrarDevolucionUseCase(alquilerId, penalidad, observaciones, montoGarantiaRetenida).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isProcessing = true) }
                    }
                    is Resource.Success -> {
                        _uiState.update { 
                            it.copy(
                                isProcessing = false,
                                successMessage = "Devolución registrada correctamente"
                            ) 
                        }
                        loadAlquiler()
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                error = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun cancelarAlquiler(motivo: String) {
        viewModelScope.launch {
            cancelarAlquilerUseCase(alquilerId, motivo).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isProcessing = true) }
                    is Resource.Success -> {
                        _uiState.update { it.copy(isProcessing = false, successMessage = "Alquiler cancelado correctamente") }
                        loadAlquiler()
                    }
                    is Resource.Error -> _uiState.update { it.copy(isProcessing = false, error = result.message) }
                }
            }
        }
    }

    fun liquidarDeuda() {
        val alquiler = _uiState.value.alquiler ?: return
        if (alquiler.saldoPendienteReal <= 0) return

        viewModelScope.launch {
            val alquilerActualizado = alquiler.copy(
                adelanto = alquiler.precioTotal + alquiler.penalidad,
                saldo = 0.0,
                updatedAt = com.google.firebase.Timestamp.now()
            )
            updateAlquilerUseCase(alquilerActualizado).collect { result ->
                if (result is Resource.Success) {
                    _uiState.update { it.copy(successMessage = "Deuda liquidada con éxito") }
                    loadAlquiler()
                } else if (result is Resource.Error) {
                    _uiState.update { it.copy(error = result.message) }
                }
            }
        }
    }

    fun generarPdf() {
        val alquiler = _uiState.value.alquiler ?: return
        val workspace = workspaceManager.currentWorkspace.value
        val pagos = _uiState.value.pagos
        
        viewModelScope.launch {
            generarPdfAlquilerUseCase.generarPdf(alquiler, workspace, pagos).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isProcessing = true) }
                    }
                    is Resource.Success -> {
                        _uiState.update { it.copy(
                            isProcessing = false,
                            successMessage = "¡Recibo generado! Ya puedes compartirlo.",
                            pdfUri = result.data
                        ) }
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(
                            isProcessing = false,
                            error = result.message
                        ) }
                    }
                }
            }
        }
    }

    fun compartirPdf(uri: Uri) {
        val result = sharePdfUseCase(uri)
        if (result is Resource.Error) {
            _uiState.update { it.copy(error = result.message) }
        }
    }

    fun abrirPdf(uri: Uri) {
        val result = viewPdfUseCase(uri)
        if (result is Resource.Error) {
            _uiState.update { it.copy(error = result.message) }
        }
    }

    fun updateAlquiler(alquiler: Alquiler) {
        val anterior = _uiState.value.alquiler ?: return
        val diff = alquiler.cantidad - anterior.cantidad

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            updateAlquilerUseCase(alquiler, diff).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update { 
                            it.copy(
                                isProcessing = false, 
                                successMessage = "Alquiler actualizado correctamente" 
                            ) 
                        }
                        loadAlquiler()
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isProcessing = false, error = result.message) }
                    }
                    is Resource.Loading -> { }
                }
            }
        }
    }

    fun anularComprobante(comprobanteId: String) {
        val workspaceId = workspaceManager.getWorkspaceId() ?: return
        viewModelScope.launch {
            comprobanteRepository.anularComprobante(workspaceId, comprobanteId).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isProcessing = true) }
                    is Resource.Success -> {
                        _uiState.update { it.copy(isProcessing = false, successMessage = "Comprobante anulado") }
                        loadAlquiler()
                    }
                    is Resource.Error -> _uiState.update { it.copy(isProcessing = false, error = result.message) }
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(
            error = null,
            successMessage = null
        ) }
    }
}

data class AlquilerDetailUiState(
    val alquiler: Alquiler? = null,
    val pagos: List<Pago> = emptyList(),
    val comprobantes: List<Comprobante> = emptyList(),
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val pdfUri: Uri? = null
)
