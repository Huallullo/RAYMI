package com.raymi.app.domain.repository

import com.raymi.app.domain.model.Comprobante
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.TipoComprobante
import kotlinx.coroutines.flow.Flow

interface ComprobanteRepository {
    suspend fun getNextNumber(workspaceId: String, tipo: TipoComprobante): Flow<Resource<Int>>
    suspend fun saveComprobante(comprobante: Comprobante): Flow<Resource<String>>
    suspend fun getComprobantesByAlquiler(workspaceId: String, alquilerId: String): Flow<Resource<List<Comprobante>>>
    suspend fun getComprobanteById(workspaceId: String, id: String): Flow<Resource<Comprobante>>
    suspend fun anularComprobante(workspaceId: String, comprobanteId: String): Flow<Resource<Unit>>
    suspend fun updateEstado(comprobanteId: String, estado: com.raymi.app.domain.model.EstadoComprobante): Flow<Resource<Unit>>
}
