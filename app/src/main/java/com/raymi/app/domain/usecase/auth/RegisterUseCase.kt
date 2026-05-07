package com.raymi.app.domain.usecase.auth

import com.raymi.app.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Caso de uso para registrar una cuenta y crear el negocio inicial del usuario.
 */
class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String, businessName: String) =
        authRepository.register(email, password, businessName)
}