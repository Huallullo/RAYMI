package com.raymi.app.presentation.comprobantes

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.core.workspace.WorkspaceManager
import com.raymi.app.domain.model.*
import com.raymi.app.domain.repository.AlquilerRepository
import com.raymi.app.domain.repository.AuthRepository
import com.raymi.app.domain.repository.ExternalLookupRepository
import com.raymi.app.domain.usecase.comprobante.GenerateComprobanteUseCase
import com.raymi.app.domain.usecase.pdf.SharePdfUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GenerateComprobanteViewModel @Inject constructor(
    private val generateComprobanteUseCase: GenerateComprobanteUseCase,
    private val alquilerRepository: AlquilerRepository,
    private val authRepository: AuthRepository,
    private val lookupRepository: ExternalLookupRepository,
    private val sharePdfUseCase: SharePdfUseCase,
    private val workspaceManager: WorkspaceManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val alquilerId: String = savedStateHandle["alquilerId"] ?: ""

    private val _uiState = MutableStateFlow(GenerateUiState())
    val uiState: StateFlow<GenerateUiState> = _uiState.asStateFlow()

    init {
        cargarDatos()
    }

    private fun cargarDatos() {
        if (alquilerId.isBlank()) return
        viewModelScope.launch {
            alquilerRepository.getAlquilerById(alquilerId).collect { result ->
                if (result is Resource.Success) {
                    val alquiler = result.data ?: return@collect
                    _uiState.update { it.copy(
                        alquiler = alquiler,
                        clienteNombre = alquiler.clienteNombre,
                        clienteDocumento = alquiler.clienteDni
                    ) }
                }
            }
        }
    }

    fun onTipoChange(tipo: TipoComprobante) {
        _uiState.update { it.copy(tipo = tipo) }
    }

    fun onNombreChange(v: String) = _uiState.update { it.copy(clienteNombre = v) }
    fun onDocumentoChange(v: String) = _uiState.update { it.copy(clienteDocumento = v) }
    fun onRazonSocialChange(v: String) = _uiState.update { it.copy(razonSocial = v) }
    fun onDireccionChange(v: String) = _uiState.update { it.copy(direccionFiscal = v) }

    fun buscarDocumento() {
        val doc = _uiState.value.clienteDocumento
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            if (doc.length == 8) {
                val res = lookupRepository.buscarDni(doc)
                if (res is Resource.Success) {
                    _uiState.update { it.copy(clienteNombre = res.data?.nombreCompleto ?: "", isSearching = false) }
                } else {
                    _uiState.update { it.copy(isSearching = false, error = res.message) }
                }
            } else if (doc.length == 11) {
                val res = lookupRepository.buscarRuc(doc)
                if (res is Resource.Success) {
                    _uiState.update { it.copy(razonSocial = res.data?.razonSocial ?: "", direccionFiscal = res.data?.direccion ?: "", isSearching = false) }
                } else {
                    _uiState.update { it.copy(isSearching = false, error = res.message) }
                }
            } else {
                _uiState.update { it.copy(isSearching = false, error = "Documento debe tener 8 (DNI) u 11 (RUC) dígitos") }
            }
        }
    }

    fun generarComprobante() {
        val state = _uiState.value
        val alquiler = state.alquiler ?: return
        val workspace = workspaceManager.currentWorkspace.value ?: return

        // Validaciones
        if (state.tipo == TipoComprobante.BOLETA) {
            if (state.clienteDocumento.length != 8) {
                _uiState.update { it.copy(error = "DNI de 8 dígitos requerido para Boleta") }
                return
            }
            if (state.clienteNombre.isBlank()) {
                _uiState.update { it.copy(error = "Nombre del cliente es requerido para Boleta") }
                return
            }
        }
        
        if (state.tipo == TipoComprobante.FACTURA) {
             if (state.clienteDocumento.length != 11) {
                 _uiState.update { it.copy(error = "RUC de 11 dígitos requerido para Factura") }
                 return
             }
             if (state.razonSocial.isBlank()) {
                 _uiState.update { it.copy(error = "Razón Social requerida para Factura") }
                 return
             }
             if (workspace.ruc.isBlank() || workspace.direccion.isBlank()) {
                 _uiState.update { it.copy(error = "Completa los datos del negocio en Configuración antes de generar factura.") }
                 return
             }
        }

        if (alquiler.precioTotal < 0) {
            _uiState.update { it.copy(error = "El total del alquiler no puede ser negativo") }
            return
        }

        viewModelScope.launch {
            val uid = authRepository.getCurrentUser()?.uid ?: "admin"
            
            val comprobante = Comprobante(
                workspaceId = workspace.id,
                alquilerId = alquiler.id,
                tipo = state.tipo,
                serie = when(state.tipo) {
                    TipoComprobante.TICKET -> workspace.serieTicket
                    TipoComprobante.BOLETA -> workspace.serieBoleta
                    TipoComprobante.FACTURA -> workspace.serieFactura
                },
                clienteId = alquiler.clienteId,
                clienteNombre = state.clienteNombre,
                clienteDocumento = state.clienteDocumento,
                clienteTipoDocumento = when(state.clienteDocumento.length) {
                    8 -> TipoDocumentoCliente.DNI
                    11 -> TipoDocumentoCliente.RUC
                    else -> TipoDocumentoCliente.SIN_DOCUMENTO
                },
                razonSocial = state.razonSocial.ifBlank { null },
                direccionFiscal = state.direccionFiscal.ifBlank { null },
                subtotal = alquiler.precioTotal / 1.18,
                igv = alquiler.precioTotal - (alquiler.precioTotal / 1.18),
                total = alquiler.precioTotal,
                pagado = alquiler.adelanto,
                saldo = alquiler.saldoPendienteReal,
                generadoPor = uid
            )

            generateComprobanteUseCase(comprobante, alquiler, workspace).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isSaving = true) }
                    is Resource.Success -> {
                        val data = result.data!!
                        _uiState.update { it.copy(
                            isSaving = false, 
                            isSuccess = true, 
                            generatedPdfUri = data.pdfUri
                        ) }
                    }
                    is Resource.Error -> _uiState.update { it.copy(isSaving = false, error = result.message) }
                }
            }
        }
    }

    fun compartirPdf() {
        _uiState.value.generatedPdfUri?.let { sharePdfUseCase(it) }
    }

    fun clearMessages() = _uiState.update { it.copy(error = null) }
}

data class GenerateUiState(
    val alquiler: Alquiler? = null,
    val tipo: TipoComprobante = TipoComprobante.TICKET,
    val clienteNombre: String = "",
    val clienteDocumento: String = "",
    val razonSocial: String = "",
    val direccionFiscal: String = "",
    val isSearching: Boolean = false,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val generatedPdfUri: Uri? = null
)
