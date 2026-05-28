package com.raymi.app.presentation.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.core.workspace.WorkspaceManager
import com.raymi.app.data.remote.FirebaseDataSource
import com.raymi.app.domain.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ManageTeamViewModel @Inject constructor(
    private val workspaceManager: WorkspaceManager,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeamUiState())
    val uiState: StateFlow<TeamUiState> = _uiState.asStateFlow()

    init {
        cargarMiembros()
    }

    fun cargarMiembros() {
        val workspaceId = workspaceManager.getWorkspaceId() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val snapshot = firestore.collection("negocios").document(workspaceId)
                    .collection("miembros").get().await()
                
                val miembros = snapshot.documents.map { doc ->
                    Miembro(
                        uid = doc.id,
                        email = doc.getString("email") ?: "",
                        rol = doc.getString("rol") ?: "empleado",
                        estado = doc.getString("estado") ?: "ACTIVO"
                    )
                }
                _uiState.update { it.copy(miembros = miembros, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    fun invitarMiembro(email: String) {
        // TODO: En un SaaS Senior esto debería ser una Cloud Function que valide el plan PRO
        // antes de permitir agregar miembros (Fase 2 de Auditoría).
        _uiState.update { it.copy(error = "Función disponible próximamente en Cloud Functions") }
    }
}

data class TeamUiState(
    val miembros: List<Miembro> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class Miembro(
    val uid: String,
    val email: String,
    val rol: String,
    val estado: String
)
