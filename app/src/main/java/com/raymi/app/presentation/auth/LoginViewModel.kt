package com.raymi.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.raymi.app.core.utils.Validators
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.AuthRepository
import com.raymi.app.domain.usecase.business.CheckBusinessConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val checkBusinessConfigUseCase: CheckBusinessConfigUseCase
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
            authRepository.register(
                email = _uiState.value.email.trim(),
                password = _uiState.value.password,
                businessName = _uiState.value.businessName.trim()
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
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoginSuccessful = true
                )
                // Verificar si el negocio ya está configurado
                verificarYNavigar()
            }
            is Resource.Error -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.message
                )
            }
        }
    }

    private fun verificarYNavigar() {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser() ?: return@launch
            val negocioId = authRepository.getCurrentBusinessId() // suponemos que existe esta función
            val isConfigured = checkBusinessConfigUseCase(negocioId)
            _navigationEvent.value = if (isConfigured) {
                NavigationEvent.GoToDashboard
            } else {
                NavigationEvent.GoToOnboarding
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
        val isLoginValid = validateLoginFields()
        val businessNameValidation = Validators.validateMinLength(
            value = _uiState.value.businessName.trim(),
            minLength = 2,
            fieldName = "Nombre del negocio"
        )

        _uiState.value = _uiState.value.copy(
            businessNameError = if (businessNameValidation.isValid) null else businessNameValidation.errorMessage
        )

        return isLoginValid && businessNameValidation.isValid
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
}