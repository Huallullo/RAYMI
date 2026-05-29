package com.raymi.app.data.repository

import com.google.firebase.Timestamp
import com.raymi.app.data.remote.FirebaseDataSource
import com.raymi.app.domain.model.Mantenimiento
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.MantenimientoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class MantenimientoRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseDataSource
) : MantenimientoRepository {

    override suspend fun getMantenimientosByItem(workspaceId: String, itemId: String): Flow<Resource<List<Mantenimiento>>> = flow {
        emit(Resource.Loading())
        try {
            val response = dataSource.queryBusinessDocuments(
                collection = "mantenimientos",
                field = "itemId",
                value = itemId,
                negocioId = workspaceId
            )
            val list = response.map { (id, data) ->
                Mantenimiento(
                    id = id,
                    itemId = data["itemId"] as? String ?: "",
                    workspaceId = data["workspaceId"] as? String ?: "",
                    fecha = data["fecha"] as? Timestamp ?: Timestamp.now(),
                    motivo = data["motivo"] as? String ?: "",
                    costo = (data["costo"] as? Number)?.toDouble() ?: 0.0,
                    descripcion = data["descripcion"] as? String ?: "",
                    responsable = data["responsable"] as? String ?: "",
                    estadoFinal = data["estadoFinal"] as? String ?: "OPERATIVO",
                    createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now()
                )
            }.sortedByDescending { it.fecha }
            emit(Resource.Success(list))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error("Error al cargar mantenimientos: ${e.message}"))
        }
    }

    override suspend fun addMantenimiento(mantenimiento: Mantenimiento): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            val data = mapOf(
                "itemId" to mantenimiento.itemId,
                "workspaceId" to mantenimiento.workspaceId,
                "fecha" to mantenimiento.fecha,
                "motivo" to mantenimiento.motivo,
                "costo" to mantenimiento.costo,
                "descripcion" to mantenimiento.descripcion,
                "responsable" to mantenimiento.responsable,
                "estadoFinal" to mantenimiento.estadoFinal,
                "createdAt" to Timestamp.now()
            )
            val id = dataSource.addBusinessDocument(mantenimiento.workspaceId, "mantenimientos", data)
            emit(Resource.Success(id))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error("Error al registrar mantenimiento: ${e.message}"))
        }
    }

    override suspend fun deleteMantenimiento(workspaceId: String, itemId: String, maintenanceId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            dataSource.deleteBusinessDocument("mantenimientos", maintenanceId, workspaceId)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error("Error al eliminar: ${e.message}"))
        }
    }
}
