package com.raymi.app.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Componente de Banner Publicitario (Recurso de Ingresos).
 * Renderiza un anuncio de AdMob de forma adaptativa.
 */
@Composable
fun AdBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = com.raymi.app.BuildConfig.ADMOB_BANNER_ID
) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                setAdUnitId(adUnitId)
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
