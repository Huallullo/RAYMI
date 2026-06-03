package com.raymi.app.data.repository

import com.raymi.app.core.utils.Constants.COLLECTION_USUARIOS
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
        val result = try {
            val data = dataSource.getDocument(COLLECTION_USUARIOS, userId)
            if (data != null) {
                val plan = UserPlanDto.fromMap(userId, data).toDomain()
                Resource.Success(plan)
            } else {
                Resource.Success(UserPlan(userId = userId, plan = PlanType.FREE))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Resource.Error("Error al obtener plan: ${e.localizedMessage}")
        }
        emit(result)
    }

    override suspend fun createFreeUserPlan(userId: String): Flow<Resource<UserPlan>> = flow {
        emit(Resource.Loading())
        val result = try {
            val newPlan = UserPlan(userId = userId, plan = PlanType.FREE)
            val dto = UserPlanDto.fromDomain(newPlan)
            dataSource.updateDocument(COLLECTION_USUARIOS, userId, dto.toMap().filterValues { it != null }.mapValues { it.value!! })
            Resource.Success(newPlan)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Resource.Error("Error al crear plan inicial")
        }
        emit(result)
    }

    override suspend fun getPlanDetails(planType: PlanType): Resource<Map<String, Any>> {
        try {
            val remoteConfig = dataSource.getDocument("config", "planes")
            if (remoteConfig != null) {
                val planData = remoteConfig[planType.name.lowercase()] as? Map<String, Any>
                if (planData != null) return Resource.Success(planData)
            }
        } catch (_: Exception) { }

        // Fallback local
        return when (planType) {
            PlanType.FREE -> Resource.Success(mapOf(
                "nombre" to "Plan Gratuito",
                "precio" to PlanType.PRICE_FREE,
                "items" to 30,
                "workspaces" to 1,
                "clientes" to 40
            ))
            PlanType.PRO -> Resource.Success(mapOf(
                "nombre" to "Plan Pro Business",
                "precio" to PlanType.PRICE_PRO,
                "items" to "Ilimitados",
                "workspaces" to "Ilimitados"
            ))
        }
    }
}
