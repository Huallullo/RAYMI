package com.raymi.app.core.workspace

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.raymi.app.domain.model.Workspace
import com.raymi.app.domain.repository.WorkspaceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import javax.inject.Provider
import com.raymi.app.domain.model.Resource
import com.raymi.app.data.remote.FirebaseDataSource

private val Context.dataStore by preferencesDataStore(name = "workspace_prefs")

/**
 * Gestor del workspace actual en la sesión del usuario.
 * Persiste el negocio seleccionado entre reinicios de la app.
 */
@Singleton
class WorkspaceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workspaceRepositoryProvider: Provider<WorkspaceRepository>,
    private val firebaseDataSource: FirebaseDataSource
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val KEY_WORKSPACE_ID = stringPreferencesKey("current_workspace_id")
    private val KEY_LANGUAGE = stringPreferencesKey("app_language")

    private val _currentWorkspace = MutableStateFlow<Workspace?>(null)
    val currentWorkspace: StateFlow<Workspace?> = _currentWorkspace.asStateFlow()

    private val _currentLanguage = MutableStateFlow("es")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    init {
        // Restaurar workspaceID e idioma desde persistencia al iniciar
        scope.launch {
            val prefs = context.dataStore.data.first()
            val id = prefs[KEY_WORKSPACE_ID]
            val lang = prefs[KEY_LANGUAGE] ?: "es"

            _currentLanguage.value = lang

            if (id != null && _currentWorkspace.value == null) {
                // Seteamos caché inicial
                firebaseDataSource.setBusinessId(id)
                
                // Primero seteamos el ID básico para evitar UI vacía si es posible
                _currentWorkspace.value = Workspace(id = id)
                
                // Cargar datos completos desde el repositorio (Lectura única al iniciar)
                try {
                    val resource = workspaceRepositoryProvider.get().getWorkspaceById(id)
                        .filter { it !is Resource.Loading }
                        .first()
                    
                    if (resource is Resource.Success) {
                        _currentWorkspace.value = resource.data
                    }
                } catch (_: Exception) {
                    // Si falla la carga remota, nos quedamos con el ID parcial
                }
            }
        }
    }

    /**
     * Establece el workspace activo y lo persiste.
     */
    fun setWorkspace(workspace: Workspace) {
        _currentWorkspace.value = workspace
        firebaseDataSource.setBusinessId(workspace.id)
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

    fun setLanguage(lang: String) {
        _currentLanguage.value = lang
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[KEY_LANGUAGE] = lang
            }
        }
    }

    /**
     * Limpia el workspace actual (cerrar sesión).
     */
    fun clearWorkspace() {
        _currentWorkspace.value = null
        firebaseDataSource.clearCache()
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs.remove(KEY_WORKSPACE_ID)
            }
        }
    }
}
