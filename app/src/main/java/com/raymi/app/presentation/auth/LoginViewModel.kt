package com.raymi.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.raymi.app.core.utils.Validators
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.AuthRepository
import com.raymi.app.core.workspace.WorkspaceManager
import com.raymi.app.domain.usecase.workspace.GetCurrentWorkspaceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val getCurrentWorkspaceUseCase: GetCurrentWorkspaceUseCase,
    private val workspaceManager: WorkspaceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    // Acciones de UI
    fun onEmailChange(email: String) {
        val sanitized = email.lowercase().filter { !it.isWhitespace() }
        _uiState.value = _uiState.value.copy(
            email = sanitized,
            emailError = null
        )
    }

    fun onPasswordChange(password: String) {
        val sanitized = password.filter { !it.isWhitespace() }
        _uiState.value = _uiState.value.copy(
            password = sanitized,
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
            businessNameError = null,
            // Limpiar protección contra bots
            botAnswer = "",
            isBotVerified = false,
            showBotMath = false
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
            val email = _uiState.value.email.trim()
            _uiState.update { it.copy(isLoading = true, error = null) }

            // ✅ UX FIX: Eliminamos el bloqueo preventivo en el Login.
            // Dejamos que Firebase Auth valide el correo y la contraseña directamente.
            // Esto evita "Falsos Negativos" como el que estás experimentando.

            authRepository.login(
                email = email,
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

    fun resetPassword(email: String) {
        val emailValidation = Validators.validateEmail(email.trim(), isRequired = true)
        if (!emailValidation.isValid) {
            _uiState.value = _uiState.value.copy(error = emailValidation.errorMessage)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            authRepository.resetPassword(email.trim()).collect { result ->
                when (result) {
                    is Resource.Loading -> { }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            infoMessage = "Si el correo está registrado, recibirás un enlace en breve.",
                            error = null,
                            showForgotPasswordDialog = false
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

    fun showForgotPassword(show: Boolean) {
        _uiState.value = _uiState.value.copy(showForgotPasswordDialog = show)
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
                    businessNameError = null,
                    isLoginSuccessful = true
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
                                _navigationEvent.emit(NavigationEvent.GoToDashboard)
                            } else {
                                // No tiene negocio aún
                                _uiState.value = _uiState.value.copy(isLoading = false)
                                _navigationEvent.emit(NavigationEvent.GoToWorkspaceSelection)
                            }
                        } else if (result is Resource.Error) {
                            _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                        }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Usuario no autenticado") }
                }
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                _navigationEvent.emit(NavigationEvent.GoToWorkspaceSelection)
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

    fun setLanguage(lang: String) {
        workspaceManager.setLanguage(lang)
    }

    fun onBotAnswerChange(answer: String) {
        _uiState.value = _uiState.value.copy(botAnswer = answer)
    }

    fun verifyBotAnswer() {
        val state = _uiState.value
        val expected = when (state.botOp) {
            "+" -> state.botNum1 + state.botNum2
            "-" -> state.botNum1 - state.botNum2
            "x" -> state.botNum1 * state.botNum2
            else -> 0
        }
        
        if (state.botAnswer.toIntOrNull() == expected) {
            _uiState.value = _uiState.value.copy(
                isBotVerified = true,
                showBotMath = false
            )
        } else {
            // Error simple: refrescar reto para evitar fuerza bruta
            refreshBotChallenge()
        }
    }

    fun refreshBotChallenge() {
        val op = listOf("+", "-", "x").random()
        val n1 = if (op == "x") (1..10).random() else (5..20).random()
        val n2 = if (op == "x") (1..5).random() else (1..n1).random()
        
        _uiState.value = _uiState.value.copy(
            botNum1 = n1,
            botNum2 = n2,
            botOp = op,
            botAnswer = "",
            isBotVerified = false
        )
    }

    fun onRobotCheckboxClick() {
        if (!_uiState.value.isBotVerified) {
            _uiState.value = _uiState.value.copy(showBotMath = true)
        }
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
    val isLoginSuccessful: Boolean = false,
    
    // Bot Protection
    val botNum1: Int = (1..15).random(),
    val botNum2: Int = (1..10).random(),
    val botOp: String = listOf("+", "-", "x").random(),
    val botAnswer: String = "",
    val isBotVerified: Boolean = false,
    val showBotMath: Boolean = false,
    val showForgotPasswordDialog: Boolean = false
)

sealed class NavigationEvent {
    object GoToDashboard : NavigationEvent()
    object GoToOnboarding : NavigationEvent()
    object GoToWorkspaceSelection : NavigationEvent()
}
