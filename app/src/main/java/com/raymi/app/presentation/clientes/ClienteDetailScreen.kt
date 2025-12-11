package com.raymi.app.presentation.clientes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.raymi.app.presentation.alquileres.AlquilerItem

import com.raymi.app.presentation.components.*

@OptIn(ExperimentalMaterial3Api::class)

@Composable

fun ClienteDetailScreen(
    clienteId: String,
    viewModel: ClienteDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToAlquiler: (String) -> Unit
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
                title = { Text("Detalle del Cliente") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (uiState.cliente != null) {
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
                    RaymiLoadingIndicator(message = "Cargando cliente...")
                }

                uiState.cliente == null -> {
                    RaymiErrorState(
                        message = "No se pudo cargar el cliente",
                        onRetry = { /* Recargar */ }
                    )
                }

                else -> {
                    val cliente = uiState.cliente!!

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
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
                                        text = "S/. ${String.format("%.2f", uiState.totalGastado)}",
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

                        // Historial de alquileres
                        Text(
                            text = "Historial de Alquileres",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (uiState.alquileres.isEmpty()) {
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
                                }
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                uiState.alquileres.forEach { alquiler ->
                                    AlquilerItem(
                                        alquiler = alquiler,
                                        onClick = { onNavigateToAlquiler(alquiler.id) }
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
    if (showEditDialog && uiState.cliente != null) {
        EditClienteDialog(
            cliente = uiState.cliente!!,
            onDismiss = { showEditDialog = false },
            onConfirm = { clienteActualizado ->
                viewModel.updateCliente(clienteActualizado)
                showEditDialog = false
            },
            isLoading = uiState.isLoading
        )
    }
}