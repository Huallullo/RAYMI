package com.raymi.app.presentation.historial

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.domain.model.EstadoAlquiler
import com.raymi.app.presentation.components.*
import java.util.Locale

/**
 * Historial de Movimientos Premium.
 * Diseño Senior: Resumen de ingresos acumulados y lista de auditoría limpia.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    viewModel: HistorialViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Historial Contable", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.exportarInventario() }) {
                        Icon(Icons.Default.Inventory, contentDescription = "Exportar Inventario")
                    }
                    IconButton(onClick = { viewModel.cargarHistorial() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            
            // 1. Panel de Resumen Histórico (Diseño Senior)
            SummaryHeader(recaudado = uiState.totalRecaudado, totalTransacciones = uiState.allAlquileres.size)

            // 2. Buscador en tiempo real
            RaymiSearchBar(
                query = uiState.query,
                onQueryChange = { viewModel.filtrar(it) },
                placeholder = "Buscar en el historial...",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> RaymiLoadingIndicator(message = "Compilando registros...")
                    uiState.error != null -> RaymiErrorState(message = uiState.error!!, onRetry = { viewModel.cargarHistorial() })
                    uiState.filteredAlquileres.isEmpty() -> {
                        RaymiEmptyState(
                            icon = Icons.Default.HistoryEdu,
                            title = "Sin Registros",
                            description = "No se encontraron movimientos cerrados."
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp, start = 24.dp, end = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.filteredAlquileres, key = { it.id }) { movimiento ->
                                TransactionItem(movimiento)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryHeader(recaudado: Double, totalTransacciones: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("RECAUDACIÓN TOTAL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(
                    "S/. ${String.format(Locale.getDefault(), "%,.2f", recaudado)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("MOVIMIENTOS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$totalTransacciones oper.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TransactionItem(alquiler: Alquiler) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icono de transacción terminada
            Surface(
                shape = CircleShape,
                color = if (alquiler.estado == EstadoAlquiler.DEVUELTO) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (alquiler.estado == EstadoAlquiler.DEVUELTO) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (alquiler.estado == EstadoAlquiler.DEVUELTO) Color(0xFF4CAF50) else Color.Red
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alquiler.clienteNombre,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = alquiler.itemNombre,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "S/. ${alquiler.precioTotal - alquiler.saldo}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = alquiler.fechaFinFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
