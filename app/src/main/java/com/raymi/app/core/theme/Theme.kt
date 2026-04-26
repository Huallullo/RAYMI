package com.raymi.app.core.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = RaymiColors.ElectricBlue,
    onPrimary = RaymiColors.TextPrimary,
    primaryContainer = RaymiColors.ElectricBlueGlow,
    onPrimaryContainer = RaymiColors.TextPrimary,

    secondary = RaymiColors.MagentaFuture,
    onSecondary = RaymiColors.TextPrimary,
    secondaryContainer = RaymiColors.MagentaGlow,
    onSecondaryContainer = RaymiColors.TextPrimary,

    tertiary = RaymiColors.EmeraldMatrix,
    onTertiary = RaymiColors.TextPrimary,

    background = RaymiColors.DarkCyber,
    onBackground = RaymiColors.TextCyber,

    surface = RaymiColors.DarkSurface,
    onSurface = RaymiColors.TextCyber,
    surfaceVariant = RaymiColors.DarkCard,
    onSurfaceVariant = RaymiColors.TextSecondary,

    error = RaymiColors.ErrorNeon,
    onError = RaymiColors.TextPrimary,

    outline = RaymiColors.TextTertiary,
    outlineVariant = RaymiColors.TextTertiary.copy(alpha = 0.3f)
)

private val LightColorScheme = lightColorScheme(
    primary = RaymiColors.CyanHolo,
    onPrimary = RaymiColors.TextPrimaryLight,
    primaryContainer = RaymiColors.CyanQuantum,
    onPrimaryContainer = RaymiColors.TextPrimaryLight,

    secondary = RaymiColors.GoldCyber,
    onSecondary = RaymiColors.TextPrimaryLight,
    secondaryContainer = RaymiColors.GoldQuantum,
    onSecondaryContainer = RaymiColors.TextPrimaryLight,

    tertiary = RaymiColors.PurpleElectric,
    onTertiary = RaymiColors.TextPrimaryLight,

    background = RaymiColors.LightCyber,
    onBackground = RaymiColors.TextCyberLight,

    surface = RaymiColors.LightSurface,
    onSurface = RaymiColors.TextCyberLight,
    surfaceVariant = RaymiColors.LightCard,
    onSurfaceVariant = RaymiColors.TextSecondaryLight,

    error = RaymiColors.ErrorNeon,
    onError = RaymiColors.TextPrimaryLight,

    outline = RaymiColors.TextSecondaryLight,
    outlineVariant = RaymiColors.TextSecondaryLight.copy(alpha = 0.3f)
)

@Composable
fun RaymiTheme(
    darkTheme: Boolean = false, // Cambiado para forzar tema claro
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RaymiTypography,
        shapes = RaymiShapes,
        content = content
    )
}