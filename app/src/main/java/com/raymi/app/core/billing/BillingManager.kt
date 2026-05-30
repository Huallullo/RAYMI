package com.raymi.app.core.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analytics: com.google.firebase.analytics.FirebaseAnalytics,
    private val auth: com.google.firebase.auth.FirebaseAuth,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val _isProPurchased = MutableStateFlow(false)
    val isProPurchased = _isProPurchased.asStateFlow()

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    init {
        startConnection()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                startConnection()
            }
        })
    }

    private fun queryPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { _, purchases ->
            _isProPurchased.value = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        }
    }

    fun launchBillingFlow(activity: Activity, productId: String) {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { _, productDetailsList ->
            val productDetails = productDetailsList.firstOrNull() ?: return@queryProductDetailsAsync
            
            val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: ""
            
            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                ))
                .build()

            analytics.logEvent("compra_pro_iniciada", null)
            billingClient.launchBillingFlow(activity, billingFlowParams)
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        analytics.logEvent("compra_pro_exitosa", null)
                        savePurchaseTokenToFirestore(purchase.purchaseToken)
                    }
                }
            } else {
                savePurchaseTokenToFirestore(purchase.purchaseToken)
            }
        }
    }

    private fun savePurchaseTokenToFirestore(purchaseToken: String) {
        val uid = auth.currentUser?.uid ?: return
        
        val purchaseData = mapOf(
            "purchaseToken" to purchaseToken,
            "timestamp" to com.google.firebase.Timestamp.now(),
            "status" to "PENDING"
        )
        
        // Cloud Function debe validar purchaseToken con Google Play Developer API antes de activar PRO
        firestore.collection("usuarios").document(uid)
            .collection("pendingPurchases")
            .add(purchaseData)
    }
}
