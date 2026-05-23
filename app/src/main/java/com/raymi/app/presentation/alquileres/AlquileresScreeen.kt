package com.raymi.app.presentation.alquileres

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.raymi.app.presentation.components.EstadoBadge
import com.raymi.app.presentation.components.RaymiEmptyState
import com.raymi.app.presentation.components.RaymiLoadMoreSection
import com.raymi.app.presentation.components.RaymiLoadingIndicator
import com.raymi.app.presentation.components.RaymiSearchBar

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
                        RaymiLoadMoreSection(
                            showing = uiState.visibleAlquileres.size,
                            total = uiState.filteredAlquileres.size,
                            itemLabelSingular = "alquiler",
                            itemLabelPlural = "alquileres",
                            hasMore = false,
                            onLoadMore = {},
                            icon = Icons.Filled.ShoppingCart,
                            modifier = Modifier.padding(horizontal = 16.dp)
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
                                    items = uiState.visibleAlquileres,
                                    key = { it.id }
                                ) { alquiler ->
                                    AlquilerItem(
                                        alquiler = alquiler,
                                        onClick = { onAlquilerClick(alquiler.id) }
                                    )
                                }
                                if (uiState.hasMoreAlquileres) {
                                    item(key = "load_more_alquileres") {
                                        RaymiLoadMoreSection(
                                            showing = uiState.visibleAlquileres.size,
                                            total = uiState.filteredAlquileres.size,
                                            itemLabelSingular = "alquiler",
                                            itemLabelPlural = "alquileres",
                                            hasMore = true,
                                            onLoadMore = { viewModel.loadMoreAlquileres() },
                                            showCounter = false
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

@Composable
fun AlquilerItem(
    alquiler: Alquiler,
    onClick: () -> Unit
) {
    // Formatear hora de creación con 12h + AM/PM y zona horaria local explícita
    val horaCreacion = alquiler.createdAt?.let { timestamp ->
        val date = timestamp.toDate()
        // Usar SimpleDateFormat con 12h y AM/PM
        val formatter = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        formatter.timeZone = java.util.TimeZone.getDefault()
        formatter.format(date)
    } ?: ""

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
                    // Hora de creación con formato am/pm
                    if (horaCreacion.isNotBlank()) {
                        Text(
                            text = "Creado: $horaCreacion",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                EstadoBadge(
                    texto = alquiler.estadoEtiqueta,
                    color = when (alquiler.estadoVisual) {
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

                    when {
                        alquiler.estado != EstadoAlquiler.ACTIVO -> { /* no mostrar días */ }
                        alquiler.estaVencido -> {
                            InfoRowCompact(
                                icon = Icons.Filled.Warning,
                                text = "Vencido (por fecha)",
                                color = RaymiColors.Error
                            )
                        }
                        alquiler.diasRestantes == 0 -> {
                            InfoRowCompact(
                                icon = Icons.Filled.Warning,
                                text = "Vence hoy",
                                color = RaymiColors.Warning
                            )
                        }
                        alquiler.diasRestantes > 0 -> {
                            InfoRowCompact(
                                icon = Icons.Filled.Info,
                                text = "${alquiler.diasRestantes} día(s) restantes",
                                color = if (alquiler.diasRestantes <= 2) RaymiColors.Warning else RaymiColors.Success
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

// Extensiones visuales (deben estar en el mismo archivo o en uno aparte)
private val Alquiler.estadoVisual: EstadoAlquiler
    get() = if (estado == EstadoAlquiler.ACTIVO && estaVencido) {
        EstadoAlquiler.VENCIDO
    } else {
        estado
    }

private val Alquiler.estadoEtiqueta: String
    get() = if (estado == EstadoAlquiler.ACTIVO && estaVencido) {
        "ACTIVO • VENCIDO"
    } else {
        estado.name
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