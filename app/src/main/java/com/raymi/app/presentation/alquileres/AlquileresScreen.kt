package com.raymi.app.presentation.alquileres

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raymi.app.core.theme.CustomShapes
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.EstadoAlquiler
import com.raymi.app.presentation.components.*

/**
 * Pantalla de Gestión de Alquileres (Contratos).
 * Diseño Senior: Enfoque en legibilidad de estados y fechas críticas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlquileresScreen(
    viewModel: AlquileresViewModel = hiltViewModel(),
    onAlquilerClick: (String) -> Unit,
    onCreateAlquiler: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { 
                    Column {
                        Text("Alquileres", fontWeight = FontWeight.Black)
                        Text("Control de préstamos y devoluciones", style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilters = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filtrar",
                            tint = if (uiState.selectedEstado != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateAlquiler,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Registrar Alquiler") },
                shape = CustomShapes.CardShape,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("fab_create_alquiler")   // ✅ AÑADIDO
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            
            // Barra de búsqueda profesional
            RaymiSearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.searchAlquileres(it) },
                placeholder = "Cliente, producto o código...",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> RaymiLoadingIndicator(message = "Sincronizando contratos...")
                    uiState.error != null -> RaymiErrorState(message = uiState.error!!, onRetry = { viewModel.loadAlquileres() })
                    uiState.filteredAlquileres.isEmpty() -> {
                        RaymiEmptyState(
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            title = "Sin Movimientos",
                            description = "No hay alquileres que coincidan con la búsqueda.",
                            actionText = "Nuevo Alquiler",
                            onActionClick = onCreateAlquiler
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(uiState.filteredAlquileres, key = { it.id }) { alquiler ->
                                PremiumAlquilerCard(
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

    // Modal de Filtros por Estado
    if (showFilters) {
        AlertDialog(
            onDismissRequest = { showFilters = false },
            title = { Text("Filtrar por Estado") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterOption("Todos", uiState.selectedEstado == null) { viewModel.filterByEstado(null); showFilters = false }
                    EstadoAlquiler.entries.forEach { estado ->
                        FilterOption(estado.name, uiState.selectedEstado == estado) {
                            viewModel.filterByEstado(estado)
                            showFilters = false
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showFilters = false }) { Text("Cerrar") } }
        )
    }
}

@Composable
fun FilterOption(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Text(label)
    }
}

@Composable
fun PremiumAlquilerCard(alquiler: Alquiler, onClick: () -> Unit) {
    AlquilerItem(alquiler = alquiler, onClick = onClick)
}
