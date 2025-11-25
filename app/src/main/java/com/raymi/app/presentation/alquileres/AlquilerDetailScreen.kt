package com.raymi.app.presentation.alquileres

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
import androidx.hilt.navigation.compose.hiltViewModel
import com.raymi.app.core.theme.CustomShapes
import com.raymi.app.core.theme.RaymiColors
import com.raymi.app.domain.model.EstadoAlquiler

import com.raymi.app.presentation.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlquilerDetailScreen(
    alquilerId: String,
    viewModel: AlquilerDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDevolucionDialog by remember { mutableStateOf(false) }

    // Mostrar mensajes
    LaunchedEffect(uiState.error, uiState.successMessage) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessages()
            // Navegar atrás después de éxito
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del Alquiler") },
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    RaymiLoadingIndicator(message = "Cargando alquiler...")
                }

                uiState.alquiler == null -> {
                    RaymiErrorState(
                        message = "No se pudo cargar el alquiler",
                        onRetry = { /* Recargar */ }
                    )
                }

                else -> {
                    val alquiler = uiState.alquiler!!

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Cabecera con estado
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = CustomShapes.CardShape,
                            colors = CardDefaults.cardColors(
                                containerColor = when (alquiler.estado) {
                                    EstadoAlquiler.ACTIVO -> MaterialTheme.colorScheme.primaryContainer
                                    EstadoAlquiler.DEVUELTO -> RaymiColors.Success.copy(alpha = 0.1f)
                                    EstadoAlquiler.VENCIDO -> RaymiColors.Error.copy(alpha = 0.1f)
                                    EstadoAlquiler.CANCELADO -> MaterialTheme.colorScheme.surfaceVariant
                                }
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
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )

                                EstadoBadge(
                                    texto = alquiler.estado.name,
                                    color = when (alquiler.estado) {
                                        EstadoAlquiler.ACTIVO -> RaymiColors.Success
                                        EstadoAlquiler.DEVUELTO -> RaymiColors.Info
                                        EstadoAlquiler.VENCIDO -> RaymiColors.Error
                                        EstadoAlquiler.CANCELADO -> RaymiColors.TextTertiary
                                    }
                                )
                            }
                        }

                        // Cliente
                        Text(
                            text = "Cliente",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = CustomShapes.CardShape
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )

                                Text(
                                    text = alquiler.clienteNombre,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Vestuario
                        Text(
                            text = "Vestuario",
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
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Checkroom,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )

                                    Column {
                                        Text(
                                            text = alquiler.vestuarioNombre,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Código: ${alquiler.vestuarioCodigo}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Fechas
                        Text(
                            text = "Fechas",
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
                                    icon = Icons.Default.CalendarToday,
                                    label = "Fecha de Inicio",
                                    value = alquiler.fechaInicioFormatted
                                )

                                Divider()

                                InfoRow(
                                    icon = Icons.Default.Event,
                                    label = "Fecha de Devolución Prevista",
                                    value = alquiler.fechaFinFormatted
                                )

                                if (alquiler.estado == EstadoAlquiler.ACTIVO) {
                                    Divider()

                                    if (alquiler.estaVencido) {
                                        InfoRow(
                                            icon = Icons.Default.Warning,
                                            label = "Estado",
                                            value = "Vencido hace ${-alquiler.diasRestantes} día(s)",
                                            valueColor = RaymiColors.Error
                                        )
                                    } else {
                                        InfoRow(
                                            icon = Icons.Default.Info,
                                            label = "Días Restantes",
                                            value = "${alquiler.diasRestantes} día(s)",
                                            valueColor = if (alquiler.diasRestantes <= 2) {
                                                RaymiColors.Warning
                                            } else {
                                                RaymiColors.Success
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Información de pago
                        Text(
                            text = "Información de Pago",
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Precio Total:",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = alquiler.precioFormateado,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Adelanto:",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = alquiler.adelantoFormateado,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = RaymiColors.Success
                                    )
                                }

                                Divider()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Saldo:",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = alquiler.saldoFormateado,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (alquiler.saldo > 0) {
                                            RaymiColors.Warning
                                        } else {
                                            RaymiColors.Success
                                        }
                                    )
                                }
                            }
                        }

                        // Observaciones
                        if (alquiler.observaciones.isNotBlank()) {
                            Text(
                                text = "Observaciones",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = CustomShapes.CardShape
                            ) {
                                Text(
                                    text = alquiler.observaciones,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        // Acciones
                        if (alquiler.estado == EstadoAlquiler.ACTIVO) {
                            Button(
                                onClick = { showDevolucionDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                enabled = !uiState.isProcessing
                            ) {
                                if (uiState.isProcessing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Registrar Devolución")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // Diálogo de confirmación de devolución
    if (showDevolucionDialog) {
        AlertDialog(
            onDismissRequest = { showDevolucionDialog = false },
            icon = {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
            },
            title = {
                Text("Registrar Devolución")
            },
            text = {
                Text("¿Confirmas que el vestuario ha sido devuelto? Esta acción marcará el alquiler como completado.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDevolucionDialog = false
                        viewModel.registrarDevolucion()
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDevolucionDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}