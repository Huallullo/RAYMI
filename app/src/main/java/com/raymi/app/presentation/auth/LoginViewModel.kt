package com.raymi.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.core.utils.Validators
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.usecase.auth.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la pantalla de inicio de sesión
 * Maneja la lógica de autenticación del usuario
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    // ========== ESTADOS UI ==========

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // ========== ACCIONES ==========

    /**
     * Actualiza el email
     */
    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(
            email = email,
            emailError = null
        )
    }

    /**
     * Actualiza la contraseña
     */
    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(
            password = password,
            passwordError = null
        )
    }

    /**
     * Alterna la visibilidad de la contraseña
     */
    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            isPasswordVisible = !_uiState.value.isPasswordVisible
        )
    }

    /**
     * Intenta iniciar sesión
     */
    fun login() {
        // Validar campos
        if (!validateFields()) {
            return
        }

        viewModelScope.launch {
            loginUseCase(
                email = _uiState.value.email.trim(),
                password = _uiState.value.password
            ).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = true,
                            error = null
                        )
                    }

                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoginSuccessful = true,
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

    /**
     * Valida los campos del formulario
     */
    private fun validateFields(): Boolean {
        val emailTrim = _uiState.value.email.trim()
        val passwordTrim = _uiState.value.password

        val emailValidation = Validators.validateEmail(emailTrim, isRequired = true)
        val passwordValidation = Validators.validatePassword(passwordTrim)

        _uiState.value = _uiState.value.copy(
            emailError = if (emailValidation.isValid) null else emailValidation.errorMessage,
            passwordError = if (passwordValidation.isValid) null else passwordValidation.errorMessage
        )

        return emailValidation.isValid && passwordValidation.isValid
    }

    /**
     * Limpia el error general
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * Estado UI para la pantalla de login
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoginSuccessful: Boolean = false
)
