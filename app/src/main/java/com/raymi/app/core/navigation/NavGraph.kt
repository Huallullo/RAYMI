package com.raymi.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.raymi.app.presentation.alquileres.AlquilerDetailScreen
import com.raymi.app.presentation.alquileres.AlquileresScreen
import com.raymi.app.presentation.alquileres.CreateAlquilerScreen
import com.raymi.app.presentation.comprobantes.GenerateComprobanteScreen
import com.raymi.app.presentation.auth.LoginScreen
import com.raymi.app.presentation.categorias.CategoriasScreen
import com.raymi.app.presentation.clientes.ClienteDetailScreen
import com.raymi.app.presentation.clientes.ClientesScreen
import com.raymi.app.presentation.dashboard.DashboardScreen
import com.raymi.app.presentation.historial.HistorialScreen
import com.raymi.app.presentation.items.AddItemScreen
import com.raymi.app.presentation.items.EditItemScreen
import com.raymi.app.presentation.items.ItemDetailScreen
import com.raymi.app.presentation.items.ItemsScreen
import com.raymi.app.presentation.items.mantenimiento.MantenimientoScreen
import com.raymi.app.presentation.plans.PlansScreen
import com.raymi.app.presentation.profile.ProfileScreen
import com.raymi.app.presentation.profile.edit.EditProfileScreen
import com.raymi.app.presentation.profile.help.HelpCenterScreen
import com.raymi.app.presentation.profile.security.SecurityScreen
import com.raymi.app.presentation.settings.BusinessSettingsScreen
import com.raymi.app.presentation.workspace.CreateWorkspaceScreen
import com.raymi.app.presentation.workspace.WorkspaceSelectionScreen
import com.raymi.app.core.ads.AdInterstitialManager

