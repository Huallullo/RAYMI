package com.raymi.app.presentation.clientes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raymi.app.core.theme.CustomShapes
import com.raymi.app.domain.model.Cliente
import com.raymi.app.presentation.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientesScreen(
    viewModel: ClientesViewModel = hiltViewModel(),
    onClienteClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    navigatedFromResult: Boolean
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(navigatedFromResult) {
        if (navigatedFromResult) {
            viewModel.loadClientes()
        }
    }

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
                title = { Text("Clientes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddClienteDialog() }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Agregar cliente")
            }
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
                    RaymiLoadingIndicator(message = "Cargando clientes...")
                }

                uiState.clientes.isEmpty() -> {
                    RaymiEmptyState(
                        icon = Icons.Filled.People,
                        title = "No hay clientes",
                        description = "Agrega tu primer cliente para comenzar",
                        actionText = "Agregar Cliente",
                        onActionClick = { viewModel.showAddClienteDialog() }
                    )
                }

                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        RaymiSearchBar(
                            query = uiState.searchQuery,
                            onQueryChange = { viewModel.searchClientes(it) },
                            placeholder = "Buscar por nombre, DNI o teléfono...",
                            modifier = Modifier.padding(16.dp)
                        )

                        if (uiState.filteredClientes.isEmpty()) {
                            RaymiEmptyState(
                                icon = Icons.Filled.SearchOff,
                                title = "No se encontraron resultados",
                                description = "Intenta con otro término de búsqueda"
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    items = uiState.filteredClientes,
                                    key = { it.id }
                                ) { cliente ->
                                    ClienteItem(
                                        cliente = cliente,
                                        onClick = { onClienteClick(cliente.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showAddDialog) {
        AddClienteDialog(
            onDismiss = { viewModel.hideAddClienteDialog() },
            onConfirm = { cliente ->
                viewModel.addCliente(cliente)
            },
            isLoading = uiState.isSaving
        )
    }
}

@Composable
fun ClienteItem(
    cliente: Cliente,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = CustomShapes.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarWithInitials(
                initials = cliente.iniciales,
                size = 56
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = cliente.nombreCompleto,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Badge,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "DNI: ${cliente.dni}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = cliente.telefono,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Ver detalle",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}