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

    // IDs de prueba de Google AdMob (Se usa BuildConfig en producción)
    val ADMOB_APP_ID = com.raymi.app.BuildConfig.ADMOB_APP_ID
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
        if (plan == null) return true // Por defecto mostrar si no hay datos
        if (plan.plan != PlanType.FREE) return false
        if (!plan.mostrarAnuncios) return false
        
        // Eliminada la restricción de 30 días para permitir ver anuncios de inmediato en Plan Free
        return true
    }

    /**
     * Crea una solicitud de anuncio estándar.
     */
    fun crearAdRequest(): AdRequest {
        return AdRequest.Builder().build()
    }
}
