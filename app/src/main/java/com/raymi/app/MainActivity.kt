package com.raymi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.raymi.app.core.theme.RaymiTheme
import com.raymi.app.presentation.MainScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Activity principal de la aplicación
 * Maneja la inicialización y navegación global
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var auth: FirebaseAuth
    
    @Inject
    lateinit var workspaceManager: com.raymi.app.core.workspace.WorkspaceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicialización Maestro de Publicidad
        lifecycleScope.launch {
            com.raymi.app.core.ads.AdManager.inicializar(this@MainActivity)
        }

        enableEdgeToEdge()

        setContent {
            RaymiTheme {
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    com.raymi.app.presentation.splash.SplashScreen(
                        onAnimationFinished = {
                            showSplash = false
                        }
                    )
                } else {
                    MainScreen(
                        workspaceManager = workspaceManager
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Eliminado auth.signOut() para permitir persistencia de sesión móvil estándar
    }
}
