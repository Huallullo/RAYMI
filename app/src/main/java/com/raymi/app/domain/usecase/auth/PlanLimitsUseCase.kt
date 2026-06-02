package com.raymi.app.domain.usecase.auth

import com.raymi.app.core.cache.SmartCache
import com.raymi.app.domain.model.PlanType
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.UserPlan
import com.raymi.app.domain.repository.UserPlanRepository
import com.raymi.app.domain.usecase.workspace.GetWorkspaceStatsUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Caso de uso centralizado para verificar límites del plan del usuario.
 * OPTIMIZACIÓN: Cachea el plan en memoria para evitar lecturas repetitivas a Firestore.
 */
@Singleton
class PlanLimitsUseCase @Inject constructor(
    private val userPlanRepository: UserPlanRepository,
    private val getWorkspaceStatsUseCase: GetWorkspaceStatsUseCase
) {
    private val planCache = SmartCache<UserPlan>()
    private val TTL_30_MIN = 30 * 60 * 1000L

    private suspend fun getPlan(userId: String): UserPlan? {
        planCache.get()?.let { return it }
        val result = userPlanRepository.getUserPlan(userId).first { it !is Resource.Loading }
        return if (result is Resource.Success) {
            result.data?.also { planCache.set(it, TTL_30_MIN) }
        } else null
    }

    suspend fun canAddMoreClients(userId: String, workspaceId: String): Boolean {
        val plan = getPlan(userId) ?: return true
        if (plan.plan == PlanType.PRO) return true
        
        val statsResult = getWorkspaceStatsUseCase(workspaceId).first { it !is Resource.Loading }
        val stats = if (statsResult is Resource.Success) statsResult.data else emptyMap()
        val currentClients = (stats?.get("totalClientes") as? Number)?.toInt() ?: 0
        
        val limit = if (plan.clientsLimit > 0) plan.clientsLimit else 40
        return currentClients < limit
    }

    suspend fun canAddMoreItems(userId: String, workspaceId: String): Boolean {
        val plan = getPlan(userId) ?: return true
        if (plan.plan == PlanType.PRO) return true

        val statsResult = getWorkspaceStatsUseCase(workspaceId).first { it !is Resource.Loading }
        val stats = if (statsResult is Resource.Success) statsResult.data else emptyMap()
        val currentItems = (stats?.get("totalItems") as? Number)?.toInt() ?: 0

        return currentItems < plan.itemsLimit
    }

    fun getCachedPlan(): UserPlan? = planCache.get()

    suspend fun invalidateCache() {
        planCache.invalidate()
    }
}
