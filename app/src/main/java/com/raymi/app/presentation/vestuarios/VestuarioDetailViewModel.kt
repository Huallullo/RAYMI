// ========== VestuarioDetailViewModel.kt ==========
package com.raymi.app.presentation.vestuarios

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.Vestuario
import com.raymi.app.domain.usecase.alquiler.GetAlquileresUseCase
import com.raymi.app.domain.usecase.vestuario.GetVestuarioByIdUseCase
import com.raymi.app.domain.usecase.vestuario.UpdateVestuarioUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VestuarioDetailViewModel @Inject constructor(
    private val getVestuarioByIdUseCase: GetVestuarioByIdUseCase,
    private val getAlquileresUseCase: GetAlquileresUseCase,
    private val updateVestuarioUseCase: UpdateVestuarioUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val vestuarioId: String = savedStateHandle["vestuarioId"] ?: ""

    private val _uiState = MutableStateFlow(VestuarioDetailUiState())
    val uiState: StateFlow<VestuarioDetailUiState> = _uiState.asStateFlow()

    init {
        loadVestuarioData()
    }

    fun loadVestuarioData() {
        viewModelScope.launch {
            // Cargar datos del vestuario
            getVestuarioByIdUseCase(vestuarioId).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            vestuario = result.data,
                            isLoading = false
                        )
                        // Cargar historial de alquileres
                        loadHistorial()
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

    private fun loadHistorial() {
        viewModelScope.launch {
            getAlquileresUseCase().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val alquileres = result.data?.filter {
                            it.itemId == vestuarioId
                        } ?: emptyList()

                        val totalAlquileres = alquileres.size
                        val totalIngresos = alquileres.sumOf { it.precioTotal }

                        _uiState.value = _uiState.value.copy(
                            historialAlquileres = alquileres,
                            totalAlquileres = totalAlquileres,
                            totalIngresos = totalIngresos
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = result.message
                        )
                    }
                    is Resource.Loading -> {}
                }
            }
        }
    }
    // Agregar método:
    fun updateVestuario(vestuario: Vestuario) {
        viewModelScope.launch {
            updateVestuarioUseCase(vestuario).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            successMessage = "Vestuario actualizado correctamente"
                        )
                        loadVestuarioData()
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

data class VestuarioDetailUiState(
    val vestuario: Vestuario? = null,
    val historialAlquileres: List<Alquiler> = emptyList(),
    val totalAlquileres: Int = 0,
    val totalIngresos: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null  // ✅ AGREGADO
)
