package com.raymi.app.presentation.alquileres

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.raymi.app.core.workspace.WorkspaceManager
import com.raymi.app.domain.model.*
import com.raymi.app.domain.usecase.alquiler.CreateAlquilerUseCase
import com.raymi.app.domain.usecase.cliente.GetClientesUseCase
import com.raymi.app.domain.usecase.item.GetItemsUseCase
import com.raymi.app.domain.usecase.notifications.EnviarMensajeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

/**
 * ViewModel para la creación de alquileres (Wizard de Contratación).
 * Diseño Senior: Cálculos automáticos, validaciones en tiempo real y soporte multi-negocio.
 */
@HiltViewModel
class CreateAlquilerViewModel @Inject constructor(
    private val getClientesUseCase: GetClientesUseCase,
    private val getItemsUseCase: GetItemsUseCase,
    private val getCategoriasUseCase: com.raymi.app.domain.usecase.categoria.GetCategoriasUseCase,
    private val createAlquilerUseCase: CreateAlquilerUseCase,
    private val enviarMensajeUseCase: EnviarMensajeUseCase,
    private val workspaceManager: WorkspaceManager,
    savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    private val preselectedItemId: String? = savedStateHandle["itemId"]

    private val _uiState = MutableStateFlow(CreateAlquilerUiState())
    val uiState: StateFlow<CreateAlquilerUiState> = _uiState.asStateFlow()

    init {
        cargarDatosIniciales()
    }

    private fun cargarDatosIniciales() {
        viewModelScope.launch {
            workspaceManager.currentWorkspace.collectLatest { workspace ->
                if (workspace != null) {
                    val workspaceId = workspace.id
                    
                    // QA Fix Senior: Lanzar corrutinas separadas para cada flujo
                    // porque .collect en Firestore nunca termina (es un stream).
                    
                    // 1. Cargar Categorías
                    launch {
                        getCategoriasUseCase(workspaceId).collect { result ->
                            if (result is Resource.Success) {
                                _uiState.update { it.copy(categorias = result.data ?: emptyList()) }
                            }
                        }
                    }

                    // 2. Cargar Clientes
                    launch {
                        getClientesUseCase().collect { result ->
                            if (result is Resource.Success) {
                                _uiState.update { it.copy(clientes = result.data ?: emptyList()) }
                            }
                        }
                    }

                    // 3. Cargar Ítems disponibles
                    launch {
                        getItemsUseCase(workspaceId).collect { result ->
                            if (result is Resource.Success) {
                                val data = result.data ?: emptyList()
                                
                                _uiState.update { state ->
                                    state.copy(
                                        itemsTotales = data,
                                        itemsDisponibles = aplicarFiltroCategoria(data, state.categoriaFiltro)
                                    )
                                }
                                
                                // QA Senior: Pre-seleccionar ítem si viene de navegación
                                if (!preselectedItemId.isNullOrBlank() && _uiState.value.selectedItem == null) {
                                    data.find { it.id == preselectedItemId }?.let { preselected ->
                                        seleccionarItem(preselected)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun filtrarPorCategoria(categoria: Categoria?) {
        _uiState.update { state ->
            state.copy(
                categoriaFiltro = categoria,
                itemsDisponibles = aplicarFiltroCategoria(state.itemsTotales, categoria)
            )
        }
    }

    private fun aplicarFiltroCategoria(items: List<Item>, categoria: Categoria?): List<Item> {
        val base = items.filter { it.estado == "DISPONIBLE" }
        return if (categoria == null) base 
        else base.filter { it.categoriaId == categoria.id }
    }

    fun seleccionarCliente(cliente: Cliente) {
        _uiState.update { it.copy(selectedCliente = cliente, showClienteDialog = false) }
    }

    fun seleccionarItem(item: Item) {
        _uiState.update { 
            it.copy(
                selectedItem = item, 
                showItemDialog = false,
                precioUnitario = item.precio.toString()
            ) 
        }
        recalcularFinanzas()
    }

    fun onCantidadChange(cantidad: String) {
        _uiState.update { it.copy(cantidad = cantidad) }
        recalcularFinanzas()
    }

    fun onAdelantoChange(monto: String) {
        _uiState.update { it.copy(adelanto = monto) }
        recalcularFinanzas()
    }

    private fun recalcularFinanzas() {
        _uiState.update { state ->
            val pUnit = state.precioUnitario.toDoubleOrNull() ?: 0.0
            val cant = state.cantidad.toIntOrNull() ?: 1
            val dias = state.diasAlquiler.coerceAtLeast(1)
            val total = pUnit * cant * dias
            val pagado = state.adelanto.toDoubleOrNull() ?: 0.0
            state.copy(
                precioTotal = total,
                saldo = (total - pagado).coerceAtLeast(0.0)
            )
        }
    }

    fun setFechaInicio(date: Date) {
        _uiState.update { it.copy(fechaInicio = date) }
        validarDuracion()
    }

    fun setFechaFin(date: Date) {
        _uiState.update { it.copy(fechaFin = date) }
        validarDuracion()
    }

    private fun validarDuracion() {
        val inicio = _uiState.value.fechaInicio
        val fin = _uiState.value.fechaFin
        if (inicio != null && fin != null) {
            val diff = fin.time - inicio.time
            val dias = (diff / (1000 * 60 * 60 * 24)).toInt() + 1
            _uiState.update { it.copy(diasAlquiler = dias) }
            recalcularFinanzas() // QA Senior: Recalcular al cambiar fechas
        }
    }

    fun crearAlquiler() {
        val state = _uiState.value
        
        // Validaciones Senior: No vacíos y tipos correctos
        if (state.selectedCliente == null) {
            _uiState.update { it.copy(error = "Debes seleccionar un cliente") }
            return
        }
        if (state.selectedItem == null) {
            _uiState.update { it.copy(error = "Debes seleccionar un producto") }
            return
        }
        if (state.fechaFin == null) {
            _uiState.update { it.copy(error = "Indica la fecha de devolución") }
            return
        }
        if (state.precioTotal <= 0) {
            _uiState.update { it.copy(error = "El precio total debe ser mayor a 0") }
            return
        }

        viewModelScope.launch {
            try {
                val workspaceId = workspaceManager.getWorkspaceId()
                if (workspaceId == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Negocio no identificado") }
                    return@launch
                }
                
                // Normalización de datos (Trimming y Casing)
                val nuevoAlquiler = Alquiler(
                    workspaceId = workspaceId,
                    clienteId = state.selectedCliente.id,
                    clienteNombre = state.selectedCliente.nombreCompleto.trim(),
                    clienteDni = state.selectedCliente.dni.trim(),
                    clienteTelefono = state.selectedCliente.telefono.trim(),
                    itemId = state.selectedItem.id,
                    itemNombre = state.selectedItem.nombre.trim(),
                    itemCodigo = state.selectedItem.codigo.trim(),
                    cantidad = state.cantidad.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                    fechaInicio = Timestamp(state.fechaInicio ?: Date()),
                    fechaFinPrevista = Timestamp(state.fechaFin),
                    precioUnitario = state.precioUnitario.toDoubleOrNull() ?: 0.0,
                    precioTotal = state.precioTotal,
                    adelanto = state.adelanto.toDoubleOrNull() ?: 0.0,
                    saldo = state.saldo,
                    observaciones = state.observaciones.trim()
                )

                createAlquilerUseCase(nuevoAlquiler).collect { result ->
                    when (result) {
                        is Resource.Loading -> _uiState.update { it.copy(isLoading = true, error = null) }
                        is Resource.Success -> {
                            _uiState.update { it.copy(
                                isLoading = false, 
                                isSuccess = true,
                                successMessage = "Alquiler registrado correctamente"
                            ) }
                            enviarConfirmacionWhatsApp(nuevoAlquiler)
                        }
                        is Resource.Error -> {
                            _uiState.update { it.copy(isLoading = false, error = result.message) }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Error técnico: ${e.localizedMessage}") }
            }
        }
    }

    private fun enviarConfirmacionWhatsApp(alquiler: Alquiler) {
        viewModelScope.launch {
            val business = workspaceManager.currentWorkspace.value?.nombre ?: "Nuestro negocio"
            enviarMensajeUseCase.enviarConfirmacionAlquiler(
                telefono = alquiler.clienteTelefono,
                cliente = alquiler.clienteNombre,
                item = alquiler.itemNombre,
                fechaDevolucion = alquiler.fechaFinFormatted,
                monto = alquiler.precioFormateado,
                negocio = business
            ).collect { }
        }
    }

    // Control de Diálogos
    fun showClienteDialog() = _uiState.update { it.copy(showClienteDialog = true) }
    fun hideClienteDialog() = _uiState.update { it.copy(showClienteDialog = false) }
    fun showItemDialog() = _uiState.update { it.copy(showItemDialog = true) }
    fun hideItemDialog() = _uiState.update { it.copy(showItemDialog = false) }
    fun onObservacionesChange(v: String) = _uiState.update { it.copy(observaciones = v) }
    fun clearMessages() = _uiState.update { it.copy(error = null, successMessage = null) }
}

data class CreateAlquilerUiState(
    val clientes: List<Cliente> = emptyList(),
    val itemsTotales: List<Item> = emptyList(),
    val itemsDisponibles: List<Item> = emptyList(),
    val categorias: List<Categoria> = emptyList(),
    val selectedCliente: Cliente? = null,
    val selectedItem: Item? = null,
    val categoriaFiltro: Categoria? = null,
    val cantidad: String = "1",
    val fechaInicio: Date? = Date(),
    val fechaFin: Date? = null,
    val diasAlquiler: Int = 0,
    val precioUnitario: String = "0.0",
    val precioTotal: Double = 0.0,
    val adelanto: String = "0.0",
    val saldo: Double = 0.0,
    val observaciones: String = "",
    val showClienteDialog: Boolean = false,
    val showItemDialog: Boolean = false,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
