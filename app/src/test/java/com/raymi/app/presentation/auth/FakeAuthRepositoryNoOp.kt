package com.raymi.app.presentation.auth

import com.google.firebase.auth.FirebaseUser
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeAuthRepositoryNoOp : AuthRepository {
    override val currentUser: FirebaseUser? = null

    override suspend fun login(email: String, password: String): Flow<Resource<FirebaseUser>> = flow {
        emit(Resource.Error("No-op test repo"))
    }

    override suspend fun register(email: String, password: String): Flow<Resource<FirebaseUser>> = flow {
        emit(Resource.Error("No-op test repo"))
    }

    override suspend fun logout(): Flow<Resource<Unit>> = flow {
        emit(Resource.Success(Unit))
    }

    override fun isUserAuthenticated(): Boolean = false
}