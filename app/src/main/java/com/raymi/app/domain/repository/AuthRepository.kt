package com.raymi.app.domain.repository

import com.google.firebase.auth.FirebaseUser
import com.raymi.app.domain.model.Resource
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    /**
     * Obtiene el usuario actual autenticado (suspending, puede ser null)
     */
    suspend fun getCurrentUser(): FirebaseUser?

    /**
     * Obtiene el ID del negocio al que pertenece el usuario actual.
     * Lanza excepción si no hay usuario autenticado o no tiene negocio asignado.
     */
    suspend fun getCurrentBusinessId(): String

    /**
     * Inicia sesión con email y contraseña
     */
    suspend fun login(email: String, password: String): Flow<Resource<FirebaseUser>>

    /**
     * Registra un nuevo usuario y crea su negocio SaaS inicial
     * @param email Correo electrónico
     * @param password Contraseña
     * @param businessName Nombre del negocio
     */
    suspend fun register(
        email: String,
        password: String,
        businessName: String
    ): Flow<Resource<FirebaseUser>>

    /**
     * Envía un correo de restablecimiento de contraseña
     */
    suspend fun resetPassword(email: String): Flow<Resource<Unit>>

    /**
     * Cierra la sesión actual
     */
    suspend fun logout(): Flow<Resource<Unit>>

    /**
     * Verifica si un correo electrónico existe en el sistema.
     * [B-10] Validación previa a recuperación de contraseña.
     */
    suspend fun checkEmailExists(email: String): Boolean

    /**
     * Actualiza el perfil del usuario (nombre y opcionalmente teléfono)
     */
    suspend fun updateProfile(name: String, phone: String?): Flow<Resource<Unit>>

    /**
     * Actualiza la contraseña del usuario actual
     */
    suspend fun changePassword(newPassword: String): Flow<Resource<Unit>>

    /**
     * Actualiza el token de notificaciones push para el usuario actual
     */
    suspend fun updateFcmToken(token: String): Flow<Resource<Unit>>

    /**
     * Verifica si hay un usuario autenticado (síncrono, no suspendido)
     */
    fun isUserAuthenticated(): Boolean
}