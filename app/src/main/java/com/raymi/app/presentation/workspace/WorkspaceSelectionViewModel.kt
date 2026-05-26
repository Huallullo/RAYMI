package com.raymi.app.presentation.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.core.workspace.WorkspaceManager
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.Workspace
import com.raymi.app.domain.repository.AuthRepository
import com.raymi.app.domain.usecase.workspace.GetWorkspacesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkspaceSelectionViewModel @Inject constructor(
    private val getWorkspacesUseCase: GetWorkspacesUseCase,
    private val authRepository: AuthRepository,
    private val workspaceManager: WorkspaceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkspaceSelectionUiState())
    val uiState: StateFlow<WorkspaceSelectionUiState> = _uiState.asStateFlow()

    init {
        loadWorkspaces()
    }

    fun loadWorkspaces() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            if (user != null) {
                getWorkspacesUseCase(user.uid).collect { result ->
                    when (result) {
                        is Resource.Loading -> {
                            _uiState.value = _uiState.value.copy(isLoading = true)
                        }
                        is Resource.Success -> {
                            _uiState.value = _uiState.value.copy(
                                workspaces = result.data ?: emptyList(),
                                isLoading = false
                            )
                        }
                        is Resource.Error -> {
                            _uiState.value = _uiState.value.copy(
                                error = result.message,
                                isLoading = false
                            )
                        }
                    }
                }
            } else {
                _uiState.value = _uiState.value.copy(error = "Usuario no autenticado")
            }
        }
    }

    fun selectWorkspace(workspace: Workspace) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        workspaceManager.setWorkspace(workspace)
        _uiState.value = _uiState.value.copy(workspaceSelected = true, isLoading = false)
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout().collect { result ->
                if (result is Resource.Success) {
                    onSuccess()
                }
            }
        }
    }
}

data class WorkspaceSelectionUiState(
    val workspaces: List<Workspace> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val workspaceSelected: Boolean = false
)
