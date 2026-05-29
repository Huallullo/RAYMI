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
import com.raymi.app.presentation.plans.PlansScreen
import com.raymi.app.presentation.profile.ProfileScreen
import com.raymi.app.presentation.profile.edit.EditProfileScreen
import com.raymi.app.presentation.settings.BusinessSettingsScreen
import com.raymi.app.presentation.workspace.CreateWorkspaceScreen
import com.raymi.app.presentation.workspace.WorkspaceSelectionScreen

/**
 * Grafo de navegación principal de la aplicación
 * Define todas las rutas y transiciones entre pantallas
 */
@Composable
fun RaymiNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ========== AUTENTICACIÓN ==========

        /**
         * Pantalla de inicio de sesión
         */
        composable(route = Screen.Login.route) {
            LoginScreen(
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    // Navegar a onboarding si es necesario
                },
                onNavigateToWorkspaceSelection = {
                    navController.navigate(Screen.WorkspaceSelection.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        /**
         * Selección de Workspace
         */
        composable(route = Screen.WorkspaceSelection.route) {
            WorkspaceSelectionScreen(
                onWorkspaceSelected = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.WorkspaceSelection.route) { inclusive = true }
                    }
                },
                onCreateWorkspace = {
                    navController.navigate(Screen.WorkspaceCreate.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        /**
         * Pantalla para crear un nuevo negocio
         */
        composable(route = Screen.WorkspaceCreate.route) {
            CreateWorkspaceScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSuccess = {
                    navController.popBackStack()
                }
            )
        }

        // ========== PANTALLAS PRINCIPALES ==========

        /**
         * Dashboard - Pantalla principal con estadísticas
         */
        composable(route = Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToClientes = {
                    navController.navigate(BottomNavItem.Clientes.route)
                },
                onNavigateToItems = {
                    navController.navigate(BottomNavItem.Items.route)
                },
                onNavigateToAlquileres = {
                    navController.navigate(BottomNavItem.Alquileres.route)
                }
            )
        }

        /**
         * Clientes - Lista de clientes
         */
        composable(route = Screen.Clientes.route) {
            val navigatedFromResult = navController.currentBackStackEntry
                ?.savedStateHandle
                ?.get<Boolean>("refresh") ?: false
            ClientesScreen(
                onClienteClick = { clienteId ->
                    navController.navigate(Screen.ClienteDetalle.createRoute(clienteId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                navigatedFromResult = navigatedFromResult
            )
        }

        /**
         * Detalle de Cliente
         */
        composable(
            route = Screen.ClienteDetalle.route,
            arguments = listOf(
                navArgument("clienteId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val clienteId = backStackEntry.arguments?.getString("clienteId") ?: ""
            ClienteDetailScreen(
                clienteId = clienteId,
                onNavigateBack = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("refresh", true)
                    navController.popBackStack()
                },
                onNavigateToAlquiler = { alquilerId ->
                    navController.navigate(Screen.AlquilerDetalle.createRoute(alquilerId))
                }
            )
        }

        /**
         * Inventario Genérico (SaaS)
         */
        composable(route = Screen.Items.route) {
            ItemsScreen(
                onItemClick = { itemId ->
                    navController.navigate(Screen.ItemDetalle.createRoute(itemId))
                },
                onAddItem = {
                    navController.navigate(Screen.ItemCreate.route)
                },
                onNavigateToCategorias = {
                    navController.navigate(Screen.Categorias.route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        /**
         * Detalle de Ítem
         */
        composable(
            route = Screen.ItemDetalle.route,
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
            ItemDetailScreen(
                itemId = itemId,
                onNavigateBack = { navController.popBackStack() },
                onEditItem = { id ->
                    navController.navigate(Screen.ItemEdit.createRoute(id))
                },
                onRentItem = { id ->
                    navController.navigate("create_alquiler?itemId=$id")
                }
            )
        }

        /**
         * Edición de Ítem
         */
        composable(
            route = Screen.ItemEdit.route,
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
            EditItemScreen(
                itemId = itemId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        /**
         * Registro de Nuevo Ítem
         */
        composable(route = Screen.ItemCreate.route) {
            AddItemScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        /**
         * Gestión de Categorías
         */
        composable(route = Screen.Categorias.route) {
            CategoriasScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        /**
         * Alquileres - Lista de alquileres
         */
        composable(route = Screen.Alquileres.route) {
            AlquileresScreen(
                onAlquilerClick = { alquilerId ->
                    navController.navigate(Screen.AlquilerDetalle.createRoute(alquilerId))
                },
                onCreateAlquiler = {
                    navController.navigate("create_alquiler")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        /**
         * Crear nuevo alquiler (Soporta itemId opcional)
         */
        composable(
            route = "create_alquiler?itemId={itemId}",
            arguments = listOf(
                navArgument("itemId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            CreateAlquilerScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        /**
         * Detalle de Alquiler
         */
        composable(
            route = Screen.AlquilerDetalle.route,
            arguments = listOf(
                navArgument("alquilerId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val alquilerId = backStackEntry.arguments?.getString("alquilerId") ?: ""
            AlquilerDetailScreen(
                alquilerId = alquilerId,
                onNavigateBack = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("refresh", true)
                    navController.popBackStack()
                },
                onGenerateComprobante = { id ->
                    navController.navigate(Screen.GenerateComprobante.createRoute(id))
                }
            )
        }

        /**
         * Generar Comprobante
         */
        composable(
            route = Screen.GenerateComprobante.route,
            arguments = listOf(navArgument("alquilerId") { type = NavType.StringType })
        ) {
            GenerateComprobanteScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlans = { navController.navigate(Screen.Plans.route) }
            )
        }

        /**
         * Historial - Historial de operaciones
         */
        composable(route = Screen.Historial.route) {
            HistorialScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        /**
         * Ajustes del Negocio (Personalización SaaS)
         */
        composable(route = Screen.BusinessSettings.route) {
            BusinessSettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        /**
         * Perfil de Usuario
         */
        composable(route = Screen.Profile.route) {
            ProfileScreen(
                onNavigateToPlans = { navController.navigate(Screen.Plans.route) },
                onNavigateToSettings = { navController.navigate(Screen.BusinessSettings.route) },
                onNavigateToEditProfile = { navController.navigate(Screen.EditProfile.route) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        /**
         * Pantalla de Edición de Perfil
         */
        composable(route = Screen.EditProfile.route) {
            EditProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        /**
         * Pantalla de Planes
         */
        composable(route = Screen.Plans.route) {
            PlansScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
