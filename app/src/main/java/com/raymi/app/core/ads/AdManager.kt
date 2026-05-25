package com.raymi.app.core.ads

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.raymi.app.domain.model.PlanType
import com.raymi.app.domain.model.UserPlan

/**
 * Gestor Maestro de Publicidad (Monetización SaaS).
 * Centraliza la lógica de AdMob para generar ingresos en el plan gratuito.
 */
object AdManager {

    // IDs de prueba de Google AdMob (Cambiar por los reales en producción)
    const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"

    /**
     * Inicializa el SDK de anuncios. Llamar en MainActivity.
     */
    fun inicializar(context: Context) {
        MobileAds.initialize(context) { }
    }

    /**
     * Determina si se deben mostrar anuncios basado en el plan del usuario.
     */
    fun debeMostrarAnuncios(plan: UserPlan?): Boolean {
        if (plan == null) return true // Por seguridad, si no hay plan, mostramos ads
        return plan.plan == PlanType.FREE && plan.mostrarAnuncios
    }

    /**
     * Crea una solicitud de anuncio estándar.
     */
    fun crearAdRequest(): AdRequest {
        return AdRequest.Builder().build()
    }
}
