package com.raymi.app.core.workspace

import com.raymi.app.domain.model.Workspace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestor del workspace actual en la sesión del usuario.
 * Permite a diferentes ViewModels conocer qué negocio está gestionando el usuario.
 */
@Singleton
class WorkspaceManager @Inject constructor() {

    private val _currentWorkspace = MutableStateFlow<Workspace?>(null)
    val currentWorkspace: StateFlow<Workspace?> = _currentWorkspace.asStateFlow()

    /**
     * Establece el workspace activo para la sesión actual
     */
    fun setWorkspace(workspace: Workspace) {
        _currentWorkspace.value = workspace
    }

    /**
     * Obtiene el ID del workspace actual. Lanza excepción si no hay uno seleccionado.
     */
    fun getWorkspaceId(): String {
        return _currentWorkspace.value?.id ?: throw IllegalStateException("No hay un workspace seleccionado")
    }

    /**
     * Limpia el workspace actual (ej: al cerrar sesión)
     */
    fun clearWorkspace() {
        _currentWorkspace.value = null
    }

    /**
     * Verifica si hay un workspace seleccionado
     */
    fun hasWorkspaceSelected(): Boolean {
        return _currentWorkspace.value != null
    }
}
