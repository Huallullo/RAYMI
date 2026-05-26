package com.raymi.app.core.workspace

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.raymi.app.domain.model.Workspace
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "workspace_prefs")

/**
 * Gestor del workspace actual en la sesión del usuario.
 * Persiste el negocio seleccionado entre reinicios de la app.
 */
@Singleton
class WorkspaceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val KEY_WORKSPACE_ID = stringPreferencesKey("current_workspace_id")

    private val _currentWorkspace = MutableStateFlow<Workspace?>(null)
    val currentWorkspace: StateFlow<Workspace?> = _currentWorkspace.asStateFlow()

    init {
        // Restaurar workspaceID desde persistencia al iniciar
        scope.launch {
            context.dataStore.data.map { prefs ->
                prefs[KEY_WORKSPACE_ID]
            }.collect { id ->
                if (id != null && _currentWorkspace.value == null) {
                    // Solo seteamos el ID básico, los repositorios cargarán el objeto completo
                    _currentWorkspace.value = Workspace(id = id)
                }
            }
        }
    }

    /**
     * Establece el workspace activo y lo persiste.
     */
    fun setWorkspace(workspace: Workspace) {
        _currentWorkspace.value = workspace
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[KEY_WORKSPACE_ID] = workspace.id
            }
        }
    }

    /**
     * Obtiene el ID del workspace actual.
     */
    fun getWorkspaceId(): String? {
        return _currentWorkspace.value?.id
    }
}
