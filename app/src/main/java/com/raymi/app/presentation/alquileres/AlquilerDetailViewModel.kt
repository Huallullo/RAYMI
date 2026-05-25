package com.raymi.app.presentation.alquileres

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.core.workspace.WorkspaceManager
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.EstadoAlquiler
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.usecase.alquiler.GetAlquilerByIdUseCase
import com.raymi.app.domain.usecase.alquiler.RegistrarDevolucionUseCase
import com.raymi.app.domain.usecase.alquiler.UpdateAlquilerUseCase
import com.raymi.app.domain.usecase.notifications.EnviarMensajeUseCase
import com.raymi.app.domain.usecase.pdf.GenerarPdfAlquilerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlquilerDetailViewModel @Inject constructor(
    private val getAlquilerByIdUseCase: GetAlquilerByIdUseCase,
    private val registrarDevolucionUseCase: RegistrarDevolucionUseCase,
    private val updateAlquilerUseCase: UpdateAlquilerUseCase,
    private val enviarMensajeUseCase: EnviarMensajeUseCase,
    private val generarPdfAlquilerUseCase: GenerarPdfAlquilerUseCase,
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
                    getAlquilerByIdUseCase(alquilerId).collect { result ->
                        when (result) {
                            is Resource.Loading -> {
                                _uiState.update { it.copy(isLoading = true) }
                            }
                            is Resource.Success -> {
                                _uiState.update { it.copy(alquiler = result.data, isLoading = false) }
                            }
                            is Resource.Error -> {
                                _uiState.update { it.copy(isLoading = false, error = result.message) }
                            }
                        }
                    }
                }
            }
        }
    }

    fun registrarDevolucion() {
        val alquiler = _uiState.value.alquiler ?: return
        
        // Regla de Negocio Senior: No permitir devolución si hay deuda (QA Fix)
        if (alquiler.saldo > 0) {
            _uiState.update { it.copy(error = "No se puede devolver: El cliente tiene un saldo pendiente de S/. ${alquiler.saldo}") }
            return
        }

        viewModelScope.launch {
            registrarDevolucionUseCase(alquilerId).collect { result ->
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

    /**
     * Permite al usuario liquidar la deuda desde el detalle (QA Bonus)
     */
    fun liquidarDeuda() {
        val alquiler = _uiState.value.alquiler ?: return
        if (alquiler.saldo <= 0) return

        viewModelScope.launch {
            val alquilerActualizado = alquiler.copy(
                adelanto = alquiler.precioTotal,
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
        
        viewModelScope.launch {
            generarPdfAlquilerUseCase.generarPdf(alquiler, workspace).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isProcessing = true)
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            successMessage = "¡Recibo generado! Ya puedes compartirlo.",
                            pdfUri = result.data
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun compartirPdfPorWhatsApp() {
        val pdfUri = _uiState.value.pdfUri ?: run {
            _uiState.value = _uiState.value.copy(error = "Primero genera el PDF")
            return
        }

        val alquiler = _uiState.value.alquiler ?: return
        val mensaje = "Detalle del alquiler de ${alquiler.itemNombre} para ${alquiler.clienteNombre}"

        viewModelScope.launch {
            enviarMensajeUseCase.compartirBoletaDigital(pdfUri, mensaje).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isProcessing = true)
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            successMessage = result.data ?: "Enviado"
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun updateAlquiler(alquiler: Alquiler) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            updateAlquilerUseCase(alquiler).collect { result ->
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

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            error = null,
            successMessage = null
        )
    }
}

data class AlquilerDetailUiState(
    val alquiler: Alquiler? = null,
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val pdfUri: Uri? = null
)
