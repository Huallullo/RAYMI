package com.raymi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.firebase.auth.FirebaseAuth
import com.raymi.app.core.theme.RaymiTheme
import com.raymi.app.presentation.MainScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Activity principal de la aplicación
 * Maneja la inicialización y navegación global
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RaymiTheme {
                // Verificar si el usuario está autenticado
                val isUserAuthenticated = auth.currentUser != null

                // Mostrar la pantalla principal con navegación
                MainScreen(isUserAuthenticated = isUserAuthenticated)
            }
        }
    }
}