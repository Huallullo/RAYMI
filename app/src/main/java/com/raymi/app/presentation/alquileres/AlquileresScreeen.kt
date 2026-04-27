package com.raymi.app.presentation.alquileres

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
import com.raymi.app.core.theme.RaymiColors
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.EstadoAlquiler
import com.raymi.app.presentation.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlquileresScreen(
    viewModel: AlquileresViewModel = hiltViewModel(),
    onAlquilerClick: (String) -> Unit,
    onCreateAlquiler: () -> Unit,
    onNavigateBack: () -> Unit,
    navigatedFromResult: Boolean
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showFilterMenu by remember { mutableStateOf(false) }

    LaunchedEffect(navigatedFromResult) {
        if (navigatedFromResult) {
            viewModel.loadAlquileres()
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
                title = { Text("Alquileres") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterMenu = true }) {
                        Badge(
                            containerColor = if (uiState.selectedEstado != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Icon(Icons.Filled.FilterList, contentDescription = "Filtrar")
                        }
                    }

                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Todos") },
                            onClick = {
                                viewModel.filterByEstado(null)
                                showFilterMenu = false
                            },
                            leadingIcon = {
                                if (uiState.selectedEstado == null) {
                                    Icon(Icons.Filled.Check, contentDescription = null)
                                }
                            }
                        )

                        HorizontalDivider()

                        EstadoAlquiler.values().forEach { estado ->
                            DropdownMenuItem(
                                text = { Text(estado.name) },
                                onClick = {
                                    viewModel.filterByEstado(estado)
                                    showFilterMenu = false
                                },
                                leadingIcon = {
                                    if (uiState.selectedEstado == estado) {
                                        Icon(Icons.Filled.Check, contentDescription = null)
                                    }
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateAlquiler
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo alquiler")
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
                    RaymiLoadingIndicator(message = "Cargando alquileres...")
                }

                uiState.alquileres.isEmpty() -> {
                    RaymiEmptyState(
                        icon = Icons.Filled.ShoppingCart,
                        title = "No hay alquileres",
                        description = "Crea tu primer alquiler para comenzar",
                        actionText = "Crear Alquiler",
                        onActionClick = onCreateAlquiler
                    )
                }

                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        RaymiSearchBar(
                            query = uiState.searchQuery,
                            onQueryChange = { viewModel.searchAlquileres(it) },
                            placeholder = "Buscar por cliente o vestuario...",
                            modifier = Modifier.padding(16.dp)
                        )

                        if (uiState.filteredAlquileres.isEmpty()) {
                            RaymiEmptyState(
                                icon = Icons.Filled.SearchOff,
                                title = "No se encontraron resultados",
                                description = "Intenta con otro término de búsqueda o filtro"
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    items = uiState.filteredAlquileres,
                                    key = { it.id }
                                ) { alquiler ->
                                    AlquilerItem(
                                        alquiler = alquiler,
                                        onClick = { onAlquilerClick(alquiler.id) }
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

@Composable
fun AlquilerItem(
    alquiler: Alquiler,
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
                        text = alquiler.vestuarioNombre,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

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

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    InfoRowCompact(
                        icon = Icons.Filled.CalendarToday,
                        text = "Inicio: ${alquiler.fechaInicioFormatted}"
                    )
                    InfoRowCompact(
                        icon = Icons.Filled.Event,
                        text = "Fin: ${alquiler.fechaFinFormatted}"
                    )

                    if (alquiler.estado == EstadoAlquiler.ACTIVO) {
                        if (alquiler.estaVencido) {
                            InfoRowCompact(
                                icon = Icons.Filled.Warning,
                                text = "Vencido hace ${-alquiler.diasRestantes} día(s)",
                                color = RaymiColors.Error
                            )
                        } else {
                            InfoRowCompact(
                                icon = Icons.Filled.Info,
                                text = "${alquiler.diasRestantes} día(s) restantes",
                                color = if (alquiler.diasRestantes <= 2) {
                                    RaymiColors.Warning
                                } else {
                                    RaymiColors.Success
                                }
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = alquiler.precioFormateado,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (alquiler.saldo > 0) {
                        Text(
                            text = "Saldo: ${alquiler.saldoFormateado}",
                            style = MaterialTheme.typography.bodySmall,
                            color = RaymiColors.Warning
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRowCompact(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = color
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}
