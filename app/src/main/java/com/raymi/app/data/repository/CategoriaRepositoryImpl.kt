package com.raymi.app.data.repository

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

    override suspend fun getCategorias(workspaceId: String): Flow<Resource<List<Categoria>>> = flow {
        emit(Resource.Loading())
        try {
            // Buscamos en la subcolección del negocio siguiendo el plan SaaS y reglas de Firestore
            val response = dataSource.queryDocuments("negocios/$workspaceId/categorias", "activa", true)
            val categorias = response.map { (id, data) ->
                CategoriaDto.fromMap(id, data).toDomain()
            }.sortedBy { it.orden }
            emit(Resource.Success(categorias))
        } catch (e: Exception) {
            emit(Resource.Error("Error al cargar categorías: ${e.localizedMessage}"))
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
            emit(Resource.Success(id))
        } catch (e: Exception) {
            emit(Resource.Error("Error al crear categoría: ${e.message}"))
        }
    }

    override suspend fun updateCategoria(categoria: Categoria): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val dto = CategoriaDto.fromDomain(categoria)
            val path = "negocios/${categoria.workspaceId}/categorias"
            dataSource.updateDocument(path, categoria.id, dto.toMap().filterValues { it != null }.mapValues { it.value!! })
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error("Error al actualizar categoría"))
        }
    }

    override suspend fun deleteCategoria(workspaceId: String, categoriaId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val path = "negocios/$workspaceId/categorias"
            dataSource.deleteDocument(path, categoriaId)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error("Error al eliminar categoría"))
        }
    }
}
