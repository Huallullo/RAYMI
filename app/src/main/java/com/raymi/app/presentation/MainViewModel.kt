package com.raymi.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.core.utils.ConnectivityObserver
import com.raymi.app.domain.repository.AuthRepository
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    connectivityObserver: ConnectivityObserver,
    private val authRepository: AuthRepository
) : ViewModel() {
    val isConnected: StateFlow<Boolean> = connectivityObserver.isConnected

    init {
        updateFcmToken()
    }

    private fun updateFcmToken() {
        if (authRepository.isUserAuthenticated()) {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                viewModelScope.launch {
                    authRepository.updateFcmToken(token).collect()
                }
            }
        }
    }
}
