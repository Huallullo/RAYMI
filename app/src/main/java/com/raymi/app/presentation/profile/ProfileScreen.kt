package com.raymi.app.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raymi.app.domain.model.PlanType
import com.raymi.app.presentation.components.AvatarWithInitials
import com.raymi.app.presentation.components.RaymiLoadingIndicator
import com.raymi.app.core.lang.LocalRaymiStrings

/**
 * Pantalla de Perfil de Usuario Premium.
 * Diseño Senior: Enfoque en la gestión de cuenta, estatus de suscripción y seguridad.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToPlans: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToHelpCenter: () -> Unit,
    onNavigateToWorkspaceSelection: () -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val strings = LocalRaymiStrings.current

    if (uiState.loggedOut) {
        LaunchedEffect(Unit) { onLogout() }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(strings.profile, fontWeight = FontWeight.Black) }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.isLoading) {
                RaymiLoadingIndicator(message = strings.loading)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    // 1. Identidad del Usuario (Avatar y Nombre)
                    UserIdentitySection(
                        name = uiState.user?.displayName ?: "User",
                        email = uiState.user?.email ?: ""
                    )

                    // 2. Estado de Suscripción SaaS (Factor de Prestigio)
                    SubscriptionStatusCard(
                        planType = uiState.plan?.plan ?: PlanType.FREE,
                        onUpgradeClick = onNavigateToPlans
                    )

                    // 3. Opciones de Cuenta y Seguridad
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(strings.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        
                        AccountOptionItem(
                            title = strings.myBusiness,
                            subtitle = strings.myBusinessSub,
                            icon = Icons.Default.Store,
                            onClick = onNavigateToSettings
                        )

                        AccountOptionItem(
                            title = strings.changeBusiness,
                            subtitle = strings.changeBusinessSub,
                            icon = Icons.Default.SwitchAccount,
                            onClick = onNavigateToWorkspaceSelection
                        )

                        AccountOptionItem(
                            title = strings.personalData,
                            subtitle = strings.personalDataSub,
                            icon = Icons.Default.Badge,
                            onClick = onNavigateToEditProfile
                        )
                        
                        AccountOptionItem(
                            title = strings.subscription,
                            subtitle = strings.subscriptionSub,
                            icon = Icons.Default.CreditCard,
                            onClick = onNavigateToPlans
                        )
                        
                        AccountOptionItem(
                            title = strings.security,
                            subtitle = strings.securitySub,
                            icon = Icons.Default.Security,
                            onClick = onNavigateToSecurity
                        )
                    }

                    // 4. Soporte y Ayuda
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(strings.helpCenter, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        AccountOptionItem(
                            title = strings.helpCenter,
                            subtitle = strings.helpCenterSub,
                            icon = Icons.AutoMirrored.Filled.HelpOutline,
                            onClick = onNavigateToHelpCenter
                        )
                    }

                    // 5. Botón de Cerrar Sesión
                    OutlinedButton(
                        onClick = { viewModel.cerrarSesion() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text(strings.logout, fontWeight = FontWeight.Bold)
                    }
                    
                    Text(
                        "${strings.appName} SaaS v2.5 • 2026",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
fun UserIdentitySection(name: String, email: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AvatarWithInitials(
            initials = name.take(1).uppercase(),
            size = 90,
            backgroundColor = MaterialTheme.colorScheme.primary,
            textColor = Color.White
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SubscriptionStatusCard(planType: PlanType, onUpgradeClick: () -> Unit) {
    val isPro = planType == PlanType.PRO
    val strings = LocalRaymiStrings.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = if (isPro) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (!isPro) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)) else null
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    strings.statusAccount, 
                    style = MaterialTheme.typography.labelSmall, 
                    color = if (isPro) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (isPro) strings.proBusiness else strings.basicPlan, 
                    style = MaterialTheme.typography.titleLarge, 
                    fontWeight = FontWeight.Black,
                    color = if (isPro) Color.White else MaterialTheme.colorScheme.primary
                )
            }
            
            if (!isPro) {
                Button(
                    onClick = onUpgradeClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(strings.bePro, fontWeight = FontWeight.Bold)
                }
            } else {
                Icon(Icons.Default.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }
    }
}

@Composable
fun AccountOptionItem(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
        }
    }
}
