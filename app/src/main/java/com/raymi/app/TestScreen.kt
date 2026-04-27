package com.raymi.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.raymi.app.presentation.components.*

@Composable
fun TestScreen() {
    var showLoading by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var showEmpty by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Logo y título
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "RAYMI 2.0",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Sistema de Gestión de Alquiler de Vestuarios",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Cards de estadísticas de prueba
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Clientes",
                value = "24",
                icon = Icons.Default.People,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Vestuarios",
                value = "18",
                icon = Icons.Default.Checkroom,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Alquileres",
                value = "12",
                icon = Icons.Default.ShoppingCart,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Ingresos",
                value = "S/. 5,420",
                icon = Icons.Default.AttachMoney,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botones de prueba
        Button(
            onClick = { showLoading = !showLoading },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Toggle Loading State")
        }

        Button(
            onClick = { showEmpty = !showEmpty },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Toggle Empty State")
        }

        Button(
            onClick = { showError = !showError },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Toggle Error State")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Estados de prueba
        when {
            showLoading -> {
                RaymiLoadingIndicator(message = "Cargando datos...")
            }
            showEmpty -> {
                RaymiEmptyState(
                    icon = Icons.Default.Inbox,
                    title = "No hay datos",
                    description = "Aún no hay información para mostrar",
                    actionText = "Agregar nuevo",
                    onActionClick = { showEmpty = false }
                )
            }
            showError -> {
                RaymiErrorState(
                    message = "No se pudo conectar con el servidor",
                    onRetry = { showError = false }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Avatar de prueba
        AvatarWithInitials(
            initials = "JD",
            size = 56
        )

        Text(
            text = "✅ Tema configurado correctamente",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
