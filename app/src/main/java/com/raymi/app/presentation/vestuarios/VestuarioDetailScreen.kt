package com.raymi.app.presentation.vestuarios

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.raymi.app.core.theme.CustomShapes
import com.raymi.app.core.theme.RaymiColors
import com.raymi.app.domain.model.EstadoVestuario
import com.raymi.app.presentation.alquileres.AlquilerItem


import com.raymi.app.presentation.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VestuarioDetailScreen(
    vestuarioId: String,
    viewModel: VestuarioDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error, uiState.successMessage) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessages()
        }
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
                    IconButton(onClick = {
                        if (uiState.vestuario != null) {
                            showEditDialog = true
                        }
                    }) {
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
                    RaymiLoadingIndicator(message = "Cargando vestuario...")
                }

                uiState.vestuario == null -> {
                    RaymiErrorState(
                        message = "No se pudo cargar el vestuario",
                        onRetry = { /* Recargar */ }
                    )
                }

                else -> {
                    val vestuario = uiState.vestuario!!

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
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
                                        text = "${uiState.totalAlquileres}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Veces alquilado",
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
                                        text = "S/. ${String.format("%.2f", uiState.totalIngresos)}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Ingresos generados",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Historial
                        Text(
                            text = "Historial de Alquileres",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (uiState.historialAlquileres.isEmpty()) {
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
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                uiState.historialAlquileres.take(5).forEach { alquiler ->
                                    AlquilerItem(
                                        alquiler = alquiler,
                                        onClick = { /* Navegar a detalle */ }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
    // Al final del composable:
    if (showEditDialog && uiState.vestuario != null) {
        EditVestuarioDialog(
            vestuario = uiState.vestuario!!,
            onDismiss = { showEditDialog = false },
            onConfirm = { vestuarioActualizado ->
                viewModel.updateVestuario(vestuarioActualizado)
                showEditDialog = false
            },
            isLoading = uiState.isLoading
        )
    }
}