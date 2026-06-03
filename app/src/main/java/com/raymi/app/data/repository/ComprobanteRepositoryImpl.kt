package com.raymi.app.data.repository

import com.raymi.app.data.model.dto.ComprobanteDto
import com.raymi.app.data.remote.ComprobanteDataSource
import com.raymi.app.data.remote.FirebaseDataSource
import com.raymi.app.domain.model.Comprobante
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.TipoComprobante
import com.raymi.app.domain.model.EstadoComprobante
import com.raymi.app.domain.repository.ComprobanteRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class ComprobanteRepositoryImpl @Inject constructor(
    private val comprobanteDataSource: ComprobanteDataSource,
    private val dataSource: FirebaseDataSource
) : ComprobanteRepository {

    override suspend fun getNextNumber(workspaceId: String, tipo: TipoComprobante): Flow<Resource<Int>> = flow {
        emit(Resource.Loading())
        try {
            val number = comprobanteDataSource.getNextNumberAtomic(workspaceId, tipo)
            emit(Resource.Success(number))
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error al obtener número correlativo: ${e.message}"))
        }
    }

    override suspend fun saveComprobante(comprobante: Comprobante): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            val dto = ComprobanteDto.fromDomain(comprobante)
            val data = dto.toMap().filterValues { it != null }.mapValues { it.value!! }
            val id = comprobanteDataSource.saveComprobante(comprobante.workspaceId, data)
            emit(Resource.Success(id))
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error al guardar comprobante: ${e.message}"))
        }
    }

    override suspend fun getComprobantesByAlquiler(workspaceId: String, alquilerId: String): Flow<Resource<List<Comprobante>>> = flow {
        emit(Resource.Loading())
        try {
            val response = dataSource.queryBusinessDocuments(
                collection = "comprobantes",
                field = "alquilerId",
                value = alquilerId,
                negocioId = workspaceId
            )
            val list = response.map { (id, data) -> ComprobanteDto.fromMap(id, data).toDomain() }
                .sortedByDescending { it.createdAt }
            emit(Resource.Success(list))
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error al cargar comprobantes: ${e.message}"))
        }
    }

    override suspend fun getComprobanteById(workspaceId: String, id: String): Flow<Resource<Comprobante>> = flow {
        emit(Resource.Loading())
        try {
            val data = dataSource.getBusinessDocument("comprobantes", id, workspaceId)
            if (data != null) {
                emit(Resource.Success(ComprobanteDto.fromMap(id, data).toDomain()))
            } else {
                emit(Resource.Error("Comprobante no encontrado"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error: ${e.message}"))
        }
    }

    override suspend fun anularComprobante(workspaceId: String, comprobanteId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            dataSource.updateBusinessDocument(
                collection = "comprobantes",
                documentId = comprobanteId,
                data = mapOf("estado" to "ANULADO"),
                negocioId = workspaceId
            )
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error al anular comprobante: ${e.message}"))
        }
    }

    override suspend fun updateEstado(
        comprobanteId: String,
        estado: EstadoComprobante
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val workspaceId = dataSource.getCurrentBusinessId() 
            dataSource.updateBusinessDocument(
                collection = "comprobantes",
                documentId = comprobanteId,
                data = mapOf("estado" to estado.name, "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()),
                negocioId = workspaceId
            )
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error("Fallo al actualizar estado del comprobante: ${e.message}"))
        }
    }
}
