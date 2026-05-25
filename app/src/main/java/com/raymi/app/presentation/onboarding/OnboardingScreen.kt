package com.raymi.app.presentation.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.painterResource
import com.raymi.app.R

data class AtributoPersonalizado(
    val nombre: String,
    val etiqueta: String,
    val requerido: Boolean = false,
    val paraBusqueda: Boolean = true
)

/**
 * Experiencia de Inicio (SaaS Onboarding) Premium.
 * Guía al usuario para configurar su negocio de forma visual y atractiva.
 */
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentStep by remember { mutableStateOf(0) }
    
    val rubros = listOf("Vestuarios", "Equipos Médicos", "Herramientas", "Vehículos", "Mobiliario")
    var selectedRubro by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) onComplete()
    }

    Scaffold(
        bottomBar = {
            OnboardingBottomBar(
                currentStep = currentStep,
                canContinue = selectedRubro.isNotEmpty() || currentStep != 0,
                onNext = { 
                    if (currentStep < 2) currentStep++ 
                    else viewModel.guardarConfiguracion(selectedRubro, "Ítem", "Ítems", emptyList<AtributoPersonalizado>())
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_raymi_logo),
                    contentDescription = "Logo RAYMI",
                    modifier = Modifier.size(60.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "RAYMI",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
            
            LinearProgressIndicator(
                progress = { (currentStep + 1) / 3f },
                modifier = Modifier.fillMaxWidth().clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "onboarding_step"
            ) { step ->
                when(step) {
                    0 -> RubroSelectionStep(selectedRubro) { selectedRubro = it }
                    1 -> FeatureDiscoveryStep()
                    2 -> ReadyStep(uiState.isLoading)
                }
            }
        }
    }
}

@Composable
fun RubroSelectionStep(selected: String, onSelect: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("¿Qué negocio vas a gestionar?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Text("Adaptaremos las herramientas a tu rubro.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        val options = listOf(
            Triple("Vestuarios", Icons.Default.Checkroom, "Ropa y accesorios"),
            Triple("Equipos", Icons.Default.PrecisionManufacturing, "Cámaras, drones, etc"),
            Triple("Herramientas", Icons.Default.Build, "Construcción y taller"),
            Triple("Vehículos", Icons.Default.DirectionsCar, "Autos, motos, bicis")
        )

        options.forEach { (name, icon, desc) ->
            RubroCard(name, icon, desc, selected == name) { onSelect(name) }
        }
    }
}

@Composable
fun FeatureDiscoveryStep() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text("Todo bajo control", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        
        FeatureItem(Icons.Default.CloudSync, "Sincronización en la nube", "Accede a tus datos desde cualquier dispositivo.")
        FeatureItem(Icons.Default.Description, "Reportes Financieros", "Genera boletas y balances anuales en PDF.")
        FeatureItem(Icons.Default.Smartphone, "Tickets vía WhatsApp", "Envía confirmaciones directas a tus clientes.")
    }
}

@Composable
fun ReadyStep(isLoading: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Text("¡Estás listo para despegar!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text("Haremos que tu negocio de alquiler sea más rentable y organizado.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        if (isLoading) CircularProgressIndicator()
    }
}

@Composable
fun FeatureItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) }
        }
        Column {
            Text(title, fontWeight = FontWeight.Bold)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun RubroCard(name: String, icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, null, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Column {
                Text(name, fontWeight = FontWeight.Bold)
                Text(desc, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.weight(1f))
            if (isSelected) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun OnboardingBottomBar(currentStep: Int, canContinue: Boolean, onNext: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = canContinue,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Text(if (currentStep == 2) "Empezar Ahora" else "Continuar", fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
        }
    }
}
