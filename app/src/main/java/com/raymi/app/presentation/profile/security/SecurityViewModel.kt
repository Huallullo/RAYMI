package com.raymi.app.presentation.profile.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.core.utils.Validators
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecurityUiState())
    val uiState = _uiState.asStateFlow()

    fun onNewPasswordChange(v: String) {
        val sanitized = v.filter { !it.isWhitespace() }
        _uiState.update { it.copy(newPassword = sanitized, error = null) }
    }

    fun onConfirmPasswordChange(v: String) {
        val sanitized = v.filter { !it.isWhitespace() }
        _uiState.update { it.copy(confirmPassword = sanitized, error = null) }
    }

    fun changePassword() {
        val state = _uiState.value
        
        // Validaciones
        val passwordValidation = Validators.validatePassword(state.newPassword)
        if (!passwordValidation.isValid) {
            _uiState.update { it.copy(error = passwordValidation.errorMessage) }
            return
        }

        if (state.newPassword != state.confirmPassword) {
            _uiState.update { it.copy(error = "Las contraseñas no coinciden") }
            return
        }

        viewModelScope.launch {
            authRepository.changePassword(state.newPassword).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Resource.Success -> _uiState.update { 
                        it.copy(isLoading = false, isSuccess = true, newPassword = "", confirmPassword = "") 
                    }
                    is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, isSuccess = false) }
    }
}

data class SecurityUiState(
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)
