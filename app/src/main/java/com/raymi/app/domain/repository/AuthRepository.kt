package com.raymi.app.domain.repository

import com.google.firebase.auth.FirebaseUser
import com.raymi.app.domain.model.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Interfaz del repositorio de autenticación
 */
interface AuthRepository {

    /**
     * Obtiene el usuario actual
     */
    val currentUser: FirebaseUser?

    /**
     * Inicia sesión con email y contraseña
     */
    suspend fun login(email: String, password: String): Flow<Resource<FirebaseUser>>

    /**
     * Registra un nuevo usuario
     */
    suspend fun register(email: String, password: String): Flow<Resource<FirebaseUser>>

    /**
     * Cierra la sesión actual
     */
    suspend fun logout(): Flow<Resource<Unit>>

    /**
     * Verifica si hay un usuario autenticado
     */
    fun isUserAuthenticated(): Boolean
}
