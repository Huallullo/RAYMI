package com.raymi.app.domain.usecase.auth

import com.raymi.app.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Caso de uso para cerrar sesión
 * Maneja la lógica de cierre de sesión del usuario
 */
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Ejecuta el cierre de sesión
     * @return Flow con el resultado de la operación
     */
    suspend operator fun invoke() = authRepository.logout()
}