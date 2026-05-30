package com.raymi.app.presentation.alquileres

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raymi.app.core.ads.AdManager
import com.raymi.app.core.theme.CustomShapes
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.EstadoAlquiler
import com.raymi.app.presentation.components.*
import com.raymi.app.core.lang.LocalRaymiStrings

/**
 * Pantalla de Gestión de Alquileres (Contratos).
 * Optimizada para SaaS (Snapshots + Pull-to-refresh) para ahorro de costos.
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
    val strings = LocalRaymiStrings.current
    var showFilters by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { 
                    Column {
                        Text(strings.rentalsManagement, fontWeight = FontWeight.Black)
                        Text(strings.rentalDesc, style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshAlquileres() }) {
                        Icon(Icons.Default.Refresh, contentDescription = strings.update)
                    }
                    IconButton(onClick = { showFilters = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = strings.filter,
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
                text = { Text(strings.createRental) },
                shape = CustomShapes.CardShape,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("fab_create_alquiler")
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refreshAlquileres() },
            modifier = Modifier.padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                
                RaymiSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.searchAlquileres(it) },
                    placeholder = strings.searchPlaceholder,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                Box(modifier = Modifier.weight(1f)) {
                    when {
                        uiState.isLoading && uiState.alquileres.isEmpty() -> RaymiLoadingIndicator(message = strings.loading)
                        uiState.error != null -> RaymiErrorState(message = uiState.error!!, onRetry = { viewModel.refreshAlquileres() })
                        uiState.filteredAlquileres.isEmpty() -> {
                            RaymiEmptyState(
                                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                                title = strings.noMovements,
                                description = strings.noMovementsDesc,
                                actionText = strings.newRental,
                                onActionClick = onCreateAlquiler
                            )
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 100.dp),
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

                if (viewModel.debeMostrarAnuncios()) {
                    AdBanner(modifier = Modifier.padding(bottom = 8.dp))
                }
            }
        }
    }

    if (showFilters) {
        AlertDialog(
            onDismissRequest = { showFilters = false },
            title = { Text(strings.filterByStatus) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterOption(strings.all, uiState.selectedEstado == null) { viewModel.filterByEstado(null); showFilters = false }
                    EstadoAlquiler.entries.forEach { estado ->
                        FilterOption(estado.name, uiState.selectedEstado == estado) {
                            viewModel.filterByEstado(estado)
                            showFilters = false
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showFilters = false }) { Text(strings.close) } }
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
