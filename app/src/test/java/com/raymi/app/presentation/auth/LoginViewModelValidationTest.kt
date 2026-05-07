package com.raymi.app.presentation.auth

import com.raymi.app.domain.usecase.auth.LoginUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelValidationTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        // Configura el Main dispatcher para que use nuestro dispatcher de prueba
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        // Restaura el Main dispatcher original después de las pruebas
        Dispatchers.resetMain()
    }

    private fun createViewModel(): LoginViewModel {
        val repository = FakeAuthRepositoryNoOp()
        return LoginViewModel(
            loginUseCase = LoginUseCase(repository),
            authRepository = repository
        )
    }

    @Test
    fun login_con_credenciales_correctas_esExitoso() {
        val viewModel = createViewModel()

        viewModel.onEmailChange("admin@raymi.com")
        viewModel.onPasswordChange("admin123")
        viewModel.login()

        // Avanzar el dispatcher para que se ejecuten las corutinas lanzadas
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isLoginSuccessful)
        assertNull(state.emailError)
        assertNull(state.passwordError)
    }

    @Test
    fun login_con_credenciales_incorrectas_muestraError() {
        val viewModel = createViewModel()

        viewModel.onEmailChange("admin@raymi.com")
        viewModel.onPasswordChange("claveIncorrecta")
        viewModel.login()

        // Avanzar el dispatcher para resolver las corutinas pendientes
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoginSuccessful)
        assertEquals("Credenciales inválidas", state.error)
    }
}