package com.raymi.app.presentation.auth

import com.raymi.app.domain.usecase.auth.LoginUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LoginViewModelValidationTest {

    @Test
    fun login_con_campos_vacios_muestra_errores() {
        val viewModel = LoginViewModel(LoginUseCase(FakeAuthRepositoryNoOp()))

        viewModel.login()

        val state = viewModel.uiState.value
        assertEquals("El email es requerido", state.emailError)
        assertEquals("La contraseña es requerida", state.passwordError)
        assertFalse(state.isLoginSuccessful)
    }

    @Test
    fun togglePasswordVisibility_cambia_estado() {
        val viewModel = LoginViewModel(LoginUseCase(FakeAuthRepositoryNoOp()))

        val initial = viewModel.uiState.value.isPasswordVisible
        viewModel.togglePasswordVisibility()
        val after = viewModel.uiState.value.isPasswordVisible

        assertFalse(initial == after)
    }
}