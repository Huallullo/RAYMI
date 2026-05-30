package com.raymi.app.presentation.splash

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raymi.app.R
import kotlinx.coroutines.delay

/**
 * Pantalla de Splash Animada (Diseño Senior).
 * Proporciona una transición fluida y una estética "futurista" al iniciar la app.
 */
@Composable
fun SplashScreen(
    onAnimationFinished: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    
    // Animación de escala para el logo
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1.2f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "LogoScale"
    )

    // Animación de opacidad
    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "LogoAlpha"
    )

    // Animación de aparición para el gato en pose (Opacidad total para ver el color real)
    val catAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1500, delayMillis = 500),
        label = "CatAlpha"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(3000)
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)),
        contentAlignment = Alignment.Center
    ) {
        // --- LA FOTO REAL DEL GATO SPHYNX ---
        // Ubicado al centro inferior, tamaño ajustado y color original al 100%
        Image(
            painter = painterResource(id = R.drawable.sphynx_photo),
            contentDescription = "Gato Sphynx Real",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(380.dp)
                .alpha(catAlpha),
            contentScale = ContentScale.Fit
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 140.dp), // Separación Senior: Elevamos el texto para que no choque con la imagen
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .scale(scale)
                    .alpha(alpha),
                contentAlignment = Alignment.Center
            ) {
                // Mejora del Logo: Aura morada futurista
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                RaymiLogoIcon(modifier = Modifier.size(100.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = startAnimation,
                enter = fadeIn(animationSpec = tween(1500)) + expandVertically(animationSpec = tween(1000))
            ) {
                Text(
                    text = "RAYMI",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 8.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            AnimatedVisibility(
                visible = startAnimation,
                enter = fadeIn(animationSpec = tween(2000, delayMillis = 500))
            ) {
                Text(
                    text = "Gesti\u00f3n de Alquileres",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
private fun RaymiLogoIcon(
    modifier: Modifier = Modifier
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_raymi_logo),
        contentDescription = "Logo RAYMI",
        modifier = modifier,
        tint = Color.Unspecified
    )
}
