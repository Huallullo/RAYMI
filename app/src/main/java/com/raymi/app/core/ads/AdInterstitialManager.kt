package com.raymi.app.core.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.raymi.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestor de Anuncios Intersticiales.
 * Refactorizado a clase inyectable para evitar fugas de memoria y mejorar DI.
 */
@Singleton
class AdInterstitialManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var interstitialAd: InterstitialAd? = null
    private var isAdLoading = false

    fun loadAd() {
        if (interstitialAd != null || isAdLoading) return

        isAdLoading = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isAdLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isAdLoading = false
                }
            }
        )
    }

    fun showAd(activity: Activity, onAdClosed: () -> Unit) {
        if (activity.isFinishing || activity.isDestroyed) {
            onAdClosed()
            return
        }

        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    onAdClosed()
                    loadAd() 
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                    interstitialAd = null
                    onAdClosed()
                }
            }
            interstitialAd?.show(activity)
        } else {
            onAdClosed()
            loadAd()
        }
    }
}
