package com.raymi.app.presentation.historial

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.raymi.app.core.theme.RaymiColors
import com.raymi.app.core.utils.formatTo
import com.raymi.app.domain.model.EstadoAlquiler
import com.raymi.app.presentation.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    viewModel: HistorialViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Alquileres") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadHistorial() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    RaymiLoadingIndicator(message = "Cargando historial...")
                }

                uiState.alquileres.isEmpty() -> {
                    RaymiEmptyState(
                        icon = Icons.Default.History,
                        title = "Sin historial",
                        description = "El historial de alquileres aparecerá aquí"
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.alquileres) { alquiler ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = when (alquiler.estado) {
                                        EstadoAlquiler.DEVUELTO -> RaymiColors.Success.copy(alpha = 0.1f)
                                        EstadoAlquiler.CANCELADO -> MaterialTheme.colorScheme.errorContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = alquiler.clienteNombre,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${alquiler.cantidad}x ${alquiler.vestuarioNombre}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        EstadoBadge(
                                            texto = alquiler.estado.name,
                                            color = when (alquiler.estado) {
                                                EstadoAlquiler.DEVUELTO -> RaymiColors.Success
                                                EstadoAlquiler.CANCELADO -> RaymiColors.Error
                                                EstadoAlquiler.ACTIVO -> RaymiColors.Warning
                                                EstadoAlquiler.VENCIDO -> RaymiColors.Error
                                            }
                                        )
                                    }

                                    Divider()

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "Inicio: ${alquiler.fechaInicioFormatted}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            if (alquiler.fechaDevolucion != null) {
                                                Text(
                                                    text = "Devuelto: ${alquiler.fechaDevolucion!!.toDate().formatTo("dd/MM/yyyy")}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = RaymiColors.Success
                                                )
                                            } else {
                                                Text(
                                                    text = "Previsto: ${alquiler.fechaFinFormatted}",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = alquiler.precioFormateado,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            if (alquiler.saldo > 0) {
                                                Text(
                                                    text = "Deuda: ${alquiler.saldoFormateado}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = RaymiColors.Error
                                                )
                                            } else {
                                                Text(
                                                    text = "✓ Pagado",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = RaymiColors.Success
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}