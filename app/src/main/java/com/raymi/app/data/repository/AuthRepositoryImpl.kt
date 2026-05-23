package com.raymi.app.data.repository

import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.raymi.app.core.utils.AppLogger
import com.raymi.app.data.remote.FirebaseDataSource
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class AuthRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseDataSource
) : AuthRepository {

    /**
     * Propiedad no suspendida que devuelve el usuario actual (puede ser null)
     */
    override val currentUser: FirebaseUser?
        get() = dataSource.getCurrentUser()

    /**
     * Versión suspendida de obtener el usuario actual
     */
    override suspend fun getCurrentUser(): FirebaseUser? = currentUser

    /**
     * Obtiene el ID del negocio actual (obliga a crear/obtener el negocio)
     */
    override suspend fun getCurrentBusinessId(): String =
        dataSource.getCurrentBusinessId()

    /**
     * Inicia sesión con email y contraseña
     */
    override suspend fun login(
        email: String,
        password: String
    ): Flow<Resource<FirebaseUser>> = flow {
        try {
            emit(Resource.Loading())

            if (email.isBlank() || password.isBlank()) {
                emit(Resource.Error("Email y contraseña son requeridos"))
                return@flow
            }

            val result = dataSource.signIn(email, password)
            val user = result.user

            if (user != null) {
                // Asegura que existe un perfil de negocio (crea si no existe)
                dataSource.ensureBusinessProfileForUser(user)
                emit(Resource.Success(user))
            } else {
                emit(Resource.Error("Error al iniciar sesión"))
            }

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val errorMessage = when (e) {
                is FirebaseAuthException -> when (e.errorCode) {
                    "ERROR_INVALID_EMAIL" -> "Email inválido"
                    "ERROR_USER_NOT_FOUND" -> "Usuario no encontrado"
                    "ERROR_WRONG_PASSWORD" -> "Contraseña incorrecta"
                    "ERROR_NETWORK_REQUEST_FAILED" -> "Error de conexión. Verifica tu internet"
                    else -> "Error de autenticación: ${e.localizedMessage}"
                }
                else -> "Error al iniciar sesión: ${e.localizedMessage}"
            }
            AppLogger.e(
                tag = "AuthRepository",
                message = "Fallo en login para email=$email",
                throwable = e
            )
            emit(Resource.Error(errorMessage))
        }
    }

    /**
     * Registra un nuevo usuario y crea su negocio SaaS Inicial
     */
    override suspend fun register(
        email: String,
        password: String,
        businessName: String
    ): Flow<Resource<FirebaseUser>> = flow {
        try {
            emit(Resource.Loading())

            if (email.isBlank() || password.isBlank() || businessName.isBlank()) {
                emit(Resource.Error("Email, contraseña y nombre del negocio son requeridos"))
                return@flow
            }

            if (password.length < 6) {
                emit(Resource.Error("La contraseña debe tener al menos 6 caracteres"))
                return@flow
            }

            val result = dataSource.signUp(email, password)
            val user = result.user

            if (user != null) {
                dataSource.createBusinessProfileForUser(user, businessName)
                emit(Resource.Success(user))
            } else {
                emit(Resource.Error("Error al registrar usuario"))
            }

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("already in use") == true ->
                    "Este email ya está registrado"
                e.message?.contains("invalid email") == true ->
                    "Email inválido"
                e.message?.contains("network") == true ->
                    "Error de conexión. Verifica tu internet"
                else ->
                    "Error al registrar: ${e.message}"
            }
            emit(Resource.Error(errorMessage))
        }
    }

    /**
     * Envía correo de recuperación de contraseña
     */
    override suspend fun resetPassword(email: String): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())

            if (email.isBlank()) {
                emit(Resource.Error("Ingresa tu email para recuperar la contraseña"))
                return@flow
            }

            dataSource.sendPasswordResetEmail(email.trim())
            emit(Resource.Success(Unit))

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val errorMessage = when (e) {
                is FirebaseAuthException -> when (e.errorCode) {
                    "ERROR_INVALID_EMAIL" -> "Email inválido"
                    "ERROR_USER_NOT_FOUND" -> "Usuario no encontrado"
                    "ERROR_NETWORK_REQUEST_FAILED" -> "Error de conexión. Verifica tu internet"
                    else -> "Error al enviar recuperación: ${e.localizedMessage}"
                }
                else -> "Error al enviar recuperación: ${e.localizedMessage}"
            }
            emit(Resource.Error(errorMessage))
        }
    }

    /**
     * Cierra la sesión actual
     */
    override suspend fun logout(): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            dataSource.signOut()
            emit(Resource.Success(Unit))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error("Error al cerrar sesión: ${e.message}"))
        }
    }

    /**
     * Verifica si hay un usuario autenticado (no suspendida)
     */
    override fun isUserAuthenticated(): Boolean = dataSource.isUserAuthenticated()
}