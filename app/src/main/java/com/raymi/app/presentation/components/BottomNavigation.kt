package com.raymi.app.presentation.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.raymi.app.core.navigation.bottomNavItems

/**
 * Componente de navegación inferior
 * Muestra las opciones principales de navegación
 */
@Composable
fun RaymiBottomNavigation(
    navController: NavController,
    currentDestination: NavDestination?
) {
    val strings = com.raymi.app.core.lang.LocalRaymiStrings.current
    
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentDestination?.hierarchy?.any {
                it.route == item.route
            } == true

            val title = when(item) {
                com.raymi.app.core.navigation.BottomNavItem.Dashboard -> strings.dashboard
                com.raymi.app.core.navigation.BottomNavItem.Clientes -> strings.clients
                com.raymi.app.core.navigation.BottomNavItem.Items -> strings.inventory
                com.raymi.app.core.navigation.BottomNavItem.Alquileres -> strings.rentals
                com.raymi.app.core.navigation.BottomNavItem.Historial -> strings.history
                com.raymi.app.core.navigation.BottomNavItem.Profile -> strings.profile
            }

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.icon,
                        contentDescription = title
                    )
                },
                label = {
                    Text(
                        text = title,
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
