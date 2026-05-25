package com.raymi.app.presentation.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.Workspace
import com.raymi.app.domain.repository.AuthRepository
import com.raymi.app.domain.usecase.workspace.CreateWorkspaceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateWorkspaceViewModel @Inject constructor(
    private val createWorkspaceUseCase: CreateWorkspaceUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateWorkspaceUiState())
    val uiState: StateFlow<CreateWorkspaceUiState> = _uiState.asStateFlow()

    fun onNombreChange(nombre: String) {
        _uiState.value = _uiState.value.copy(nombre = nombre, error = null)
    }

    fun onDescripcionChange(descripcion: String) {
        _uiState.value = _uiState.value.copy(descripcion = descripcion)
    }

    fun onTipoNegocioChange(tipo: String) {
        _uiState.value = _uiState.value.copy(tipoNegocio = tipo)
    }

    /**
     * Intenta registrar el nuevo negocio en Firebase.
     */
    fun registrarNegocio() {
        val nombre = _uiState.value.nombre.trim()
        if (nombre.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "El nombre del negocio es obligatorio")
            return
        }

        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            if (user != null) {
                val nuevoWorkspace = Workspace(
                    ownerId = user.uid,
                    nombre = nombre,
                    descripcion = _uiState.value.descripcion.trim(),
                    tipoNegocio = _uiState.value.tipoNegocio
                )

                createWorkspaceUseCase(nuevoWorkspace).collect { result ->
                    when (result) {
                        is Resource.Loading -> {
                            _uiState.value = _uiState.value.copy(isLoading = true)
                        }
                        is Resource.Success -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isSuccess = true
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
            } else {
                _uiState.value = _uiState.value.copy(error = "Sesión no válida")
            }
        }
    }
}

/**
 * Estado de la interfaz para crear un negocio.
 */
data class CreateWorkspaceUiState(
    val nombre: String = "",
    val descripcion: String = "",
    val tipoNegocio: String = "VESTUARIOS", // Por defecto el rubro actual
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)
