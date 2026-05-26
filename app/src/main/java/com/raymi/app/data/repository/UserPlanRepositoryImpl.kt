package com.raymi.app.data.repository

import com.raymi.app.data.model.dto.UserPlanDto
import com.raymi.app.data.remote.FirebaseDataSource
import com.raymi.app.domain.model.PlanType
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.UserPlan
import com.raymi.app.domain.repository.UserPlanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPlanRepositoryImpl @Inject constructor(
    private val dataSource: FirebaseDataSource
) : UserPlanRepository {

    override suspend fun getUserPlan(userId: String): Flow<Resource<UserPlan>> = flow {
        emit(Resource.Loading())
        try {
            val data = dataSource.getDocument(FirebaseDataSource.COLLECTION_USUARIOS, userId)
            if (data != null) {
                val plan = UserPlanDto.fromMap(userId, data).toDomain()
                emit(Resource.Success(plan))
            } else {
                // Si no existe, creamos un plan FREE por defecto
                emit(Resource.Success(UserPlan(userId = userId, plan = PlanType.FREE)))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Error al obtener plan: ${e.localizedMessage}"))
        }
    }

    override suspend fun createFreeUserPlan(userId: String): Flow<Resource<UserPlan>> = flow {
        emit(Resource.Loading())
        try {
            val newPlan = UserPlan(userId = userId, plan = PlanType.FREE)
            val dto = UserPlanDto.fromDomain(newPlan)
            dataSource.updateDocument(FirebaseDataSource.COLLECTION_USUARIOS, userId, dto.toMap().filterValues { it != null }.mapValues { it.value!! })
            emit(Resource.Success(newPlan))
        } catch (_: Exception) {
            emit(Resource.Error("Error al crear plan inicial"))
        }
    }

    override suspend fun upgradeToPro(userId: String): Flow<Resource<UserPlan>> = flow {
        emit(Resource.Loading())
        try {
            val proPlan = UserPlan(
                userId = userId, 
                plan = PlanType.PRO, 
                workspacesLimit = 999, 
                itemsLimit = 9999, 
                mostrarAnuncios = false
            )
            val dto = UserPlanDto.fromDomain(proPlan)
            dataSource.updateDocument(FirebaseDataSource.COLLECTION_USUARIOS, userId, dto.toMap().filterValues { it != null }.mapValues { it.value!! })
            emit(Resource.Success(proPlan))
        } catch (_: Exception) {
            emit(Resource.Error("Error al subir a PRO"))
        }
    }

    override suspend fun getPlanDetails(planType: PlanType): Resource<Map<String, Any>> {
        return when (planType) {
            PlanType.FREE -> Resource.Success(mapOf(
                "nombre" to "Plan Gratuito",
                "precio" to PlanType.PRICE_FREE,
                "items" to 50,
                "workspaces" to 1
            ))
            PlanType.PRO -> Resource.Success(mapOf(
                "nombre" to "Plan Pro Business",
                "precio" to PlanType.PRICE_PRO,
                "items" to "Ilimitados",
                "workspaces" to "Ilimitados"
            ))
        }
    }

    override suspend fun canCreateWorkspace(userId: String): Boolean {
        return try {
            val planResult = getUserPlan(userId).first { it !is Resource.Loading }
            if (planResult is Resource.Success) {
                val plan = planResult.data ?: return false
                if (plan.plan == PlanType.PRO) return true
                
                // Contar negocios donde el usuario es dueño
                val ownedWorkspaces = dataSource.queryDocuments(FirebaseDataSource.COLLECTION_NEGOCIOS, "ownerUid", userId)
                ownedWorkspaces.size < plan.workspacesLimit
            } else false
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun canAddMoreItems(userId: String, workspaceId: String): Boolean {
        return try {
            val planResult = getUserPlan(userId).first { it !is Resource.Loading }
            if (planResult is Resource.Success) {
                val plan = planResult.data ?: return false
                if (plan.plan == PlanType.PRO) return true
                
                // Usar estadísticas atómicas para evitar lecturas costosas
                val stats = dataSource.getStats(workspaceId)
                val currentItems = (stats?.get("totalItems") as? Number)?.toInt() ?: 0
                currentItems < plan.itemsLimit
            } else false
        } catch (_: Exception) {
            false
        }
    }
}
