package com.raymi.app.presentation.clientes

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.data.remote.StorageDataSource
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.Cliente
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.usecase.cliente.GetClienteByIdUseCase
import com.raymi.app.domain.usecase.cliente.UpdateClienteUseCase
import com.raymi.app.domain.usecase.cliente.DeleteClienteUseCase
import com.raymi.app.domain.usecase.alquiler.GetAlquileresByClienteUseCase
import com.raymi.app.core.workspace.WorkspaceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClienteDetailViewModel @Inject constructor(
    private val getClienteByIdUseCase: GetClienteByIdUseCase,
    private val updateClienteUseCase: UpdateClienteUseCase,
    private val deleteClienteUseCase: DeleteClienteUseCase,
    private val getAlquileresByClienteUseCase: GetAlquileresByClienteUseCase,
    private val storageDataSource: StorageDataSource,
    private val workspaceManager: WorkspaceManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val clienteId: String = savedStateHandle["clienteId"] ?: ""

    private val _uiState = MutableStateFlow(ClienteDetailUiState())
    val uiState: StateFlow<ClienteDetailUiState> = _uiState.asStateFlow()

    init {
        loadClienteData()
    }

    fun loadClienteData() {
        viewModelScope.launch {
            getClienteByIdUseCase(clienteId).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Resource.Success -> {
                        _uiState.update { it.copy(cliente = result.data, isLoading = false) }
                        loadAlquileres()
                    }
                    is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    private fun loadAlquileres() {
        viewModelScope.launch {
            val workspaceId = workspaceManager.getWorkspaceId() ?: return@launch
            getAlquileresByClienteUseCase(workspaceId, clienteId).collect { result ->
                if (result is Resource.Success) {
                    val alquileres = result.data ?: emptyList()
                    _uiState.update { it.copy(
                        alquileres = alquileres,
                        totalAlquileres = alquileres.size,
                        totalGastado = alquileres.sumOf { a -> a.precioTotal }
                    ) }
                }
            }
        }
    }

    /**
     * Actualiza los datos del cliente, incluyendo posibles nuevas fotos de identidad.
     */
    fun updateCliente(
        cliente: Cliente,
        dniFront: Uri? = null,
        dniBack: Uri? = null,
        face: Uri? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val workspaceId = workspaceManager.getWorkspaceId() ?: throw Exception("Negocio no identificado")
                var finalCliente = cliente

                // Actualizar fotos si se seleccionaron nuevas
                dniFront?.let {
                    _uiState.value.cliente?.fotoDniFrontUrl?.let { old -> 
                        val path = storageDataSource.getPathFromUrl(old)
                        if (!path.isNullOrBlank()) storageDataSource.deleteFile(path) 
                    }
                    val url = storageDataSource.uploadFile("negocios/$workspaceId/clientes/${cliente.dni}_front.webp", it)
                    finalCliente = finalCliente.copy(fotoDniFrontUrl = url)
                }
                dniBack?.let {
                    _uiState.value.cliente?.fotoDniBackUrl?.let { old -> 
                        val path = storageDataSource.getPathFromUrl(old)
                        if (!path.isNullOrBlank()) storageDataSource.deleteFile(path) 
                    }
                    val url = storageDataSource.uploadFile("negocios/$workspaceId/clientes/${cliente.dni}_back.webp", it)
                    finalCliente = finalCliente.copy(fotoDniBackUrl = url)
                }
                face?.let {
                    _uiState.value.cliente?.fotoRostroUrl?.let { old -> 
                        val path = storageDataSource.getPathFromUrl(old)
                        if (!path.isNullOrBlank()) storageDataSource.deleteFile(path) 
                    }
                    val url = storageDataSource.uploadFile("negocios/$workspaceId/clientes/${cliente.dni}_face.webp", it)
                    finalCliente = finalCliente.copy(fotoRostroUrl = url)
                }

                updateClienteUseCase(finalCliente).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            _uiState.update { it.copy(isLoading = false, successMessage = "Cliente actualizado con éxito") }
                            loadClienteData()
                        }
                        is Resource.Error -> {
                            _uiState.update { it.copy(isLoading = false, error = result.message) }
                        }
                        is Resource.Loading -> {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ClienteDetail", "Fallo al actualizar", e)
                _uiState.update { it.copy(isLoading = false, error = "Fallo al actualizar: ${e.localizedMessage}") }
            }
        }
    }

    fun eliminarCliente(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Borrar fotos del Storage
            _uiState.value.cliente?.let { c ->
                listOfNotNull(c.fotoDniFrontUrl, c.fotoDniBackUrl, c.fotoRostroUrl).forEach { url ->
                    storageDataSource.getPathFromUrl(url)?.let { path -> storageDataSource.deleteFile(path) }
                }
            }

            deleteClienteUseCase(clienteId).collect { result ->
                if (result is Resource.Success) {
                    _uiState.update { it.copy(isLoading = false, successMessage = "Cliente eliminado") }
                    onSuccess()
                } else if (result is Resource.Error) {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun clearMessages() = _uiState.update { it.copy(error = null, successMessage = null) }
}

data class ClienteDetailUiState(
    val cliente: Cliente? = null,
    val alquileres: List<Alquiler> = emptyList(),
    val totalAlquileres: Int = 0,
    val totalGastado: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
