// ========== AlquilerDetailViewModel.kt ==========
package com.raymi.app.presentation.alquileres

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.usecase.alquiler.GetAlquilerByIdUseCase
import com.raymi.app.domain.usecase.alquiler.RegistrarDevolucionUseCase
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
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val alquilerId: String = savedStateHandle["alquilerId"] ?: ""

    private val _uiState = MutableStateFlow(AlquilerDetailUiState())
    val uiState: StateFlow<AlquilerDetailUiState> = _uiState.asStateFlow()

    init {
        loadAlquiler()
    }

    private fun loadAlquiler() {
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
    val successMessage: String? = null
)