package com.raymi.app.presentation.alquileres

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.Cliente
import com.raymi.app.domain.model.EstadoAlquiler
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.Vestuario
import com.raymi.app.domain.usecase.alquiler.CreateAlquilerUseCase
import com.raymi.app.domain.usecase.cliente.GetClientesUseCase
import com.raymi.app.domain.usecase.vestuario.GetVestuariosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CreateAlquilerViewModel @Inject constructor(
    private val getClientesUseCase: GetClientesUseCase,
    private val getVestuariosUseCase: GetVestuariosUseCase,
    private val createAlquilerUseCase: CreateAlquilerUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateAlquilerUiState())
    val uiState: StateFlow<CreateAlquilerUiState> = _uiState.asStateFlow()

    init {
        loadClientes()
        loadVestuariosDisponibles()
    }

    private fun loadClientes() {
        viewModelScope.launch {
            getClientesUseCase().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            clientes = result.data ?: emptyList()
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(error = result.message)
                    }
                    is Resource.Loading -> {}
                }
            }
        }
    }

    private fun loadVestuariosDisponibles() {
        viewModelScope.launch {
            getVestuariosUseCase().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val disponibles = result.data?.filter {
                            it.estado.name == "DISPONIBLE"
                        } ?: emptyList()
                        _uiState.value = _uiState.value.copy(
                            vestuariosDisponibles = disponibles
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(error = result.message)
                    }
                    is Resource.Loading -> {}
                }
            }
        }
    }

    fun selectCliente(cliente: Cliente) {
        _uiState.value = _uiState.value.copy(
            selectedCliente = cliente,
            showClienteDialog = false
        )
    }

    fun selectVestuario(vestuario: Vestuario) {
        _uiState.value = _uiState.value.copy(
            selectedVestuario = vestuario,
            showVestuarioDialog = false,
            precioUnitario = vestuario.precio.toString()
        )
        calcularPrecioTotal()
    }

    // ========== CANTIDAD ==========

    fun onCantidadChange(cantidad: String) {
        // Solo permitir números positivos
        val cantidadInt = cantidad.toIntOrNull()
        if (cantidadInt != null && cantidadInt > 0) {
            _uiState.value = _uiState.value.copy(cantidad = cantidad)
            calcularPrecioTotal()
        } else if (cantidad.isEmpty()) {
            _uiState.value = _uiState.value.copy(cantidad = "")
        }
    }

    // ========== CÁLCULOS ==========

    private fun calcularPrecioTotal() {
        val precioUnit = _uiState.value.precioUnitario.toDoubleOrNull() ?: 0.0
        val cantidad = _uiState.value.cantidad.toIntOrNull() ?: 1
        val total = precioUnit * cantidad

        _uiState.value = _uiState.value.copy(
            precioTotal = String.format("%.2f", total)
        )
        calcularSaldo()
    }

    fun showClienteDialog() {
        _uiState.value = _uiState.value.copy(showClienteDialog = true)
    }

    fun hideClienteDialog() {
        _uiState.value = _uiState.value.copy(showClienteDialog = false)
    }

    fun showVestuarioDialog() {
        _uiState.value = _uiState.value.copy(showVestuarioDialog = true)
    }

    fun hideVestuarioDialog() {
        _uiState.value = _uiState.value.copy(showVestuarioDialog = false)
    }

    fun searchClientes(query: String) {
        _uiState.value = _uiState.value.copy(clienteSearchQuery = query)
    }

    fun searchVestuarios(query: String) {
        _uiState.value = _uiState.value.copy(vestuarioSearchQuery = query)
    }

    fun setFechaInicio(fecha: Date) {
        _uiState.value = _uiState.value.copy(fechaInicio = fecha)
        calcularDias()
    }

    fun setFechaFin(fecha: Date) {
        _uiState.value = _uiState.value.copy(fechaFin = fecha)
        calcularDias()
    }

    private fun calcularDias() {
        val inicio = _uiState.value.fechaInicio
        val fin = _uiState.value.fechaFin

        if (inicio != null && fin != null && fin.after(inicio)) {
            val diff = fin.time - inicio.time
            val dias = (diff / (1000 * 60 * 60 * 24)).toInt() + 1
            _uiState.value = _uiState.value.copy(diasAlquiler = dias)
        } else {
            _uiState.value = _uiState.value.copy(diasAlquiler = 0)
        }
    }

    fun onAdelantoChange(adelanto: String) {
        _uiState.value = _uiState.value.copy(adelanto = adelanto)
        calcularSaldo()
    }

    private fun calcularSaldo() {
        val precio = _uiState.value.precioTotal.toDoubleOrNull() ?: 0.0
        val adelanto = _uiState.value.adelanto.toDoubleOrNull() ?: 0.0
        val saldo = precio - adelanto
        _uiState.value = _uiState.value.copy(saldo = saldo)
    }

    fun onObservacionesChange(observaciones: String) {
        _uiState.value = _uiState.value.copy(observaciones = observaciones)
    }

    fun createAlquiler() {
        // Validar campos
        if (_uiState.value.selectedCliente == null) {
            _uiState.value = _uiState.value.copy(error = "Debe seleccionar un cliente")
            return
        }

        if (_uiState.value.selectedVestuario == null) {
            _uiState.value = _uiState.value.copy(error = "Debe seleccionar un vestuario")
            return
        }

        if (_uiState.value.fechaInicio == null) {
            _uiState.value = _uiState.value.copy(error = "Debe seleccionar fecha de inicio")
            return
        }

        if (_uiState.value.fechaFin == null) {
            _uiState.value = _uiState.value.copy(error = "Debe seleccionar fecha de devolución")
            return
        }

        val cantidad = _uiState.value.cantidad.toIntOrNull()
        if (cantidad == null || cantidad <= 0) {
            _uiState.value = _uiState.value.copy(error = "La cantidad debe ser mayor a 0")
            return
        }

        val precioUnit = _uiState.value.precioUnitario.toDoubleOrNull()
        if (precioUnit == null || precioUnit <= 0) {
            _uiState.value = _uiState.value.copy(error = "Precio inválido")
            return
        }

        val precioTotal = _uiState.value.precioTotal.toDoubleOrNull() ?: 0.0
        val adelanto = _uiState.value.adelanto.toDoubleOrNull() ?: 0.0

        if (adelanto > precioTotal) {
            _uiState.value = _uiState.value.copy(error = "El adelanto no puede ser mayor al precio total")
            return
        }

        // Crear alquiler
        val alquiler = Alquiler(
            clienteId = _uiState.value.selectedCliente!!.id,
            clienteNombre = _uiState.value.selectedCliente!!.nombreCompleto,
            vestuarioId = _uiState.value.selectedVestuario!!.id,
            vestuarioNombre = _uiState.value.selectedVestuario!!.danza,
            vestuarioCodigo = _uiState.value.selectedVestuario!!.codigo,
            cantidad = cantidad,  // ✅
            fechaInicio = Timestamp(_uiState.value.fechaInicio!!),
            fechaFinPrevista = Timestamp(_uiState.value.fechaFin!!),
            precioUnitario = precioUnit,  // ✅
            precioTotal = precioTotal,
            adelanto = adelanto,
            saldo = _uiState.value.saldo,
            estado = EstadoAlquiler.ACTIVO,
            observaciones = _uiState.value.observaciones,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )

        viewModelScope.launch {
            createAlquilerUseCase(alquiler).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSuccess = true,
                            successMessage = "Alquiler creado exitosamente"
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

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            error = null,
            successMessage = null
        )
    }
}

data class CreateAlquilerUiState(
    val clientes: List<Cliente> = emptyList(),
    val vestuariosDisponibles: List<Vestuario> = emptyList(),
    val selectedCliente: Cliente? = null,
    val selectedVestuario: Vestuario? = null,
    val cantidad: String = "1",  // ✅ NUEVO
    val fechaInicio: Date? = null,
    val fechaFin: Date? = null,
    val diasAlquiler: Int = 0,
    val precioUnitario: String = "",  // ✅ NUEVO
    val precioTotal: String = "",
    val adelanto: String = "",
    val saldo: Double = 0.0,
    val observaciones: String = "",
    val showClienteDialog: Boolean = false,
    val showVestuarioDialog: Boolean = false,
    val clienteSearchQuery: String = "",
    val vestuarioSearchQuery: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)