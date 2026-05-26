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
            if (workspaceId.isBlank()) {
                emit(Resource.Error("ID de negocio no válido"))
                return@flow
            }
            
            val response = dataSource.queryBusinessDocuments(
                collection = "categorias",
                field = "activa",
                value = true,
                negocioId = workspaceId
            )
            val categorias = response.map { (id, data) ->
                CategoriaDto.fromMap(id, data).toDomain()
            }.sortedBy { it.orden }
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
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error("Error al actualizar: ${e.message}"))
        }
    }

    override suspend fun deleteCategoria(workspaceId: String, categoriaId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            // Nota: En un sistema SaaS real, quizás prefieras un "borrado lógico" (activa = false)
            // pero aquí implementamos el borrado físico solicitado.
            dataSource.deleteBusinessDocument(
                collection = "categorias",
                documentId = categoriaId
            )
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error("Error al eliminar: ${e.message}"))
        }
    }
}
