package com.raymi.app.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

// Rutas de navegación
sealed class Screen(val route: String) {
    // Auth
    object Login : Screen("login")
    object WorkspaceSelection : Screen("workspace_selection")
    object WorkspaceCreate : Screen("workspace_create")

    // Main
    object Dashboard : Screen("dashboard")
    object Clientes : Screen("clientes")
    object Items : Screen("items")
    object ItemCreate : Screen("item_create")
    object Categorias : Screen("categorias")
    object Alquileres : Screen("alquileres")
    object Historial : Screen("historial")
    object Profile : Screen("profile")
    object BusinessSettings : Screen("business_settings")
    object Plans : Screen("plans")
    object EditProfile : Screen("edit_profile")
    object Security : Screen("security")
    object HelpCenter : Screen("help_center")

    // Detalle
    object ClienteDetalle : Screen("cliente/{clienteId}") {
        fun createRoute(clienteId: String) = "cliente/$clienteId"
    }
    object ItemDetalle : Screen("item/{itemId}") {
        fun createRoute(itemId: String) = "item/$itemId"
    }
    object ItemEdit : Screen("item_edit/{itemId}") {
        fun createRoute(itemId: String) = "item_edit/$itemId"
    }
    object ItemMantenimiento : Screen("item_maintenance/{itemId}") {
        fun createRoute(itemId: String) = "item_maintenance/$itemId"
    }
    object AlquilerDetalle : Screen("alquiler/{alquilerId}") {
        fun createRoute(alquilerId: String) = "alquiler/$alquilerId"
    }
    object GenerateComprobante : Screen("generate_comprobante/{alquilerId}") {
        fun createRoute(alquilerId: String) = "generate_comprobante/$alquilerId"
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
        icon = Icons.Outlined.Dashboard,
        selectedIcon = Icons.Filled.Dashboard
    )

    object Clientes : BottomNavItem(
        route = Screen.Clientes.route,
        title = "Clientes",
        icon = Icons.Outlined.People,
        selectedIcon = Icons.Filled.People
    )

    object Items : BottomNavItem(
        route = Screen.Items.route,
        title = "Inventario",
        icon = Icons.Outlined.Checkroom,
        selectedIcon = Icons.Filled.Checkroom
    )

    object Alquileres : BottomNavItem(
        route = Screen.Alquileres.route,
        title = "Alquileres",
        icon = Icons.Outlined.ShoppingCart,
        selectedIcon = Icons.Filled.ShoppingCart
    )

    object Historial : BottomNavItem(
        route = Screen.Historial.route,
        title = "Historial",
        icon = Icons.Outlined.History,
        selectedIcon = Icons.Filled.History
    )

    object Profile : BottomNavItem(
        route = Screen.Profile.route,
        title = "Perfil",
        icon = Icons.Outlined.AccountCircle,
        selectedIcon = Icons.Filled.AccountCircle
    )
}

// Lista de items del bottom navigation
val bottomNavItems = listOf(
    BottomNavItem.Dashboard,
    BottomNavItem.Clientes,
    BottomNavItem.Items,
    BottomNavItem.Alquileres,
    BottomNavItem.Historial,
    BottomNavItem.Profile
)
