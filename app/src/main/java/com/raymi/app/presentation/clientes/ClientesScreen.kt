package com.raymi.app.presentation.clientes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ContactSupport
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raymi.app.core.ads.AdManager
import com.raymi.app.core.theme.CustomShapes
import com.raymi.app.domain.model.Cliente
import com.raymi.app.presentation.components.*
import androidx.compose.ui.platform.testTag
import com.raymi.app.core.lang.LocalRaymiStrings

import androidx.compose.material3.pulltorefresh.PullToRefreshBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientesScreen(
    viewModel: ClientesViewModel = hiltViewModel(),
    onClienteClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") navigatedFromResult: Boolean
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val strings = LocalRaymiStrings.current

    LaunchedEffect(uiState.successMessage, uiState.error) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(strings.clients, fontWeight = FontWeight.Black)
                        Text("${uiState.clientes.size} ${strings.activeClients.lowercase()}", style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.search)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshClientes() }) {
                        Icon(Icons.Default.Refresh, contentDescription = strings.update)
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.showAddClienteDialog() },
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text(strings.newClient) },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CustomShapes.CardShape,
                modifier = Modifier
                    .padding(bottom = 96.dp) // Más aire arriba
                    .testTag("fab_add_cliente")
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refreshClientes() },
            modifier = Modifier.padding(paddingValues).fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                
                // 1. Buscador Inteligente
                RaymiSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.searchClientes(it) },
                    placeholder = strings.searchClient + "...",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchClientes("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = strings.cancel)
                            }
                        }
                    }
                )

                // 2. Chips de Ordenamiento
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = uiState.orden == OrdenCliente.RECIBIENTES,
                            onClick = { viewModel.cambiarOrden(OrdenCliente.RECIBIENTES) },
                            label = { Text(strings.recent) },
                            leadingIcon = { if (uiState.orden == OrdenCliente.RECIBIENTES) Icon(Icons.Default.Check, null, Modifier.size(16.dp)) },
                            shape = CircleShape
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.orden == OrdenCliente.ALFABETICO,
                            onClick = { viewModel.cambiarOrden(OrdenCliente.ALFABETICO) },
                            label = { Text("A-Z") },
                            shape = CircleShape
                        )
                    }
                }

                // 3. Listado de Clientes
                Box(modifier = Modifier.weight(1f)) {
                    if (uiState.visibleClientes.isEmpty() && !uiState.isLoading) {
                        RaymiEmptyState(
                            icon = Icons.AutoMirrored.Filled.ContactSupport,
                            title = strings.clients,
                            description = if (uiState.searchQuery.isEmpty()) strings.emptyClientsDesc else strings.searchNoResults,
                            actionText = null, // FAB es suficiente
                            onActionClick = {}
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 20.dp, start = 24.dp, end = 24.dp, top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.visibleClientes, key = { it.id }) { cliente ->
                                ModernClienteItem(
                                    cliente = cliente,
                                    onClick = { onClienteClick(cliente.id) }
                                )
                            }
                            
                            if (uiState.hasMoreClientes) {
                                item {
                                    TextButton(
                                        onClick = { viewModel.loadMoreClientes() },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text(strings.showMoreClients) }
                                }
                            }
                        }
                    }
                }

                if (viewModel.debeMostrarAnuncios()) {
                    AdBanner(modifier = Modifier.padding(bottom = 8.dp))
                }
            }
        }
    }

    if (uiState.showAddDialog) {
        AddClienteDialog(
            onDismiss = { viewModel.hideAddClienteDialog() }
        )
    }
}

@Composable
fun ModernClienteItem(
    cliente: Cliente, 
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = CustomShapes.CardShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AvatarWithInitials(
                initials = cliente.iniciales,
                size = 52,
                backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                textColor = MaterialTheme.colorScheme.primary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cliente.nombreCompleto,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "ID: ${cliente.dni}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.ChevronRight, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
