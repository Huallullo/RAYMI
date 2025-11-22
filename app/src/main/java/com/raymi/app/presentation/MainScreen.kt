package com.raymi.app.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.raymi.app.core.navigation.*

/**
 * Pantalla principal que contiene el Scaffold con Bottom Navigation
 * Maneja la navegación entre las pantallas principales de la app
 */
@Composable
fun MainScreen(
    isUserAuthenticated: Boolean
) {
    val navController = rememberNavController()

    // Determinar la ruta inicial según el estado de autenticación
    val startDestination = if (isUserAuthenticated) {
        Screen.Dashboard.route
    } else {
        Screen.Login.route
    }

    // Estado actual de navegación
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Determinar si mostrar bottom navigation
    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.route
                        } == true

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.icon,
                                    contentDescription = item.title
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    // Pop hasta el inicio del grafo para evitar stack grande
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    // Evitar múltiples copias de la misma pantalla
                                    launchSingleTop = true
                                    // Restaurar estado al volver
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        // Grafo de navegación principal
        Box(modifier = Modifier.padding(paddingValues)) {
            RaymiNavGraph(
                navController = navController,
                startDestination = startDestination
            )
        }
    }
}