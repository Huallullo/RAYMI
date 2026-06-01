package com.raymi.app.core.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Paleta de Colores DARK: Estilo sofisticado y premium.
 */
private val DarkColorScheme = darkColorScheme(
    primary = RaymiColors.IndigoLight,
    onPrimary = Color.White,
    primaryContainer = RaymiColors.IndigoDark,
    onPrimaryContainer = Color.White,

    secondary = RaymiColors.EmeraldAccent,
    onSecondary = Color.Black,
    
    background = RaymiColors.Slate900,
    onBackground = Color.White,
    
    surface = RaymiColors.Slate900,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1E293B), // Slate 800 aprox
    onSurfaceVariant = RaymiColors.Slate500,

    outline = RaymiColors.Slate700
)

/**
 * Paleta de Colores LIGHT: Limpieza total, enfoque en contenido (Estilo SaaS Premium).
 */
private val LightColorScheme = lightColorScheme(
    primary = RaymiColors.IndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF), // Indigo 50
    onPrimaryContainer = RaymiColors.IndigoPrimary,

    secondary = RaymiColors.EmeraldAccent,
    onSecondary = Color.White,
    
    background = RaymiColors.Slate50,
    onBackground = RaymiColors.Slate900,
    
    surface = Color.White,
    onSurface = RaymiColors.Slate900,
    surfaceVariant = Color(0xFFF1F5F9), // Slate 100
    onSurfaceVariant = RaymiColors.Slate800,

    outline = RaymiColors.Slate600 // Máximo contraste en bordes y etiquetas
)

@Composable
fun RaymiTheme(
    darkTheme: Boolean = false, // Forzamos Light Mode por defecto para consistencia SaaS
    content: @Composable () -> Unit
) {
    // Si en el futuro quieres habilitar Dark Mode, vuelve a usar: isSystemInDarkTheme()
    val colorScheme = LightColorScheme 

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb() // Aseguramos color de barra
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RaymiTypography,
        shapes = RaymiShapes,
        content = content
    )
}
