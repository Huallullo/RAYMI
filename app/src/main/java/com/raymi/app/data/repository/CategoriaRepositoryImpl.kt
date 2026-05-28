package com.raymi.app.data.repository

import com.raymi.app.core.cache.SmartCache
import com.raymi.app.data.model.dto.CategoriaDto
import com.raymi.app.data.remote.FirebaseDataSource
import com.raymi.app.domain.model.Categoria
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.repository.CategoriaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoriaRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseDataSource
) : CategoriaRepository {

    // Cache de 30 minutos — categorías no cambian frecuentemente
    private val cacheByWorkspace = mutableMapOf<String, SmartCache<List<Categoria>>>()

    private fun getCacheFor(workspaceId: String) =
        cacheByWorkspace.getOrPut(workspaceId) { SmartCache() }

    override suspend fun getCategorias(workspaceId: String): Flow<Resource<List<Categoria>>> = flow {
        emit(Resource.Loading())
        try {
            if (workspaceId.isBlank()) {
                emit(Resource.Error("ID de negocio no válido"))
                return@flow
            }

            // Primero intenta el caché (30 min TTL)
            val cache = getCacheFor(workspaceId)
            val cached = cache.get()
            if (cached != null) {
                emit(Resource.Success(cached))
                return@flow
            }

            // Solo llama a Firestore si el caché expiró
            val response = dataSource.queryBusinessDocuments(
                collection = "categorias",
                field = "activa",
                value = true,
                negocioId = workspaceId
            )
            val categorias = response
                .map { (id, data) -> CategoriaDto.fromMap(id, data).toDomain() }
                .sortedBy { it.orden }

            cache.set(categorias, ttlMs = 30 * 60 * 1000) // 30 minutos
            emit(Resource.Success(categorias))
        } catch (e: Exception) {
            val errorMsg = if (e.message?.contains("PERMISSION_DENIED") == true) {
                "Acceso denegado: Revisa tus permisos en Firebase."
            } else {
                "Error al cargar categorías: ${e.localizedMessage}"
            }
            emit(Resource.Error(errorMsg))
        }
    }

    override suspend fun addCategoria(categoria: Categoria): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            val dto = CategoriaDto.fromDomain(categoria)
            val id = dataSource.addBusinessDocument(
                workspaceId = categoria.workspaceId,
                collection = "categorias",
                data = dto.toMap().filterValues { it != null }.mapValues { it.value!! }
            )
            getCacheFor(categoria.workspaceId).invalidate() // invalida al crear
            emit(Resource.Success(id))
        } catch (e: Exception) {
            emit(Resource.Error("Error al crear categoría: ${e.message}"))
        }
    }

    override suspend fun updateCategoria(categoria: Categoria): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val dto = CategoriaDto.fromDomain(categoria)
            dataSource.updateBusinessDocument(
                collection = "categorias",
                documentId = categoria.id,
                data = dto.toMap().filterValues { it != null }.mapValues { it.value!! }
            )
            getCacheFor(categoria.workspaceId).invalidate() // invalida al editar
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error("Error al actualizar: ${e.message}"))
        }
    }

    override suspend fun deleteCategoria(workspaceId: String, categoriaId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            dataSource.deleteBusinessDocument(
                collection = "categorias",
                documentId = categoriaId
            )
            getCacheFor(workspaceId).invalidate() // invalida al borrar
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error("Error al eliminar: ${e.message}"))
        }
    }
}
