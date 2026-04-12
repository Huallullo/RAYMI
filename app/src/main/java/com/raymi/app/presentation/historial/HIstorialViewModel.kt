package com.raymi.app.presentation.historial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.usecase.alquiler.GetAlquileresUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistorialViewModel @Inject constructor(
    private val getAlquileresUseCase: GetAlquileresUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistorialUiState())
    val uiState: StateFlow<HistorialUiState> = _uiState.asStateFlow()

    init {
        loadHistorial()
    }

    fun loadHistorial() {
        viewModelScope.launch {
            getAlquileresUseCase().collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                    is Resource.Success -> {
                        // Mostrar solo alquileres completados (devueltos o cancelados)
                        val alquileresCompletados = result.data?.filter {
                            it.estado.name == "DEVUELTO" || it.estado.name == "CANCELADO"
                        }?.sortedByDescending { it.updatedAt } ?: emptyList()

                        _uiState.value = _uiState.value.copy(
                            alquileres = alquileresCompletados,
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
}

data class HistorialUiState(
    val alquileres: List<Alquiler> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)