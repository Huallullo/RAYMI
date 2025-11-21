package com.raymi.app.core.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = RaymiColors.Gold,
    onPrimary = RaymiColors.DarkBackground,
    primaryContainer = RaymiColors.GoldVariant,
    onPrimaryContainer = RaymiColors.DarkBackground,

    secondary = RaymiColors.IncaRed,
    onSecondary = RaymiColors.TextPrimary,
    secondaryContainer = RaymiColors.IncaRedDark,
    onSecondaryContainer = RaymiColors.TextPrimary,

    tertiary = RaymiColors.Terracota,
    onTertiary = RaymiColors.TextPrimary,

    background = RaymiColors.DarkBackground,
    onBackground = RaymiColors.TextPrimary,

    surface = RaymiColors.DarkSurface,
    onSurface = RaymiColors.TextPrimary,
    surfaceVariant = RaymiColors.DarkCard,
    onSurfaceVariant = RaymiColors.TextSecondary,

    error = RaymiColors.Error,
    onError = RaymiColors.TextPrimary,

    outline = RaymiColors.TextTertiary,
    outlineVariant = RaymiColors.TextTertiary.copy(alpha = 0.3f)
)

private val LightColorScheme = lightColorScheme(
    primary = RaymiColors.IncaRed,
    onPrimary = RaymiColors.TextPrimary,
    primaryContainer = RaymiColors.IncaRedLight,
    onPrimaryContainer = RaymiColors.TextPrimaryLight,

    secondary = RaymiColors.Gold,
    onSecondary = RaymiColors.DarkBackground,
    secondaryContainer = RaymiColors.GoldLight,
    onSecondaryContainer = RaymiColors.TextPrimaryLight,

    tertiary = RaymiColors.Terracota,
    onTertiary = RaymiColors.TextPrimary,

    background = RaymiColors.LightBackground,
    onBackground = RaymiColors.TextPrimaryLight,

    surface = RaymiColors.LightSurface,
    onSurface = RaymiColors.TextPrimaryLight,
    surfaceVariant = RaymiColors.LightCard,
    onSurfaceVariant = RaymiColors.TextSecondaryLight,

    error = RaymiColors.Error,
    onError = RaymiColors.TextPrimary,

    outline = RaymiColors.TextSecondaryLight,
    outlineVariant = RaymiColors.TextSecondaryLight.copy(alpha = 0.3f)
)

@Composable
fun RaymiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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