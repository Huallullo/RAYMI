package com.raymi.app.domain.repository

import com.raymi.app.domain.model.Mantenimiento
import com.raymi.app.domain.model.Resource
import kotlinx.coroutines.flow.Flow

interface MantenimientoRepository {
    suspend fun getMantenimientosByItem(workspaceId: String, itemId: String): Flow<Resource<List<Mantenimiento>>>
    suspend fun addMantenimiento(mantenimiento: Mantenimiento): Flow<Resource<String>>
    suspend fun deleteMantenimiento(workspaceId: String, itemId: String, maintenanceId: String): Flow<Resource<Unit>>
}
