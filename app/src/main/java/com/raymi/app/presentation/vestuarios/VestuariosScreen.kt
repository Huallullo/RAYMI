package com.raymi.app.presentation.vestuarios

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raymi.app.core.theme.CustomShapes
import com.raymi.app.core.theme.RaymiColors
import com.raymi.app.domain.model.EstadoVestuario
import com.raymi.app.domain.model.Vestuario
import com.raymi.app.presentation.components.*

/**
 * Pantalla de gestión de vestuarios
 * Muestra la lista de vestuarios en formato de grilla
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VestuariosScreen(
    viewModel: VestuariosViewModel = hiltViewModel(),
    onVestuarioClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    navigatedFromResult: Boolean
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showFilterMenu by remember { mutableStateOf(false) }

    LaunchedEffect(navigatedFromResult) {
        if (navigatedFromResult) {
            viewModel.loadVestuarios()
        }
    }

    // Mostrar mensajes
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
                title = { Text("Vestuarios") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Botón de filtro
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

                    // Menú de filtro
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

                        EstadoVestuario.values().forEach { estado ->
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
        },    floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddVestuarioDialog() }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Agregar vestuario")
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
                    RaymiLoadingIndicator(message = "Cargando vestuarios...")
                }

                uiState.vestuarios.isEmpty() -> {
                    RaymiEmptyState(
                        icon = Icons.Filled.Checkroom,
                        title = "No hay vestuarios",
                        description = "Agrega vestuarios para comenzar"
                    )
                }

                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Barra de búsqueda
                        RaymiSearchBar(
                            query = uiState.searchQuery,
                            onQueryChange = { viewModel.searchVestuarios(it) },
                            placeholder = "Buscar por código, danza o departamento...",
                            modifier = Modifier.padding(16.dp)
                        )

                        // Contador de resultados
                        if (uiState.selectedEstado != null || uiState.searchQuery.isNotBlank()) {
                            Text(
                                text = "${uiState.filteredVestuarios.size} vestuario(s) encontrado(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }

                        // Grilla de vestuarios
                        if (uiState.filteredVestuarios.isEmpty()) {
                            RaymiEmptyState(
                                icon = Icons.Filled.SearchOff,
                                title = "No se encontraron resultados",
                                description = "Intenta con otro término de búsqueda o filtro"
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = uiState.filteredVestuarios,
                                    key = { it.id }
                                ) { vestuario ->
                                    VestuarioCard(
                                        vestuario = vestuario,
                                        onClick = { onVestuarioClick(vestuario.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
// Al final de VestuariosScreen, REEMPLAZAR los diálogos con esto:

// Diálogo para agregar vestuario
    if (uiState.showAddDialog) {
        AddVestuarioDialog(
            onDismiss = { viewModel.hideAddVestuarioDialog() },
            onConfirm = { vestuario ->
                viewModel.addVestuario(vestuario)  // ✅ AGREGAR ESTA LÍNEA
            },
            isLoading = uiState.isSaving
        )
    }

// Diálogo para editar vestuario
    if (uiState.showEditDialog && uiState.selectedVestuario != null) {
        EditVestuarioDialog(
            vestuario = uiState.selectedVestuario!!,
            onDismiss = { viewModel.hideEditVestuarioDialog() },
            onConfirm = { vestuarioActualizado ->
                viewModel.updateVestuario(vestuarioActualizado)  // ✅ AGREGAR ESTA LÍNEA
            },
            isLoading = uiState.isSaving
        )
    }
    
}

/**
 * Card de vestuario en la grilla
 */
@Composable
fun VestuarioCard(
    vestuario: Vestuario,
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
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Código y estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = vestuario.codigo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                EstadoBadge(
                    texto = vestuario.estado.name.take(4),
                    color = when (vestuario.estado) {
                        EstadoVestuario.DISPONIBLE -> RaymiColors.Success
                        EstadoVestuario.ALQUILADO -> RaymiColors.Warning
                        EstadoVestuario.MANTENIMIENTO -> RaymiColors.Info
                        EstadoVestuario.NO_DISPONIBLE -> RaymiColors.Error
                    }
                )
            }

            // Danza
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = vestuario.danza,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Departamento
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = vestuario.departamento,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            HorizontalDivider()

            // Precio
            Text(
                text = vestuario.precioFormateado,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
