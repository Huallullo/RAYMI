package com.raymi.app.presentation.clientes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.raymi.app.core.theme.CustomShapes
import com.raymi.app.domain.model.Cliente
import com.raymi.app.presentation.components.AvatarWithInitials
import com.raymi.app.presentation.components.RaymiLoadingIndicator

/**
 * Pantalla de detalle de un cliente
 * Muestra toda la información del cliente y su historial de alquileres
 *
 * NOTA: Esta es una versión simplificada.
 * La versión completa requeriría un ViewModel para cargar los datos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClienteDetailScreen(
    clienteId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAlquiler: (String) -> Unit
) {
    // TODO: Implementar ViewModel para cargar datos del cliente
    // Por ahora mostramos una pantalla de ejemplo

    val scrollState = rememberScrollState()

    // Cliente de ejemplo (esto debería venir del ViewModel)
    val cliente = remember {
        Cliente(
            id = clienteId,
            dni = "12345678",
            nombre = "Juan",
            apellidos = "Pérez García",
            telefono = "987654321",
            email = "juan@example.com",
            direccion = "Av. Principal 123"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del Cliente") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Editar cliente */ }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cabecera con avatar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = CustomShapes.CardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AvatarWithInitials(
                        initials = cliente.iniciales,
                        size = 80
                    )

                    Text(
                        text = cliente.nombreCompleto,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Información personal
            Text(
                text = "Información Personal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = CustomShapes.CardShape
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoRow(
                        icon = Icons.Default.Badge,
                        label = "DNI",
                        value = cliente.dni
                    )

                    Divider()

                    InfoRow(
                        icon = Icons.Default.Phone,
                        label = "Teléfono",
                        value = cliente.telefono
                    )

                    if (cliente.email.isNotBlank()) {
                        Divider()
                        InfoRow(
                            icon = Icons.Default.Email,
                            label = "Email",
                            value = cliente.email
                        )
                    }

                    if (cliente.direccion.isNotBlank()) {
                        Divider()
                        InfoRow(
                            icon = Icons.Default.Home,
                            label = "Dirección",
                            value = cliente.direccion
                        )
                    }
                }
            }

            // Historial de alquileres
            Text(
                text = "Historial de Alquileres",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = CustomShapes.CardShape
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "Sin alquileres registrados",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Los alquileres aparecerán aquí",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Estadísticas
            Text(
                text = "Estadísticas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = CustomShapes.CardShape
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Total alquileres",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = CustomShapes.CardShape
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "S/. 0.00",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Total gastado",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Fila de información con icono
 */
@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}