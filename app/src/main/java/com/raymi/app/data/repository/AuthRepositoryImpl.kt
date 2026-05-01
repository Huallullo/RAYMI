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

/**
 * Implementación del repositorio de autenticación
 * Maneja todas las operaciones relacionadas con Firebase Auth
 */
class AuthRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseDataSource
) : AuthRepository {
    companion object {
        private const val ADMIN_EMAIL = "raymi@gmail.com"
    }
    /**
     * Obtiene el usuario actual autenticado
     */
    override val currentUser: FirebaseUser?
        get() = dataSource.getCurrentUser()

    /**
     * Inicia sesión con email y contraseña
     * @param email Correo electrónico del usuario
     * @param password Contraseña del usuario
     * @return Flow con el resultado de la operación
     */
    override suspend fun login(
        email: String,
        password: String
    ): Flow<Resource<FirebaseUser>> = flow {
        try {
            // Emitir estado de carga
            emit(Resource.Loading())

            // Validar campos
            if (email.isBlank() || password.isBlank()) {
                emit(Resource.Error("Email y contraseña son requeridos"))
                return@flow
            }
            if (!email.equals(ADMIN_EMAIL, ignoreCase = true)) {
                emit(Resource.Error("Acceso denegado. Solo la cuenta admin autorizada puede ingresar."))
                return@flow
            }

            // Intentar iniciar sesión
            val result = dataSource.signIn(email, password)
            val user = result.user

            if (user != null) {
                if (!user.email.equals(ADMIN_EMAIL, ignoreCase = true)) {
                    dataSource.signOut()
                    emit(Resource.Error("Acceso denegado. Solo la cuenta admin autorizada puede ingresar."))
                    return@flow
                }
                if (!user.isEmailVerified) {
                    dataSource.signOut()
                    emit(Resource.Error("Debe verificar el correo admin antes de ingresar."))
                    return@flow
                }
                emit(Resource.Success(user))
            } else {
                emit(Resource.Error("Error al iniciar sesión"))
            }

        } catch (e: CancellationException) {
            throw e
        }catch (e: Exception) {
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
     * Registra un nuevo usuario
     * @param email Correo electrónico del usuario
     * @param password Contraseña del usuario
     * @return Flow con el resultado de la operación
     */
    override suspend fun register(
        email: String,
        password: String
    ): Flow<Resource<FirebaseUser>> = flow {
        try {
            // Emitir estado de carga
            emit(Resource.Loading())

            // Validar campos
            if (email.isBlank() || password.isBlank()) {
                emit(Resource.Error("Email y contraseña son requeridos"))
                return@flow
            }

            if (password.length < 6) {
                emit(Resource.Error("La contraseña debe tener al menos 6 caracteres"))
                return@flow
            }
            if (!email.equals(ADMIN_EMAIL, ignoreCase = true)) {
                emit(Resource.Error("Solo se permite registrar la cuenta admin autorizada."))
                return@flow
            }
            // Intentar registrar usuario
            val result = dataSource.signUp(email, password)
            val user = result.user

            if (user != null) {
                emit(Resource.Success(user))
            } else {
                emit(Resource.Error("Error al registrar usuario"))
            }

        } catch (e: CancellationException) {
            throw e
        }catch (e: Exception) {
            // Manejar errores específicos
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
     * Cierra la sesión actual
     * @return Flow con el resultado de la operación
     */
    override suspend fun logout(): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            dataSource.signOut()
            emit(Resource.Success(Unit))
        }catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error("Error al cerrar sesión: ${e.message}"))
        }
    }

    /**
     * Verifica si hay un usuario autenticado
     * @return true si hay un usuario autenticado, false en caso contrario
     */
    override fun isUserAuthenticated(): Boolean {
        return dataSource.isUserAuthenticated()
    }
}
