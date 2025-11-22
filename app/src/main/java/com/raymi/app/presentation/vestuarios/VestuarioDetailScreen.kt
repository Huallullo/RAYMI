package com.raymi.app.presentation.vestuarios

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
import com.raymi.app.core.theme.RaymiColors
import com.raymi.app.domain.model.EstadoVestuario
import com.raymi.app.domain.model.Vestuario
import com.raymi.app.presentation.clientes.InfoRow
import com.raymi.app.presentation.components.EstadoBadge

/**
 * Pantalla de detalle de un vestuario
 * Muestra toda la información del vestuario
 *
 * NOTA: Esta es una versión simplificada.
 * La versión completa requeriría un ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VestuarioDetailScreen(
    vestuarioId: String,
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Vestuario de ejemplo (esto debería venir del ViewModel)
    val vestuario = remember {
        Vestuario(
            id = vestuarioId,
            codigo = "VES-001",
            danza = "Marinera Norteña",
            departamento = "La Libertad",
            descripcion = "Traje tradicional de marinera norteña para dama",
            talla = "M",
            precio = 150.0,
            estado = EstadoVestuario.DISPONIBLE
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del Vestuario") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Editar */ }) {
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
            // Cabecera con código y estado
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
                    Icon(
                        imageVector = Icons.Default.Checkroom,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = vestuario.codigo,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    EstadoBadge(
                        texto = vestuario.estado.name,
                        color = when (vestuario.estado) {
                            EstadoVestuario.DISPONIBLE -> RaymiColors.Success
                            EstadoVestuario.ALQUILADO -> RaymiColors.Warning
                            EstadoVestuario.MANTENIMIENTO -> RaymiColors.Info
                            EstadoVestuario.NO_DISPONIBLE -> RaymiColors.Error
                        }
                    )
                }
            }

            // Información básica
            Text(
                text = "Información Básica",
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
                        icon = Icons.Default.MusicNote,
                        label = "Danza",
                        value = vestuario.danza
                    )

                    Divider()

                    InfoRow(
                        icon = Icons.Default.Place,
                        label = "Departamento",
                        value = vestuario.departamento
                    )

                    Divider()

                    InfoRow(
                        icon = Icons.Default.Straighten,
                        label = "Talla",
                        value = vestuario.talla
                    )

                    Divider()

                    InfoRow(
                        icon = Icons.Default.AttachMoney,
                        label = "Precio por día",
                        value = vestuario.precioFormateado
                    )
                }
            }

            // Descripción
            if (vestuario.descripcion.isNotBlank()) {
                Text(
                    text = "Descripción",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = CustomShapes.CardShape
                ) {
                    Text(
                        text = vestuario.descripcion,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Historial
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
                        text = "Sin historial de alquileres",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}