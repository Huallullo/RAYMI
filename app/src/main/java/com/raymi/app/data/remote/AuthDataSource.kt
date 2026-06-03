package com.raymi.app.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fuente de datos dedicada exclusivamente a la Autenticación.
 */
@Singleton
class AuthDataSource @Inject constructor(
    private val auth: FirebaseAuth
) {
    fun getCurrentUser(): FirebaseUser? = auth.currentUser
    
    fun isUserAuthenticated(): Boolean = auth.currentUser != null
    
    suspend fun signIn(email: String, password: String) = 
        auth.signInWithEmailAndPassword(email, password).await()
        
    suspend fun signUp(email: String, password: String) = 
        auth.createUserWithEmailAndPassword(email, password).await()
        
    suspend fun sendPasswordResetEmail(email: String) = 
        auth.sendPasswordResetEmail(email).await()
        
    suspend fun updatePassword(password: String) {
        val user = auth.currentUser ?: throw IllegalStateException("No hay sesión activa para cambiar contraseña")
        user.updatePassword(password).await()
    }
        
    fun signOut() = auth.signOut()
}
