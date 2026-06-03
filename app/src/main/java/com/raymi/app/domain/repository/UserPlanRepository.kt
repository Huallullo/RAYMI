package com.raymi.app.domain.repository

import com.raymi.app.domain.model.PlanType
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.UserPlan
import kotlinx.coroutines.flow.Flow

interface UserPlanRepository {
    /**
     * Obtiene el plan actual del usuario
     */
    suspend fun getUserPlan(userId: String): Flow<Resource<UserPlan>>
    
    /**
     * Crea un plan FREE para nuevo usuario
     */
    suspend fun createFreeUserPlan(userId: String): Flow<Resource<UserPlan>>
    
    /**
     * Obtiene detalles del plan para UI
     */
    suspend fun getPlanDetails(planType: PlanType): Resource<Map<String, Any>>
}

