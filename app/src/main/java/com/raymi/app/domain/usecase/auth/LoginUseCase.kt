package com.raymi.app.domain.usecase.auth

import com.raymi.app.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Caso de uso para iniciar sesión
 * Maneja la lógica de negocio para la autenticación del usuario
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Ejecuta el inicio de sesión
     * @param email Email del usuario
     * @param password Contraseña del usuario
     * @return Flow con el resultado de la operación
     */
    suspend operator fun invoke(email: String, password: String) =
        authRepository.login(email, password)
}
