package com.raymi.app.presentation

import androidx.lifecycle.ViewModel
import com.raymi.app.core.utils.ConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    connectivityObserver: ConnectivityObserver
) : ViewModel() {
    val isConnected: StateFlow<Boolean> = connectivityObserver.isConnected
}
