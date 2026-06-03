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

    // OPTIMIZACIÓN: TTL de 4 horas para categorías (cambian poco)
    private val TTL_CATEGORIAS = 4 * 60 * 60 * 1000L
    private val cacheByWorkspace = mutableMapOf<String, SmartCache<List<Categoria>>>()

    private fun getCacheFor(workspaceId: String) =
        cacheByWorkspace.getOrPut(workspaceId) { SmartCache() }

    override fun getCategorias(workspaceId: String): Flow<Resource<List<Categoria>>> = flow {
        emit(Resource.Loading())
        
        val result = try {
            if (workspaceId.isBlank()) {
                Resource.Error("ID de negocio no válido")
            } else {
                val cache = getCacheFor(workspaceId)
                val cached = cache.get()
                if (cached != null) {
                    Resource.Success(cached)
                } else {
                    val response = dataSource.queryBusinessDocuments(
                        collection = "categorias",
                        field = "activa",
                        value = true,
                        negocioId = workspaceId
                    )
                    val categorias = response
                        .map { (id, data) -> CategoriaDto.fromMap(id, data).toDomain() }
                        .sortedBy { it.orden }

                    cache.set(categorias, ttlMs = TTL_CATEGORIAS)
                    Resource.Success(categorias)
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            val errorMsg = if (e.message?.contains("PERMISSION_DENIED") == true) {
                "Acceso denegado: Revisa tus permisos en Firebase."
            } else {
                "Error al cargar categorías: ${e.localizedMessage}"
            }
            Resource.Error(errorMsg)
        }
        
        emit(result)
    }

    override fun addCategoria(categoria: Categoria): Flow<Resource<String>> = flow {
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

    override fun updateCategoria(categoria: Categoria): Flow<Resource<Unit>> = flow {
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

    override fun deleteCategoria(workspaceId: String, categoriaId: String): Flow<Resource<Unit>> = flow {
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

    override suspend fun invalidarCache(workspaceId: String) {
        getCacheFor(workspaceId).invalidate()
    }
}
