package com.raymi.app.presentation.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raymi.app.domain.model.PlanType
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.raymi.app.core.lang.LocalRaymiStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen(
    viewModel: PlansViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val strings = LocalRaymiStrings.current
    val activity = remember(context) { context.findActivity() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(strings.subscription, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Text(
                if (strings is com.raymi.app.core.lang.SpanishStrings) "Potencia tu negocio con herramientas de nivel corporativo" else "Power up your business with corporate-level tools",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 1. Tarjeta Plan FREE
            PlanCard(
                name = if (strings is com.raymi.app.core.lang.SpanishStrings) "Plan Inicial" else "Starter Plan",
                price = "S/. ${uiState.freePrice}",
                features = if (strings is com.raymi.app.core.lang.SpanishStrings) listOf(
                    "Tickets Ilimitados (Locales)",
                    "Hasta 1 Negocio (Workspace)",
                    "Hasta 30 Productos / Items",
                    "Hasta 40 Clientes registrados",
                    "Incluye Anuncios"
                ) else listOf(
                    "Unlimited Tickets (Local)",
                    "Up to 1 Business (Workspace)",
                    "Up to 30 Products / Items",
                    "Up to 40 Registered Clients",
                    "Includes Ads"
                ),
                isSelected = uiState.currentPlan?.plan == PlanType.FREE,
                buttonText = if (uiState.currentPlan?.plan == PlanType.FREE) (if (strings is com.raymi.app.core.lang.SpanishStrings) "Plan Actual" else "Current Plan") else "Select",
                onAction = {}
            )

            // 2. Tarjeta Plan PRO
            PlanCard(
                name = "Raymi Pro Business",
                price = "S/. ${uiState.proPrice} / $5.40 USD",
                features = if (strings is com.raymi.app.core.lang.SpanishStrings) listOf(
                    "Boletas y Facturas Ilimitadas",
                    "Validez SUNAT (Nubefact/ApiPeru)",
                    "Negocios Ilimitados",
                    "Productos Ilimitados",
                    "Sin Anuncios",
                    "Reportes Financieros PDF",
                    "Soporte Prioritario WhatsApp"
                ) else listOf(
                    "Unlimited Bills & Invoices",
                    "SUNAT Validity (Nubefact/ApiPeru)",
                    "Unlimited Businesses",
                    "Unlimited Products",
                    "No Advertisements",
                    "PDF Financial Reports",
                    "Priority WhatsApp Support"
                ),
                isSelected = uiState.currentPlan?.plan == PlanType.PRO,
                isPremium = true,
                buttonText = if (uiState.currentPlan?.plan == PlanType.PRO) (if (strings is com.raymi.app.core.lang.SpanishStrings) "Plan Actual" else "Current Plan") else strings.bePro,
                onAction = { activity?.let { viewModel.startBillingFlow(it) } },
                isLoading = uiState.isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                if (strings is com.raymi.app.core.lang.SpanishStrings) "Cancela en cualquier momento. Sin contratos forzosos." else "Cancel anytime. No forced contracts.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
fun PlanCard(
    name: String,
    price: String,
    features: List<String>,
    isSelected: Boolean,
    isPremium: Boolean = false,
    buttonText: String,
    onAction: () -> Unit,
    isLoading: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = if (isPremium) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shadowElevation = if (isPremium) 8.dp else 2.dp
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (isPremium) Color.White else MaterialTheme.colorScheme.onSurface)
                if (isPremium) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                }
            }
            
            Text(price, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = if (isPremium) Color.White else MaterialTheme.colorScheme.primary)
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                features.forEach { feature ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (isPremium) Color.White else Color(0xFF4CAF50))
                        Text(feature, style = MaterialTheme.typography.bodyMedium, color = if (isPremium) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            
            Button(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPremium) Color.White else MaterialTheme.colorScheme.primary,
                    contentColor = if (isPremium) MaterialTheme.colorScheme.primary else Color.White
                ),
                shape = MaterialTheme.shapes.large,
                enabled = !isSelected && !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary)
                else Text(buttonText, fontWeight = FontWeight.Bold)
            }
        }
    }
}
