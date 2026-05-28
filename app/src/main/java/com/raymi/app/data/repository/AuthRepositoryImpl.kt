package com.raymi.app.data.repository

import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.raymi.app.core.utils.AppLogger
import com.raymi.app.core.utils.Constants.COLLECTION_USUARIOS
import com.raymi.app.data.remote.AuthDataSource
import com.raymi.app.data.remote.FirebaseDataSource
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class AuthRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseDataSource,
    private val authDataSource: AuthDataSource
) : AuthRepository {

    /**
     * Versión suspendida de obtener el usuario actual
     */
    override suspend fun getCurrentUser(): FirebaseUser? = authDataSource.getCurrentUser()

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

            val result = authDataSource.signIn(email, password)
            val user = result.user

            if (user != null) {
                try {
                    // Asegura que existe un perfil de negocio (crea si no existe)
                    dataSource.ensureBusinessProfileForUser(user)
                    emit(Resource.Success(user))
                } catch (e: Exception) {
                    AppLogger.e("AuthRepository", "Error al asegurar perfil de negocio: ${e.message}")
                    // Si el error es de permisos, avisamos al usuario pero permitimos el login si es posible
                    if (e.message?.contains("PERMISSION_DENIED") == true) {
                        emit(Resource.Error("Error de base de datos: Verifica los permisos de tu cuenta o contacta a soporte."))
                    } else {
                        emit(Resource.Error("Error al configurar tu perfil: ${e.localizedMessage}"))
                    }
                }
            } else {
                emit(Resource.Error("Error al iniciar sesión"))
            }

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val errorMessage = when (e) {
                is FirebaseAuthException -> when (e.errorCode) {
                    "ERROR_INVALID_EMAIL" -> "El correo electrónico no tiene un formato válido."
                    "ERROR_USER_NOT_FOUND" -> "No existe ninguna cuenta con este correo."
                    "ERROR_WRONG_PASSWORD" -> "La contraseña es incorrecta."
                    "ERROR_USER_DISABLED" -> "Esta cuenta ha sido deshabilitada."
                    "ERROR_TOO_MANY_REQUESTS" -> "Demasiados intentos. Intenta más tarde."
                    "ERROR_NETWORK_REQUEST_FAILED" -> "Error de red. Verifica tu conexión a internet."
                    "ERROR_EMAIL_ALREADY_IN_USE" -> "Este correo ya está registrado en otra cuenta."
                    "ERROR_WEAK_PASSWORD" -> "La contraseña es muy débil (mínimo 6 caracteres)."
                    else -> "Error de autenticación: ${e.localizedMessage ?: "desconocido"}"
                }
                else -> {
                    val msg = e.localizedMessage ?: ""
                    when {
                        msg.contains("already in use") -> "Este correo ya está en uso por otro negocio."
                        msg.contains("network") -> "Error de conexión. Revisa tu internet."
                        msg.contains("PERMISSION_DENIED") -> "Error de acceso: No tienes permisos para esta carpeta. Revisa tus reglas de Firebase."
                        else -> "Error al entrar al sistema: ${e.localizedMessage}"
                    }
                }
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

            val result = authDataSource.signUp(email, password)
            val user = result.user

            if (user != null) {
                try {
                    dataSource.createBusinessProfileForUser(user, businessName)
                    emit(Resource.Success(user))
                } catch (e: Exception) {
                    AppLogger.e("AuthRepository", "Error al crear perfil en registro: ${e.message}")
                    val detail = if (e.message?.contains("PERMISSION_DENIED") == true) 
                        "Error de permisos en Firebase. Asegúrate de haber desplegado las reglas de Firestore correctamente." 
                        else e.localizedMessage
                    emit(Resource.Error("Registro exitoso en Auth, pero falló la base de datos: $detail"))
                }
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

            authDataSource.sendPasswordResetEmail(email.trim())
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
            authDataSource.signOut()
            emit(Resource.Success(Unit))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error("Error al cerrar sesión: ${e.message}"))
        }
    }

    override suspend fun updateProfile(name: String, phone: String?): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())
            val user = authDataSource.getCurrentUser() ?: throw Exception("No hay usuario autenticado")
            
            val profileUpdates = userProfileChangeRequest {
                displayName = name
            }
            
            // Usamos tareas de Firebase directas aquí o extendemos AuthDataSource
            com.google.android.gms.tasks.Tasks.await(user.updateProfile(profileUpdates))
            
            // Actualizar teléfono si es necesario en Firestore (SaaS Profile)
            val uid = user.uid
            dataSource.updateDocument(COLLECTION_USUARIOS, uid, mapOf(
                "nombre" to name,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            ))
            
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error("Error al actualizar perfil: ${e.message}"))
        }
    }

    /**
     * Verifica si hay un usuario autenticado (no suspendida)
     */
    override fun isUserAuthenticated(): Boolean = authDataSource.isUserAuthenticated()
}