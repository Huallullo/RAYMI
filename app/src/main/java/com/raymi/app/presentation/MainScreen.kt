package com.raymi.app.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.raymi.app.core.lang.EnglishStrings
import com.raymi.app.core.lang.LocalRaymiStrings
import com.raymi.app.core.lang.SpanishStrings
import com.raymi.app.core.navigation.*
import com.raymi.app.core.ads.AdInterstitialManager

/**
 * Pantalla contenedora principal (Scaffold Maestro).
 * Diseño Futurista: Barra de navegación flotante con efectos de elevación y animaciones suaves.
 */
@Composable
fun MainScreen(
    workspaceManager: com.raymi.app.core.workspace.WorkspaceManager,
    adInterstitialManager: AdInterstitialManager, // Pasado desde MainActivity
    viewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }

    val currentWorkspace by workspaceManager.currentWorkspace.collectAsStateWithLifecycle()
    val currentLanguage by workspaceManager.currentLanguage.collectAsStateWithLifecycle()
    
    // Motor de decisión de idioma robusto
    val strings = remember(currentWorkspace?.idioma, currentLanguage, currentDestination?.route) {
        val route = currentDestination?.route
        val isAuthFlow = route == Screen.Login.route || 
                         route == Screen.WorkspaceSelection.route ||
                         route == Screen.WorkspaceCreate.route

        val finalLang = if (isAuthFlow) currentLanguage else (currentWorkspace?.idioma ?: currentLanguage)
        if (finalLang == "en") EnglishStrings() else SpanishStrings()
    }

    CompositionLocalProvider(LocalRaymiStrings provides strings) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (!isConnected) {
                    Surface(
                        color = Color(0xFFFFF7ED), // Ambar muy claro
                        modifier = Modifier.fillMaxWidth().statusBarsPadding(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFED7AA))
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CloudOff, null, modifier = Modifier.size(14.dp), tint = Color(0xFFC2410C))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (strings is SpanishStrings) "Modo sin conexión" else "Offline mode",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFC2410C),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                // 1. EL CONTENIDO OCUPA TODO EL ESPACIO (Pasa por debajo de la barra)
                Box(modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding())) {
                    RaymiNavGraph(
                        navController = navController,
                        startDestination = Screen.Login.route,
                        adInterstitialManager = adInterstitialManager
                    )
                }
                
                // 2. LA BARRA ES UNA CÁPSULA FLOTANTE MINIMALISTA (Overlay Real)
                if (showBottomBar) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.navigationBars) // Solo el espacio de los botones del sistema
                            .padding(bottom = 4.dp) // Pegado al borde inferior del sistema para ganar espacio
                    ) {
                        FuturisticBottomBar(
                            navController = navController,
                            currentDestination = currentDestination,
                            strings = strings
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FuturisticBottomBar(
    navController: androidx.navigation.NavHostController,
    currentDestination: androidx.navigation.NavDestination?,
    strings: com.raymi.app.core.lang.RaymiStrings
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 20.dp) // Margen lateral de la cápsula para que flote
            .shadow(16.dp, shape = CircleShape)
            .clip(CircleShape),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 6.dp, vertical = 4.dp) // Reducción de grosor
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { item ->
                val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                
                val title = when(item) {
                    BottomNavItem.Dashboard -> strings.dashboard
                    BottomNavItem.Clientes -> strings.clients
                    BottomNavItem.Items -> strings.inventory
                    BottomNavItem.Alquileres -> strings.rentals
                    BottomNavItem.Historial -> strings.history
                    BottomNavItem.Profile -> strings.profile
                }

                FuturisticNavItem(
                    item = item,
                    title = title,
                    selected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun FuturisticNavItem(
    item: BottomNavItem,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val duration = 400
    
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(duration),
        label = "color"
    )

    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(duration),
        label = "bgColor"
    )

    Box(
        modifier = Modifier
            .height(40.dp) // Más delgado
            .clip(CircleShape)
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp), // Menos padding lateral
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            
            AnimatedVisibility(
                visible = selected,
                enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
                exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut()
            ) {
                Text(
                    text = title,
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = contentColor,
                    maxLines = 1
                )
            }
        }
    }
}
