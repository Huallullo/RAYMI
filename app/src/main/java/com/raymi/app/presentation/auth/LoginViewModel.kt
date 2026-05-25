package com.raymi.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.raymi.app.core.utils.Validators
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.AuthRepository
import com.raymi.app.domain.usecase.business.CheckBusinessConfigUseCase
import com.raymi.app.core.workspace.WorkspaceManager
import com.raymi.app.domain.usecase.workspace.GetCurrentWorkspaceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val checkBusinessConfigUseCase: CheckBusinessConfigUseCase,
    private val getCurrentWorkspaceUseCase: GetCurrentWorkspaceUseCase,
    private val workspaceManager: WorkspaceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableStateFlow<NavigationEvent?>(null)
    val navigationEvent: StateFlow<NavigationEvent?> = _navigationEvent.asStateFlow()

    // Acciones de UI
    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(
            email = email,
            emailError = null
        )
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(
            password = password,
            passwordError = null
        )
    }

    fun onBusinessNameChange(businessName: String) {
        _uiState.value = _uiState.value.copy(
            businessName = businessName,
            businessNameError = null
        )
    }

    fun toggleAuthMode() {
        _uiState.value = _uiState.value.copy(
            isRegisterMode = !_uiState.value.isRegisterMode,
            email = "", // Limpiar campos al cambiar de modo
            password = "",
            businessName = "",
            error = null,
            infoMessage = null,
            emailError = null,
            passwordError = null,
            businessNameError = null
        )
    }

    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            isPasswordVisible = !_uiState.value.isPasswordVisible
        )
    }

    fun login() {
        if (!validateLoginFields()) return

        viewModelScope.launch {
            authRepository.login(
                email = _uiState.value.email.trim(),
                password = _uiState.value.password
            ).collect { result ->
                handleAuthResult(result)
            }
        }
    }

    fun register() {
        if (!validateRegisterFields()) return

        viewModelScope.launch {
            val email = _uiState.value.email.trim()
            val defaultBusinessName = email.substringBefore("@").replaceFirstChar { it.uppercase() } + " Business"
            
            authRepository.register(
                email = email,
                password = _uiState.value.password,
                businessName = _uiState.value.businessName.trim().ifBlank { defaultBusinessName }
            ).collect { result ->
                handleAuthResult(result)
            }
        }
    }

    fun resetPassword() {
        val emailTrim = _uiState.value.email.trim()
        val emailValidation = Validators.validateEmail(emailTrim, isRequired = true)
        _uiState.value = _uiState.value.copy(
            emailError = if (emailValidation.isValid) null else emailValidation.errorMessage
        )

        if (!emailValidation.isValid) return

        viewModelScope.launch {
            authRepository.resetPassword(emailTrim).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = true,
                            error = null,
                            infoMessage = null
                        )
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            infoMessage = "Te enviamos un correo para recuperar tu contraseña",
                            error = null
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

    private fun handleAuthResult(result: Resource<FirebaseUser>) {
        when (result) {
            is Resource.Loading -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null,
                    infoMessage = null
                )
            }
            is Resource.Success -> {
                // Limpiar campos al tener éxito
                _uiState.value = _uiState.value.copy(
                    email = "",
                    password = "",
                    businessName = "",
                    emailError = null,
                    passwordError = null,
                    businessNameError = null
                )
                // El login/registro fue exitoso en Auth y Firestore inicial
                verificarConfiguracionYNavigar()
            }
            is Resource.Error -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.message
                )
            }
        }
    }

    private fun verificarConfiguracionYNavigar() {
        viewModelScope.launch {
            try {
                val user = authRepository.getCurrentUser()
                if (user != null) {
                    getCurrentWorkspaceUseCase(user.uid).collect { result ->
                        if (result is Resource.Success) {
                            val workspace = result.data
                            if (workspace != null) {
                                workspaceManager.setWorkspace(workspace)
                                _uiState.value = _uiState.value.copy(isLoading = false)
                                _navigationEvent.value = NavigationEvent.GoToDashboard
                            } else {
                                // No tiene negocio aún
                                _uiState.value = _uiState.value.copy(isLoading = false)
                                _navigationEvent.value = NavigationEvent.GoToWorkspaceSelection
                            }
                        } else if (result is Resource.Error) {
                            _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                        }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Usuario no autenticado") }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                _navigationEvent.value = NavigationEvent.GoToWorkspaceSelection
            }
        }
    }

    private fun validateLoginFields(): Boolean {
        val emailTrim = _uiState.value.email.trim()
        val passwordTrim = _uiState.value.password

        val emailValidation = Validators.validateEmail(emailTrim, isRequired = true)
        val passwordValidation = Validators.validateMinLength(
            value = passwordTrim,
            minLength = 6,
            fieldName = "Contraseña"
        )

        _uiState.value = _uiState.value.copy(
            emailError = if (emailValidation.isValid) null else emailValidation.errorMessage,
            passwordError = if (passwordValidation.isValid) null else passwordValidation.errorMessage
        )

        return emailValidation.isValid && passwordValidation.isValid
    }

    private fun validateRegisterFields(): Boolean {
        return validateLoginFields()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearInfoMessage() {
        _uiState.value = _uiState.value.copy(infoMessage = null)
    }

    fun clearNavigationEvent() {
        _navigationEvent.value = null
    }
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val businessName: String = "",
    val isRegisterMode: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val businessNameError: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val infoMessage: String? = null,
    val isLoginSuccessful: Boolean = false
)

sealed class NavigationEvent {
    object GoToDashboard : NavigationEvent()
    object GoToOnboarding : NavigationEvent()
    object GoToWorkspaceSelection : NavigationEvent()
}
