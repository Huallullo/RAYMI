package com.raymi.app.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

// Rutas de navegación
sealed class Screen(val route: String) {
    // Auth
    object Splash : Screen("splash")
    object Login : Screen("login")

    // Main
    object Dashboard : Screen("dashboard")
    object Clientes : Screen("clientes")
    object Vestuarios : Screen("vestuarios")
    object Alquileres : Screen("alquileres")
    object Historial : Screen("historial")
    object Profile : Screen("profile")

    // Detalle
    object ClienteDetalle : Screen("cliente/{clienteId}") {
        fun createRoute(clienteId: String) = "cliente/$clienteId"
    }
    object VestuarioDetalle : Screen("vestuario/{vestuarioId}") {
        fun createRoute(vestuarioId: String) = "vestuario/$vestuarioId"
    }
    object AlquilerDetalle : Screen("alquiler/{alquilerId}") {
        fun createRoute(alquilerId: String) = "alquiler/$alquilerId"
    }
}
// Items del Bottom Navigation
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon
) {
    object Dashboard : BottomNavItem(
        route = Screen.Dashboard.route,
        title = "Inicio",
        icon = Icons.Default.Dashboard,
        selectedIcon = Icons.Filled.Dashboard
    )

    object Clientes : BottomNavItem(
        route = Screen.Clientes.route,
        title = "Clientes",
        icon = Icons.Default.People,
        selectedIcon = Icons.Filled.People
    )

    object Vestuarios : BottomNavItem(
        route = Screen.Vestuarios.route,
        title = "Vestuarios",
        icon = Icons.Default.Checkroom,
        selectedIcon = Icons.Filled.Checkroom
    )

    object Alquileres : BottomNavItem(
        route = Screen.Alquileres.route,
        title = "Alquileres",
        icon = Icons.Default.ShoppingCart,
        selectedIcon = Icons.Filled.ShoppingCart
    )

    object Historial : BottomNavItem(
        route = Screen.Historial.route,
        title = "Historial",
        icon = Icons.Default.History,
        selectedIcon = Icons.Filled.History
    )
}

// Lista de items del bottom navigation
val bottomNavItems = listOf(
    BottomNavItem.Dashboard,
    BottomNavItem.Clientes,
    BottomNavItem.Vestuarios,
    BottomNavItem.Alquileres,
    BottomNavItem.Historial
)