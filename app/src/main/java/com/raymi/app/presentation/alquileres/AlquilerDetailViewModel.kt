// ========== AlquilerDetailViewModel.kt ==========
package com.raymi.app.presentation.alquileres

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.EstadoAlquiler
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.usecase.alquiler.GetAlquilerByIdUseCase
import com.raymi.app.domain.usecase.alquiler.RegistrarDevolucionUseCase
import com.raymi.app.domain.usecase.alquiler.UpdateAlquilerUseCase
import com.raymi.app.domain.usecase.notifications.EnviarMensajeUseCase
import com.raymi.app.domain.usecase.pdf.GenerarPdfAlquilerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlquilerDetailViewModel @Inject constructor(
    private val getAlquilerByIdUseCase: GetAlquilerByIdUseCase,
    private val registrarDevolucionUseCase: RegistrarDevolucionUseCase,
    private val updateAlquilerUseCase: UpdateAlquilerUseCase,
    private val enviarMensajeUseCase: EnviarMensajeUseCase,
    private val generarPdfAlquilerUseCase: GenerarPdfAlquilerUseCase,
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
            getAlquilerByIdUseCase(alquilerId).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            alquiler = result.data,
                            isLoading = false
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun registrarDevolucion() {
        viewModelScope.launch {
            registrarDevolucionUseCase(alquilerId).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isProcessing = true)
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            successMessage = "Devolución registrada correctamente"
                        )
                        // Recargar datos
                        loadAlquiler()
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
    fun registrarPago(montoPago: Double) {
        viewModelScope.launch {
            val alquilerActual = _uiState.value.alquiler ?: return@launch

            val nuevoAdelanto = alquilerActual.adelanto + montoPago
            val nuevoSaldo = alquilerActual.precioTotal - nuevoAdelanto

            // Determinar nuevo estado: si saldo es 0 o negativo, CANCELADO; sino, el mismo
            val nuevoEstado = if (nuevoSaldo <= 0.0) EstadoAlquiler.CANCELADO else alquilerActual.estado

            val alquilerActualizado = alquilerActual.copy(
                adelanto = nuevoAdelanto,
                saldo = nuevoSaldo,
                estado = nuevoEstado,
                updatedAt = com.google.firebase.Timestamp.now()
            )

            updateAlquilerUseCase(alquilerActualizado).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.value = _uiState.value.copy(isProcessing = true)
                    is Resource.Success -> {
                        val mensaje = if (nuevoSaldo <= 0) {
                            "¡Alquiler cancelado! Deuda saldada. Puedes proceder a la devolución."
                        } else {
                            "Pago registrado. Saldo restante: S/. ${String.format(java.util.Locale.getDefault(), "%.2f", nuevoSaldo)}"
                        }
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            successMessage = mensaje
                        )
                        loadAlquiler()
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
    // Agregar método para actualizar:
    fun updateAlquiler(alquiler: Alquiler) {
        viewModelScope.launch {
            updateAlquilerUseCase(alquiler).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isProcessing = true)
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            successMessage = "Alquiler actualizado correctamente"
                        )
                        loadAlquiler()
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
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            error = null,
            successMessage = null
        )
    }

    fun generarPdf() {
        val alquiler = _uiState.value.alquiler ?: return
        viewModelScope.launch {
            generarPdfAlquilerUseCase.generarPdf(alquiler).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isProcessing = true)
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            successMessage = "PDF generado correctamente"
                        )
                        // Aquí almacenamos el Uri generado para compartirlo después
                        _uiState.value = _uiState.value.copy(pdfUri = result.data)
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
        val mensaje = "Detalle del alquiler de ${alquiler.vestuarioNombre} para ${alquiler.clienteNombre}"

        viewModelScope.launch {
            enviarMensajeUseCase.compartirPdfPorWhatsApp(pdfUri, mensaje).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isProcessing = true)
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            successMessage = result.data
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
}

data class AlquilerDetailUiState(
    val alquiler: Alquiler? = null,
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val pdfUri: Uri? = null // Cambiado a Uri para el PDF generado
)
