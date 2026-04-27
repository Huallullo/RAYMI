package com.raymi.app.presentation.auth

import com.google.firebase.auth.FirebaseUser
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.AuthRepository
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeAuthRepositoryNoOp : AuthRepository {
    override val currentUser: FirebaseUser? = null

    override suspend fun login(email: String, password: String): Flow<Resource<FirebaseUser>> = flow {
        if (email == "admin@raymi.com" && password == "admin123") {
            // Se crea un usuario falso con MockK (no se accede a sus métodos, solo se necesita no nulo)
            val mockUser = mockk<FirebaseUser>()
            emit(Resource.Success(mockUser))
        } else {
            emit(Resource.Error("Credenciales inválidas"))
        }
    }

    override suspend fun register(email: String, password: String): Flow<Resource<FirebaseUser>> = flow {
        emit(Resource.Error("No-op test repo"))
    }

    override suspend fun logout(): Flow<Resource<Unit>> = flow {
        emit(Resource.Success(Unit))
    }

    override fun isUserAuthenticated(): Boolean = false
}
