package com.raymi.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.UserPlan
import com.raymi.app.domain.repository.AuthRepository
import com.raymi.app.domain.repository.UserPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para el Perfil de Usuario.
 * Maneja la información de la cuenta y el estatus del plan SaaS.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPlanRepository: UserPlanRepository,
    private val performFullAuditUseCase: com.raymi.app.domain.usecase.workspace.PerformFullAuditUseCase,
    private val workspaceManager: com.raymi.app.core.workspace.WorkspaceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        cargarDatosPerfil()
    }

    fun sincronizacionTotal() {
        val workspaceId = workspaceManager.getWorkspaceId() ?: return
        viewModelScope.launch {
            performFullAuditUseCase(workspaceId).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isSyncing = true) }
                    is Resource.Success -> _uiState.update { it.copy(isSyncing = false, successMessage = "Base de datos sincronizada y reparada") }
                    is Resource.Error -> _uiState.update { it.copy(isSyncing = false, error = result.message) }
                }
            }
        }
    }

    fun clearMessages() = _uiState.update { it.copy(error = null, successMessage = null) }

    private fun cargarDatosPerfil() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            if (user != null) {
                _uiState.update { it.copy(user = user) }
                
                // Cargar el plan del usuario
                userPlanRepository.getUserPlan(user.uid).collect { result ->
                    when (result) {
                        is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                        is Resource.Success -> _uiState.update { it.copy(plan = result.data, isLoading = false) }
                        is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                }
            } else {
                _uiState.update { it.copy(error = "No hay sesión activa") }
            }
        }
    }

    fun cerrarSesion() {
        viewModelScope.launch {
            authRepository.logout().collect { result ->
                if (result is Resource.Success) {
                    _uiState.update { it.copy(loggedOut = true) }
                }
            }
        }
    }
}

data class ProfileUiState(
    val user: FirebaseUser? = null,
    val plan: UserPlan? = null,
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val loggedOut: Boolean = false
)
