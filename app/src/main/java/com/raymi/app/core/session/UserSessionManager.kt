package com.raymi.app.core.session

import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.UserPlan
import com.raymi.app.domain.repository.UserPlanRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSessionManager @Inject constructor(
    private val userPlanRepository: UserPlanRepository,
    private val auth: com.google.firebase.auth.FirebaseAuth
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val _userPlan = MutableStateFlow<UserPlan?>(null)
    val userPlan: StateFlow<UserPlan?> = _userPlan.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.uid
            if (uid != null) {
                observeUserPlan(uid)
            } else {
                _userPlan.value = null
            }
        }
    }

    private fun observeUserPlan(uid: String) {
        scope.launch {
            userPlanRepository.getUserPlan(uid).collect { result ->
                if (result is Resource.Success) {
                    _userPlan.value = result.data
                }
            }
        }
    }

    fun refreshPlan() {
        auth.uid?.let { observeUserPlan(it) }
    }
}