@Composable
fun RaymiNavGraph(
    navController: NavHostController,
    startDestination: String,
    adInterstitialManager: AdInterstitialManager
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = Screen.Login.route) {
            LoginScreen(
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Login.route) { inclusive = true } }
                },
                onNavigateToOnboarding = { },
                onNavigateToWorkspaceSelection = {
                    navController.navigate(Screen.WorkspaceSelection.route) { popUpTo(Screen.Login.route) { inclusive = true } }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.WorkspaceSelection.route) {
            WorkspaceSelectionScreen(
                onWorkspaceSelected = {
                    navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.WorkspaceSelection.route) { inclusive = true } }
                },
                onCreateWorkspace = { navController.navigate(Screen.WorkspaceCreate.route) },
                onNavigateToPlans = { navController.navigate(Screen.Plans.route) },
                onLogout = { navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } },
                onNavigateBack = { if (navController.previousBackStackEntry != null) navController.popBackStack() }
            )
        }

        composable(route = Screen.WorkspaceCreate.route) {
            CreateWorkspaceScreen(onNavigateBack = { navController.popBackStack() }, onSuccess = { navController.popBackStack() })
        }

        composable(route = Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToClientes = { navController.navigate(BottomNavItem.Clientes.route) },
                onNavigateToItems = { navController.navigate(BottomNavItem.Items.route) },
                onNavigateToAlquileres = { navController.navigate(BottomNavItem.Alquileres.route) }
            )
        }

        composable(route = Screen.Clientes.route) {
            val refresh = navController.currentBackStackEntry?.savedStateHandle?.get<Boolean>("refresh") ?: false
            ClientesScreen(
                onClienteClick = { id -> navController.navigate(Screen.ClienteDetalle.createRoute(id)) },
                onNavigateBack = { navController.popBackStack() },
                navigatedFromResult = refresh
            )
        }

        composable(route = Screen.ClienteDetalle.route, arguments = listOf(navArgument("clienteId") { type = NavType.StringType })) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("clienteId") ?: ""
            ClienteDetailScreen(
                clienteId = id,
                onNavigateBack = {
                    navController.previousBackStackEntry?.savedStateHandle?.set("refresh", true)
                    navController.popBackStack()
                },
                onNavigateToAlquiler = { alqId -> navController.navigate(Screen.AlquilerDetalle.createRoute(alqId)) }
            )
        }

        composable(route = Screen.Items.route) {
            ItemsScreen(
                onItemClick = { id -> navController.navigate(Screen.ItemDetalle.createRoute(id)) },
                onAddItem = { navController.navigate(Screen.ItemCreate.route) },
                onNavigateToCategorias = { navController.navigate(Screen.Categorias.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.ItemDetalle.route, arguments = listOf(navArgument("itemId") { type = NavType.StringType })) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("itemId") ?: ""
            ItemDetailScreen(
                itemId = id,
                onNavigateBack = { navController.popBackStack() },
                onEditItem = { itemId -> navController.navigate(Screen.ItemEdit.createRoute(itemId)) },
                onRentItem = { itemId -> navController.navigate("create_alquiler?itemId=$itemId") },
                onNavigateToMaintenance = { itemId -> navController.navigate(Screen.ItemMantenimiento.createRoute(itemId)) }
            )
        }

        composable(route = Screen.ItemMantenimiento.route, arguments = listOf(navArgument("itemId") { type = NavType.StringType })) {
            MantenimientoScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(route = Screen.ItemEdit.route, arguments = listOf(navArgument("itemId") { type = NavType.StringType })) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("itemId") ?: ""
            EditItemScreen(itemId = id, onNavigateBack = { navController.popBackStack() })
        }

        composable(route = Screen.ItemCreate.route) {
            AddItemScreen(onNavigateBack = { navController.popBackStack() }, onNavigateToPlans = { navController.navigate(Screen.Plans.route) })
        }

        composable(route = Screen.Categorias.route) {
            CategoriasScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(route = Screen.Alquileres.route) {
            AlquileresScreen(
                onAlquilerClick = { id -> navController.navigate(Screen.AlquilerDetalle.createRoute(id)) },
                onCreateAlquiler = { navController.navigate("create_alquiler") },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = "create_alquiler?itemId={itemId}", arguments = listOf(navArgument("itemId") { type = NavType.StringType; nullable = true; defaultValue = null })) {
            CreateAlquilerScreen(adInterstitialManager = adInterstitialManager, onNavigateBack = { navController.popBackStack() })
        }

        composable(route = Screen.AlquilerDetalle.route, arguments = listOf(navArgument("alquilerId") { type = NavType.StringType })) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("alquilerId") ?: ""
            AlquilerDetailScreen(
                alquilerId = id,
                onNavigateBack = {
                    navController.previousBackStackEntry?.savedStateHandle?.set("refresh", true)
                    navController.popBackStack()
                },
                onGenerateComprobante = { alqId -> navController.navigate(Screen.GenerateComprobante.createRoute(alqId)) }
            )
        }

        composable(route = Screen.GenerateComprobante.route, arguments = listOf(navArgument("alquilerId") { type = NavType.StringType })) {
            GenerateComprobanteScreen(onNavigateBack = { navController.popBackStack() }, onNavigateToPlans = { navController.navigate(Screen.Plans.route) })
        }

        composable(route = Screen.Historial.route) { HistorialScreen(onNavigateBack = { navController.popBackStack() }) }
        composable(route = Screen.BusinessSettings.route) { BusinessSettingsScreen(onNavigateBack = { navController.popBackStack() }) }
        composable(route = Screen.Profile.route) {
            ProfileScreen(
                onNavigateToPlans = { navController.navigate(Screen.Plans.route) },
                onNavigateToSettings = { navController.navigate(Screen.BusinessSettings.route) },
                onNavigateToEditProfile = { navController.navigate(Screen.EditProfile.route) },
                onNavigateToSecurity = { navController.navigate(Screen.Security.route) },
                onNavigateToHelpCenter = { navController.navigate(Screen.HelpCenter.route) },
                onNavigateToWorkspaceSelection = { navController.navigate(Screen.WorkspaceSelection.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } } },
                onLogout = { navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } }
            )
        }

        composable(route = Screen.EditProfile.route) { EditProfileScreen(onNavigateBack = { navController.popBackStack() }) }
        composable(route = Screen.HelpCenter.route) { HelpCenterScreen(onNavigateBack = { navController.popBackStack() }) }
        composable(route = Screen.Security.route) { SecurityScreen(onNavigateBack = { navController.popBackStack() }) }
        composable(route = Screen.Plans.route) { PlansScreen(onNavigateBack = { navController.popBackStack() }) }
    }
}
