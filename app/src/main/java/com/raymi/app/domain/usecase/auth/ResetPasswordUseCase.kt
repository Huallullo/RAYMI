package com.raymi.app.domain.usecase.auth

import com.raymi.app.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Caso de uso para solicitar recuperación de contraseña por email.
 */
class ResetPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String) = authRepository.resetPassword(email)
}