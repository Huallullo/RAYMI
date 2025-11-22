package com.raymi.app.presentation.historial

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel para el historial
 * Maneja el registro de actividades del sistema
 *
 * NOTA: Implementación básica de placeholder.
 */
@HiltViewModel
class HistorialViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(HistorialUiState())
    val uiState: StateFlow<HistorialUiState> = _uiState.asStateFlow()
}

/**
 * Estado UI para el historial
 */
data class HistorialUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)