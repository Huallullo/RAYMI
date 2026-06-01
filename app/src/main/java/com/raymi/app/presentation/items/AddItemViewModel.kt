package com.raymi.app.presentation.items

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.core.workspace.WorkspaceManager
import com.raymi.app.domain.model.Categoria
import com.raymi.app.domain.model.Item
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.usecase.categoria.GetCategoriasUseCase
import com.raymi.app.domain.usecase.item.AddItemUseCase
import com.raymi.app.data.remote.StorageDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel para registrar nuevos ítems en el inventario con soporte de imágenes.
 */
@HiltViewModel
class AddItemViewModel @Inject constructor(
    private val addItemUseCase: AddItemUseCase,
    private val getCategoriasUseCase: GetCategoriasUseCase,
    private val userPlanRepository: com.raymi.app.domain.repository.UserPlanRepository,
    private val authRepository: com.raymi.app.domain.repository.AuthRepository,
    private val analytics: com.google.firebase.analytics.FirebaseAnalytics,
    private val storageDataSource: StorageDataSource,
    private val workspaceManager: WorkspaceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddItemUiState())
    val uiState: StateFlow<AddItemUiState> = _uiState.asStateFlow()

    init {
        cargarCategorias()
        generarCodigoAutomatico()
    }

    private fun generarCodigoAutomatico() {
        onCodigoChange(com.raymi.app.core.utils.GeneradorCodigo.generarCodigoItem())
    }

    private fun cargarCategorias() {
        viewModelScope.launch {
            try {
                val workspaceId = workspaceManager.getWorkspaceId() ?: return@launch
                getCategoriasUseCase(workspaceId).collect { result ->
                    when (result) {
                        is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                        is Resource.Success -> {
                            _uiState.update { it.copy(categorias = result.data ?: emptyList(), isLoading = false) }
                        }
                        is Resource.Error -> {
                            _uiState.update { it.copy(isLoading = false, error = result.message) }
                        }
                    }
                }
            } catch (_: Exception) { }
        }
    }

    fun onNombreChange(nombre: String) = _uiState.update { it.copy(nombre = nombre) }
    fun onCodigoChange(codigo: String) = _uiState.update { it.copy(codigo = codigo) }
    fun onPrecioChange(precio: String) = _uiState.update { it.copy(precio = precio) }
    fun onCantidadChange(cantidad: Int) = _uiState.update { it.copy(cantidad = cantidad) }
    fun onCategoriaChange(categoria: Categoria) {
        _uiState.update { it.copy(categoriaSeleccionada = categoria) }
        
        // Cargar atributos predefinidos de la categoría (TAREA 11)
        val nuevosAtributos = mutableMapOf<String, String>()
        categoria.attributeTemplates.forEach { template ->
            nuevosAtributos[template] = ""
        }
        _uiState.update { it.copy(atributos = nuevosAtributos) }
    }
    fun onImageSelected(uri: Uri?) = _uiState.update { it.copy(selectedImageUri = uri) }
    
    fun onAtributoChange(clave: String, valor: String) {
        val nuevosAtributos = _uiState.value.atributos.toMutableMap()
        nuevosAtributos[clave] = valor
        _uiState.update { it.copy(atributos = nuevosAtributos) }
    }

    fun clearMessages() = _uiState.update { it.copy(error = null) }

    fun eliminarAtributo(clave: String) {
        val nuevosAtributos = _uiState.value.atributos.toMutableMap()
        nuevosAtributos.remove(clave)
        _uiState.update { it.copy(atributos = nuevosAtributos) }
    }

    fun guardarItem() {
        val state = _uiState.value
        val nombreLimpio = state.nombre.trim()
        val codigoLimpio = state.codigo.trim().uppercase()
        
        if (nombreLimpio.isBlank()) {
            _uiState.update { it.copy(error = "El nombre del producto es obligatorio") }
            return
        }
        if (codigoLimpio.isBlank()) {
            _uiState.update { it.copy(error = "El código/SKU es obligatorio") }
            return
        }
        if (state.categoriaSeleccionada == null) {
            _uiState.update { it.copy(error = "Selecciona una categoría") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val user = authRepository.getCurrentUser() ?: throw Exception("Usuario no autenticado")
                val workspaceId = workspaceManager.getWorkspaceId() ?: throw Exception("Negocio no identificado")
                
                // Verificar límites del plan
                val canAdd = userPlanRepository.canAddMoreItems(user.uid, workspaceId)
                if (!canAdd) {
                    _uiState.update { it.copy(isLoading = false, shouldNavigateToPlans = true) }
                    return@launch
                }

                var imageUrl: String? = null
                state.selectedImageUri?.let { uri ->
                    val path = "negocios/$workspaceId/items/${UUID.randomUUID()}.webp"
                    imageUrl = storageDataSource.uploadFile(path, uri)
                }

                val nuevoItem = Item(
                    workspaceId = workspaceId,
                    nombre = nombreLimpio,
                    codigo = codigoLimpio,
                    categoriaId = state.categoriaSeleccionada.id,
                    precio = state.precio.toDoubleOrNull() ?: 0.0,
                    cantidad = state.cantidad,
                    atributos = state.atributos.mapValues { it.value.trim() },
                    imagenUrl = imageUrl
                )

                addItemUseCase(nuevoItem).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            analytics.logEvent("item_creado", null)
                            _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                        }
                        is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }
}

data class AddItemUiState(
    val nombre: String = "",
    val codigo: String = "",
    val precio: String = "",
    val cantidad: Int = 1,
    val categorias: List<Categoria> = emptyList(),
    val categoriaSeleccionada: Categoria? = null,
    val atributos: Map<String, String> = emptyMap(),
    val selectedImageUri: Uri? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val shouldNavigateToPlans: Boolean = false
)
