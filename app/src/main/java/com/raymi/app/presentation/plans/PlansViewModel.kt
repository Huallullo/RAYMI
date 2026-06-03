package com.raymi.app.presentation.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raymi.app.core.billing.BillingManager
import com.raymi.app.domain.model.PlanType
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.UserPlan
import com.raymi.app.domain.repository.AuthRepository
import com.raymi.app.domain.repository.UserPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class PlansViewModel @Inject constructor(
    private val userPlanRepository: UserPlanRepository,
    private val authRepository: AuthRepository,
    private val billingManager: BillingManager,
    private val analytics: com.google.firebase.analytics.FirebaseAnalytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlansUiState())
    val uiState: StateFlow<PlansUiState> = _uiState.asStateFlow()

    init {
        cargarPlanActual()
        cargarDetallesPlanes()
        observeBillingStatus()
        analytics.logEvent("upgrade_screen_vista", null)
    }

    private fun cargarDetallesPlanes() {
        viewModelScope.launch {
            val freeDetails = userPlanRepository.getPlanDetails(PlanType.FREE)
            val proDetails = userPlanRepository.getPlanDetails(PlanType.PRO)
            
            _uiState.update { it.copy(
                freePrice = (freeDetails.data?.get("precio") ?: PlanType.PRICE_FREE).toString(),
                proPrice = (proDetails.data?.get("precio") ?: PlanType.PRICE_PRO).toString()
            ) }
        }
    }

    private fun observeBillingStatus() {
        billingManager.isProPurchased
            .onEach { purchased ->
                if (purchased && _uiState.value.currentPlan?.plan != PlanType.PRO) {
                    // Solo actualizamos si el usuario compró y aún no es PRO en Firestore
                    upgradeToProAfterValidation()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun cargarPlanActual() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser() ?: return@launch
            userPlanRepository.getUserPlan(user.uid).collect { result ->
                if (result is Resource.Success) {
                    _uiState.update { it.copy(currentPlan = result.data) }
                }
            }
        }
    }

    fun startBillingFlow(activity: android.app.Activity) {
        billingManager.launchBillingFlow(activity, "raymi_pro_subscription")
    }

    private fun upgradeToProAfterValidation() {
        // [C-02] Eliminada escritura directa desde el cliente por seguridad.
        // La promoción a PRO ahora es gestionada por Cloud Functions tras validar el recibo.
        _uiState.update { it.copy(
            isLoading = false, 
            infoMessage = "Validando su compra... El plan PRO se activará en unos instantes."
        ) }
        // Se recomienda refrescar el plan después de unos segundos
        viewModelScope.launch {
            delay(5000)
            cargarPlanActual()
        }
    }
}

data class PlansUiState(
    val currentPlan: UserPlan? = null,
    val freePrice: String = PlanType.PRICE_FREE.toString(),
    val proPrice: String = PlanType.PRICE_PRO.toString(),
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val infoMessage: String? = null
)
