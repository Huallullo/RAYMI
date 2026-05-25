package com.raymi.app.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.raymi.app.core.ads.AdManager

/**
 * Componente de Banner Publicitario (Recurso de Ingresos).
 * Renderiza un anuncio de AdMob de forma adaptativa.
 */
@Composable
fun AdBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = AdManager.TEST_BANNER_ID
) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                setAdUnitId(adUnitId)
                loadAd(AdManager.crearAdRequest())
            }
        }
    )
}
