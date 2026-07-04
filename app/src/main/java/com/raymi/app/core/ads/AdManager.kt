package com.raymi.app.core.ads

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.raymi.app.domain.model.PlanType
import com.raymi.app.domain.model.UserPlan
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestor Maestro de Publicidad (Monetización SaaS).
 * Inyectable via Hilt para mejor testabilidad y ciclo de vida.
 */
@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun inicializar() {
        MobileAds.initialize(context) { }
    }

    /**
     * Determina si se deben mostrar anuncios basado en el plan del usuario.
     */
    fun debeMostrarAnuncios(plan: UserPlan?): Boolean {
        // [QA Senior] Deshabilitar anuncios en pruebas de instrumentación
        if (isRunningTest()) return false

        if (plan == null) return true 
        if (plan.plan != PlanType.FREE) return false
        if (!plan.mostrarAnuncios) return false
        return true
    }

    companion object {
        fun isRunningTest(): Boolean {
            if (System.getProperty("dexmaker.dexcache") != null) return true
            return try {
                Class.forName("androidx.test.platform.app.InstrumentationRegistry")
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun crearAdRequest(): AdRequest = AdRequest.Builder().build()
}
