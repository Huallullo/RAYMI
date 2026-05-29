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
import com.raymi.app.core.ads.AdManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CreateAlquilerViewModel @Inject constructor(
    private val getClientesUseCase: GetClientesUseCase,
    private val getItemsUseCase: GetItemsUseCase,
    private val getCategoriasUseCase: com.raymi.app.domain.usecase.categoria.GetCategoriasUseCase,
    private val createAlquilerUseCase: CreateAlquilerUseCase,
    private val enviarMensajeUseCase: EnviarMensajeUseCase,
    private val userPlanRepository: com.raymi.app.domain.repository.UserPlanRepository,
    private val auth: com.google.firebase.auth.FirebaseAuth,
    private val analytics: com.google.firebase.analytics.FirebaseAnalytics,
    private val connectivityObserver: com.raymi.app.core.utils.ConnectivityObserver,
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
                    launch {
                        getCategoriasUseCase(workspaceId).collect { result ->
                            if (result is Resource.Success) {
                                _uiState.update { it.copy(categorias = result.data ?: emptyList()) }
                            }
                        }
                    }
                    launch {
                        getClientesUseCase().collect { result ->
                            if (result is Resource.Success) {
                                _uiState.update { it.copy(clientes = result.data ?: emptyList()) }
                            }
                        }
                    }
                    launch {
                        getItemsUseCase(workspaceId).collect { result ->
                            if (result is Resource.Success) {
                                val data = result.data ?: emptyList()
                                _uiState.update { it.copy(itemsTotales = data, itemsDisponibles = aplicarFiltroCategoria(data, it.categoriaFiltro)) }
                                if (!preselectedItemId.isNullOrBlank() && _uiState.value.selectedItems.isEmpty()) {
                                    data.find { it.id == preselectedItemId }?.let { agregarItem(it) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun filtrarPorCategoria(categoria: Categoria?) {
        _uiState.update { it.copy(categoriaFiltro = categoria, itemsDisponibles = aplicarFiltroCategoria(it.itemsTotales, categoria)) }
    }

    private fun aplicarFiltroCategoria(items: List<Item>, categoria: Categoria?): List<Item> {
        val base = items.filter { it.estado == "DISPONIBLE" }
        return if (categoria == null) base else base.filter { it.categoriaId == categoria.id }
    }

    fun seleccionarCliente(cliente: Cliente) = _uiState.update { it.copy(selectedCliente = cliente, showClienteDialog = false) }

    fun agregarItem(item: Item, cantidad: Int = 1) {
        val dias = _uiState.value.diasAlquiler.coerceAtLeast(1)
        val nuevoItem = AlquilerItem(
            itemId = item.id,
            itemNombre = item.nombre,
            itemCodigo = item.codigo,
            cantidad = cantidad,
            precioUnitario = item.precio,
            subtotal = item.precio * cantidad * dias
        )
        _uiState.update { it.copy(selectedItems = it.selectedItems + nuevoItem, showItemDialog = false) }
        recalcularFinanzas()
    }

    fun removerItem(index: Int) {
        _uiState.update { it.copy(selectedItems = it.selectedItems.filterIndexed { i, _ -> i != index }) }
        recalcularFinanzas()
    }

    fun onAdelantoChange(monto: String) {
        _uiState.update { it.copy(adelanto = monto) }
        recalcularFinanzas()
    }

    fun onGarantiaChange(monto: String) {
        _uiState.update { it.copy(garantia = monto) }
        recalcularFinanzas()
    }

    fun setEstadoInicial(estado: EstadoAlquiler) = _uiState.update { it.copy(estadoInicial = estado) }

    private fun recalcularFinanzas() {
        _uiState.update { state ->
            val subtotalItems = state.selectedItems.sumOf { it.subtotal }
            val pagado = state.adelanto.toDoubleOrNull() ?: 0.0
            state.copy(precioTotal = subtotalItems, saldo = (subtotalItems - pagado).coerceAtLeast(0.0))
        }
    }

    fun setFechaInicio(date: Date) {
        _uiState.update { it.copy(fechaInicio = date) }
        actualizarDias()
    }

    fun setFechaFin(date: Date) {
        _uiState.update { it.copy(fechaFin = date) }
        actualizarDias()
    }

    private fun actualizarDias() {
        val inicio = _uiState.value.fechaInicio
        val fin = _uiState.value.fechaFin
        if (inicio != null && fin != null) {
            val diff = fin.time - inicio.time
            val dias = (diff / (1000 * 60 * 60 * 24)).toInt() + 1
            _uiState.update { state ->
                val updatedItems = state.selectedItems.map { it.copy(subtotal = it.precioUnitario * it.cantidad * dias) }
                state.copy(diasAlquiler = dias, selectedItems = updatedItems)
            }
            recalcularFinanzas()
        }
    }

    fun crearAlquiler() {
        val state = _uiState.value
        if (state.selectedCliente == null) { _uiState.update { it.copy(error = "Selecciona un cliente") }; return }
        if (state.selectedItems.isEmpty()) { _uiState.update { it.copy(error = "Agrega al menos un ítem") }; return }
        if (state.fechaFin == null) { _uiState.update { it.copy(error = "Indica fecha de devolución") }; return }

        viewModelScope.launch {
            if (!connectivityObserver.isConnected.value) {
                _uiState.update { it.copy(successMessage = "Sin internet - El alquiler se sincronizará al reconectar") }
            }

            val workspaceId = workspaceManager.getWorkspaceId() ?: return@launch
            val mainItem = state.selectedItems.first()
            val nuevoAlquiler = Alquiler(
                workspaceId = workspaceId,
                clienteId = state.selectedCliente.id,
                clienteNombre = state.selectedCliente.nombreCompleto,
                clienteDni = state.selectedCliente.dni,
                clienteTelefono = state.selectedCliente.telefono,
                itemId = mainItem.itemId,
                itemNombre = if (state.selectedItems.size > 1) "${mainItem.itemNombre} (+${state.selectedItems.size - 1})" else mainItem.itemNombre,
                itemCodigo = mainItem.itemCodigo,
                cantidad = state.selectedItems.sumOf { it.cantidad },
                items = state.selectedItems,
                fechaInicio = Timestamp(state.fechaInicio ?: Date()),
                fechaFinPrevista = Timestamp(state.fechaFin),
                precioUnitario = mainItem.precioUnitario,
                precioTotal = state.precioTotal,
                adelanto = state.adelanto.toDoubleOrNull() ?: 0.0,
                saldo = state.saldo,
                garantia = state.garantia.toDoubleOrNull() ?: 0.0,
                estado = state.estadoInicial,
                observaciones = state.observaciones.trim()
            )

            createAlquilerUseCase(nuevoAlquiler).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Resource.Success -> {
                        _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                        enviarConfirmacionWhatsApp(nuevoAlquiler)
                        
                        // Monetización: Mostrar Intersticial
                        verificarYMostrarAd()
                    }
                    is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    private fun enviarConfirmacionWhatsApp(alquiler: Alquiler) {
        viewModelScope.launch {
            val business = workspaceManager.currentWorkspace.value?.nombre ?: "Negocio"
            enviarMensajeUseCase.enviarConfirmacionAlquiler(
                alquiler.clienteTelefono, alquiler.clienteNombre, alquiler.itemNombre, 
                alquiler.fechaFinFormatted, alquiler.precioFormateado, business
            ).collect { }
        }
    }

    private fun verificarYMostrarAd() {
        viewModelScope.launch {
            auth.uid?.let { uid ->
                userPlanRepository.getUserPlan(uid).collect { result ->
                    if (result is Resource.Success) {
                        val plan = result.data
                        if (AdManager.debeMostrarAnuncios(plan)) {
                            _uiState.update { it.copy(shouldShowInterstitial = true) }
                        }
                    }
                }
            }
        }
    }

    fun onInterstitialShown() {
        _uiState.update { it.copy(shouldShowInterstitial = false) }
    }

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
    val selectedItems: List<AlquilerItem> = emptyList(),
    val categoriaFiltro: Categoria? = null,
    val fechaInicio: Date? = Date(),
    val fechaFin: Date? = null,
    val diasAlquiler: Int = 0,
    val precioTotal: Double = 0.0,
    val adelanto: String = "0.0",
    val garantia: String = "0.0",
    val saldo: Double = 0.0,
    val estadoInicial: EstadoAlquiler = EstadoAlquiler.ACTIVO,
    val observaciones: String = "",
    val showClienteDialog: Boolean = false,
    val showItemDialog: Boolean = false,
    val shouldShowInterstitial: Boolean = false,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
