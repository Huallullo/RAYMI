package com.raymi.app.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState

    fun guardarConfiguracion(
        rubro: String,
        tipoActivoSingular: String,
        tipoActivoPlural: String,
        atributos: List<AtributoPersonalizado>
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // Aquí llamaremos a un caso de uso que actualizará el documento del negocio
            // con la nueva configuración.
            // Por ahora, solo simulamos éxito
            kotlinx.coroutines.delay(1000)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isComplete = true
            )
        }
    }
}

data class OnboardingUiState(
    val isLoading: Boolean = false,
    val isComplete: Boolean = false,
    val error: String? = null
)