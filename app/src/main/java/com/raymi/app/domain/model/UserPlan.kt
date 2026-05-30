package com.raymi.app.domain.model

import com.google.firebase.Timestamp

/**
 * Plan del usuario: FREE (con anuncios) o PRO (sin anuncios, ilimitado)
 * 
 * PERÚ mercado:
 * - FREE: S/. 0 (mostrar ads)
 * - PRO: S/. 19.99/mes (sin ads, workspaces/items ilimitados)
 */
data class UserPlan(
    val userId: String = "",
    val plan: PlanType = PlanType.FREE,
    val precioMensual: Double = 0.0,  // PEN (Soles Peruanos)
    val activo: Boolean = true,
    val fechaInicio: Timestamp = Timestamp.now(),
    val fechaVencimiento: Timestamp? = null,
    val metodoPago: String = "",  // "GOOGLE_PLAY" | "TRANSFERENCIA" | "TARJETA"
    val idCompra: String? = null,  // Google Play Billing ID
    val renovacionAutomatica: Boolean = false,
    val workspacesLimit: Int = 1,  // FREE=1, PRO=ilimitado
    val itemsLimit: Int = 30,      // FREE=30, PRO=ilimitado
    val clientsLimit: Int = 40,    // FREE=40, PRO=ilimitado
    val mostrarAnuncios: Boolean = true,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

enum class PlanType {
    FREE,   // S/. 0: Con anuncios, 1 workspace, 30 items, 40 clientes
    PRO;    // S/. 20.00/mes: Sin anuncios, ilimitado

    companion object {
        const val PRICE_FREE = 0.0
        const val PRICE_PRO = 20.00 
        const val PRICE_PRO_USD = 5.40
        const val CURRENCY = "PEN"
    }
}

data class PlanDetails(
    val tipo: PlanType,
    val precio: Double,
    val moneda: String = "PEN",  // Soles peruanos
    val workspacesCount: Int,
    val itemsCount: Int,
    val mostrarAnuncios: Boolean,
    val descripcion: String
)

