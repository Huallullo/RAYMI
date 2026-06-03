package com.raymi.app.presentation.items

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.raymi.app.core.theme.CustomShapes
import com.raymi.app.domain.model.Item
import com.raymi.app.presentation.components.*
import com.raymi.app.core.lang.LocalRaymiStrings

/**
 * Pantalla Principal de Inventario (SaaS) - Versión Pulida.
 * Diseño Senior: Animaciones de entrada, estados de carga elegantes y jerarquía visual mejorada.
 */
import androidx.compose.material3.pulltorefresh.PullToRefreshBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(
    viewModel: ItemsViewModel = hiltViewModel(),
    onItemClick: (String) -> Unit,
    onAddItem: () -> Unit,
    onNavigateToCategorias: () -> Unit,
    onNavigateBack: () -> Unit,
    navigatedFromResult: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val strings = LocalRaymiStrings.current
    var showCategoryWarning by remember { mutableStateOf(false) }

    // Auto-refresh al volver de crear/editar un ítem
    LaunchedEffect(navigatedFromResult) {
        if (navigatedFromResult) {
            viewModel.refreshItems()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = { 
                    Column {
                        Text(strings.globalInventory, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                        Text(strings.centralizedManagement, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.search)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshItems() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                    TextButton(onClick = onNavigateToCategorias) {
                        Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text(strings.categories, fontWeight = FontWeight.Bold)
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (uiState.categorias.isEmpty()) {
                        showCategoryWarning = true
                    } else {
                        onAddItem()
                    }
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(strings.newItem, fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CustomShapes.CardShape,
                modifier = Modifier
                    .padding(bottom = 96.dp) // Más aire arriba para no chocar con la barra
                    .testTag("fab_add_item")
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refreshItems() },
            modifier = Modifier.padding(paddingValues).fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Buscador con Diseño Premium
                RaymiSearchBar(
                    query = uiState.queryBusqueda,
                    onQueryChange = { viewModel.buscar(it) },
                    placeholder = strings.searchPlaceholder,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )

                // 2. Filtro de Categorías
                AnimatedVisibility(
                    visible = uiState.categorias.isNotEmpty(),
                    enter = fadeIn() + expandVertically()
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = uiState.categoriaFiltro == null,
                                onClick = { viewModel.filtrarPorCategoria(null) },
                                label = { Text(strings.all) },
                                shape = CircleShape,
                                leadingIcon = { if (uiState.categoriaFiltro == null) Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                            )
                        }
                        items(uiState.categorias) { categoria ->
                            FilterChip(
                                selected = uiState.categoriaFiltro?.id == categoria.id,
                                onClick = { viewModel.filtrarPorCategoria(categoria) },
                                label = { Text(categoria.nombre) },
                                shape = CircleShape
                            )
                        }
                    }
                }

                // 3. Contenido Principal
                Box(modifier = Modifier.weight(1f)) {
                    if (uiState.itemsFiltrados.isEmpty() && !uiState.isLoading) {
                        RaymiEmptyState(
                            icon = Icons.Default.Inventory,
                            title = strings.emptyInventory,
                            description = if (uiState.queryBusqueda.isEmpty()) strings.emptyInventoryDesc else strings.searchNoResults,
                            actionText = null, // FAB es suficiente y evita redundancia
                            onActionClick = {}
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp, top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.itemsFiltrados, key = { it.id }) { item ->
                                AnimatedItemEntry {
                                    ModernItemCard(
                                        item = item,
                                        onClick = { onItemClick(item.id) }
                                    )
                                }
                            }

                            if (uiState.hasMore) {
                                item(span = { GridItemSpan(2) }) {
                                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                        TextButton(onClick = { viewModel.cargarMas() }) {
                                            Text(strings.showMoreProducts, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (viewModel.debeMostrarAnuncios(uiState.userPlan)) {
                    AdBanner(modifier = Modifier.padding(bottom = 8.dp))
                }
            }
        }
    }

    if (showCategoryWarning) {
        AlertDialog(
            onDismissRequest = { showCategoryWarning = false },
            title = { Text(strings.categoryRequiredTitle, fontWeight = FontWeight.Black) },
            text = { Text(strings.categoryRequiredDesc) },
            confirmButton = {
                Button(onClick = { 
                    showCategoryWarning = false
                    onNavigateToCategorias() 
                }) {
                    Text(strings.createCategoryNow)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryWarning = false }) { Text(strings.cancel) }
            },
            shape = MaterialTheme.shapes.extraLarge
        )
    }
}

/**
 * Animación de entrada suave para los ítems de la lista.
 */
@Composable
fun AnimatedItemEntry(content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 4 }
    ) {
        content()
    }
}

/**
 * Card de Ítem Refinada - Diseño Senior Premium.
 */
@Composable
fun ModernItemCard(
    item: Item,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = CustomShapes.CardShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (!item.imagenUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.imagenUrl,
                        contentDescription = item.nombre,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                }
                
                // Badge de Estado Superior Derecho (Minimalista)
                Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.TopEnd) {
                    val stockDisponible = (item.cantidad - item.unidadesAlquiladas).coerceAtLeast(0)
                    val estadoTexto = when {
                        item.estado == "MANTENIMIENTO" -> "Reparación"
                        item.unidadesAlquiladas > 0 && stockDisponible > 0 -> "En Uso (${item.unidadesAlquiladas})"
                        item.unidadesAlquiladas >= item.cantidad -> "Alquilado"
                        else -> "Disponible"
                    }
                    val estadoColor = when {
                        item.estado == "MANTENIMIENTO" -> Color(0xFF64748B)
                        item.unidadesAlquiladas > 0 && stockDisponible > 0 -> Color(0xFF3B82F6) // Azul para parcial
                        item.unidadesAlquiladas >= item.cantidad -> Color(0xFFF59E0B) // Ambar para total
                        else -> Color(0xFF10B981) // Verde para disponible
                    }
                    
                    EstadoBadge(
                        texto = estadoTexto,
                        color = estadoColor
                    )
                }
            }

            // Información de Identidad
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.nombre,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.codigo,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Pie de Tarjeta: Finanzas y Stock
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "S/. " + String.format(java.util.Locale.US, "%.2f", item.precio),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                val stockDisponible = (item.cantidad - item.unidadesAlquiladas).coerceAtLeast(0)
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = CircleShape
                ) {
                    Text(
                        text = "$stockDisponible / ${item.cantidad} und.",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}
