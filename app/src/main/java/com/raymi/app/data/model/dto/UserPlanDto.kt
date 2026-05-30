package com.raymi.app.data.model.dto

import com.google.firebase.Timestamp
import com.raymi.app.domain.model.PlanType
import com.raymi.app.domain.model.UserPlan

data class UserPlanDto(
    val userId: String = "",
    val plan: String = "FREE",
    val precioMensual: Double = 0.0,
    val activo: Boolean = true,
    val fechaInicio: Timestamp = Timestamp.now(),
    val fechaVencimiento: Timestamp? = null,
    val metodoPago: String = "",
    val idCompra: String? = null,
    val renovacionAutomatica: Boolean = false,
    val workspacesLimit: Int = 1,
    val itemsLimit: Int = 30,
    val clientsLimit: Int = 50,
    val mostrarAnuncios: Boolean = true,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    fun toDomain(): UserPlan = UserPlan(
        userId = userId,
        plan = try { PlanType.valueOf(plan) } catch (e: Exception) { PlanType.FREE },
        precioMensual = precioMensual,
        activo = activo,
        fechaInicio = fechaInicio,
        fechaVencimiento = fechaVencimiento,
        metodoPago = metodoPago,
        idCompra = idCompra,
        renovacionAutomatica = renovacionAutomatica,
        workspacesLimit = workspacesLimit,
        itemsLimit = itemsLimit,
        clientsLimit = clientsLimit,
        mostrarAnuncios = mostrarAnuncios,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(domain: UserPlan): UserPlanDto = UserPlanDto(
            userId = domain.userId,
            plan = domain.plan.name,
            precioMensual = domain.precioMensual,
            activo = domain.activo,
            fechaInicio = domain.fechaInicio,
            fechaVencimiento = domain.fechaVencimiento,
            metodoPago = domain.metodoPago,
            idCompra = domain.idCompra,
            renovacionAutomatica = domain.renovacionAutomatica,
            workspacesLimit = domain.workspacesLimit,
            itemsLimit = domain.itemsLimit,
            clientsLimit = domain.clientsLimit,
            mostrarAnuncios = domain.mostrarAnuncios,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )

        fun fromMap(userId: String, map: Map<String, Any>): UserPlanDto = UserPlanDto(
            userId = userId,
            plan = map["plan"] as? String ?: "FREE",
            precioMensual = (map["precioMensual"] as? Number)?.toDouble() ?: 0.0,
            activo = map["activo"] as? Boolean ?: true,
            fechaInicio = map["fechaInicio"] as? Timestamp ?: Timestamp.now(),
            fechaVencimiento = map["fechaVencimiento"] as? Timestamp,
            metodoPago = map["metodoPago"] as? String ?: "",
            idCompra = map["idCompra"] as? String,
            renovacionAutomatica = map["renovacionAutomatica"] as? Boolean ?: false,
            workspacesLimit = (map["workspacesLimit"] as? Number)?.toInt() ?: 1,
            itemsLimit = (map["itemsLimit"] as? Number)?.toInt() ?: 30,
            clientsLimit = (map["clientsLimit"] as? Number)?.toInt() ?: 40,
            mostrarAnuncios = map["mostrarAnuncios"] as? Boolean ?: true,
            createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now(),
            updatedAt = map["updatedAt"] as? Timestamp ?: Timestamp.now()
        )
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "plan" to plan,
        "precioMensual" to precioMensual,
        "activo" to activo,
        "fechaInicio" to fechaInicio,
        "fechaVencimiento" to fechaVencimiento,
        "metodoPago" to metodoPago,
        "idCompra" to idCompra,
        "renovacionAutomatica" to renovacionAutomatica,
        "workspacesLimit" to workspacesLimit,
        "itemsLimit" to itemsLimit,
        "clientsLimit" to clientsLimit,
        "mostrarAnuncios" to mostrarAnuncios,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )
}
