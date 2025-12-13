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
import com.raymi.app.presentation.auth.LoginScreen
import com.raymi.app.presentation.clientes.ClienteDetailScreen
import com.raymi.app.presentation.clientes.ClientesScreen
import com.raymi.app.presentation.dashboard.DashboardScreen
import com.raymi.app.presentation.historial.HistorialScreen
import com.raymi.app.presentation.vestuarios.VestuarioDetailScreen
import com.raymi.app.presentation.vestuarios.VestuariosScreen

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
                onLoginSuccess = {
                    // Navegar al dashboard y limpiar el back stack
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
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
                    navController.navigate(Screen.Clientes.route)
                },
                onNavigateToVestuarios = {
                    navController.navigate(Screen.Vestuarios.route)
                },
                onNavigateToAlquileres = {
                    navController.navigate(Screen.Alquileres.route)
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
         * Vestuarios - Lista de vestuarios
         */
        composable(route = Screen.Vestuarios.route) {
            val navigatedFromResult = navController.currentBackStackEntry
                ?.savedStateHandle
                ?.get<Boolean>("refresh") ?: false
            VestuariosScreen(
                onVestuarioClick = { vestuarioId ->
                    navController.navigate(Screen.VestuarioDetalle.createRoute(vestuarioId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                navigatedFromResult = navigatedFromResult
            )
        }

        /**
         * Detalle de Vestuario
         */
        composable(
            route = Screen.VestuarioDetalle.route,
            arguments = listOf(
                navArgument("vestuarioId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val vestuarioId = backStackEntry.arguments?.getString("vestuarioId") ?: ""
            VestuarioDetailScreen(
                vestuarioId = vestuarioId,
                onNavigateBack = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("refresh", true)
                    navController.popBackStack()
                }
            )
        }

        /**
         * Alquileres - Lista de alquileres
         */
        composable(route = Screen.Alquileres.route) {
            val navigatedFromResult = navController.currentBackStackEntry
                ?.savedStateHandle
                ?.get<Boolean>("refresh") ?: false
            AlquileresScreen(
                onAlquilerClick = { alquilerId ->
                    navController.navigate(Screen.AlquilerDetalle.createRoute(alquilerId))
                },
                onCreateAlquiler = {
                    navController.navigate("create_alquiler")
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                navigatedFromResult = navigatedFromResult
            )
        }

        /**
         * Crear nuevo alquiler
         */
        composable(route = "create_alquiler") {
            CreateAlquilerScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onAlquilerCreated = { alquilerId ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("refresh", true)
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
                }
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
    }
}