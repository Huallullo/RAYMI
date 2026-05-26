package com.raymi.app.presentation.alquileres

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raymi.app.domain.model.EstadoAlquiler
import com.raymi.app.presentation.components.*

/**
 * Detalle de Alquiler Premium.
 * Diseño Senior: Separación clara de responsabilidades, estados financieros y gestión de producto.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlquilerDetailScreen(
    alquilerId: String,
    viewModel: AlquilerDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDevolucionDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    // Observar mensajes
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

    LaunchedEffect(alquilerId) {
        viewModel.loadAlquiler()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Contrato de Alquiler", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = { viewModel.generarPdf() }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Exportar")
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.alquiler != null && uiState.alquiler?.estado == EstadoAlquiler.ACTIVO) {
                Surface(tonalElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { showDevolucionDialog = true },
                        modifier = Modifier.padding(24.dp).fillMaxWidth().height(56.dp),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Icon(Icons.AutoMirrored.Filled.AssignmentReturn, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text("Registrar Devolución", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                uiState.isLoading -> RaymiLoadingIndicator(message = "Consultando contrato...")
                uiState.error != null -> RaymiErrorState(message = uiState.error!!, onRetry = { viewModel.loadAlquiler() })
                uiState.alquiler != null -> {
                    val alquiler = uiState.alquiler!!
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // 1. Estado y Finanzas
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("MONTO TOTAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text(alquiler.precioFormateado, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                            }
                            EstadoBadge(
                                texto = if (alquiler.estaVencido) "VENCIDO" else alquiler.estado.name,
                                color = if (alquiler.estaVencido) Color(0xFFF44336) else Color(0xFF4CAF50)
                            )
                        }

                        // QA Fix: Resumen de Saldo y Pago
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = if (alquiler.saldo > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f) else Color(0xFFE8F5E9)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        if (alquiler.saldo > 0) "SALDO PENDIENTE" else "PAGO COMPLETADO",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (alquiler.saldo > 0) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                                    )
                                    Text(
                                        if (alquiler.saldo > 0) "S/. ${alquiler.saldo}" else "Todo pagado",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = if (alquiler.saldo > 0) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                                    )
                                }
                                
                                if (alquiler.saldo > 0) {
                                    Button(
                                        onClick = { viewModel.liquidarDeuda() },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        Text("Liquidar", fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(32.dp))
                                }
                            }
                        }

                        // 2. Información del Cliente
                        Text("Cliente Subscrito", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(48.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(alquiler.clienteNombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("DNI: ${alquiler.clienteDni}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // 3. Producto en Alquiler
                        Text("Ítem en Alquiler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(alquiler.itemNombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("SKU: ${alquiler.itemCodigo}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("Cantidad: ${alquiler.cantidad}", style = MaterialTheme.typography.bodyMedium)
                                    Text("Unitario: ${alquiler.precioUnitario}", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        // 4. Fechas Críticas
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = if (alquiler.estaVencido) 0.2f else 0.05f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Text("Entrega: ${alquiler.fechaInicioFormatted}")
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.NotificationImportant, 
                                        contentDescription = null, 
                                        modifier = Modifier.size(20.dp),
                                        tint = if (alquiler.estaVencido) Color(0xFFF44336) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text("Devolución: ${alquiler.fechaFinFormatted}", color = if (alquiler.estaVencido) Color(0xFFF44336) else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    if (showDevolucionDialog) {
        AlertDialog(
            onDismissRequest = { showDevolucionDialog = false },
            title = { Text("¿Registrar Devolución?") },
            text = { Text("El ítem volverá a estar disponible en tu inventario y el contrato se marcará como DEVUELTO.") },
            confirmButton = {
                Button(onClick = { 
                    viewModel.registrarDevolucion() 
                    showDevolucionDialog = false
                }) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { showDevolucionDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showEditDialog && uiState.alquiler != null) {
        EditAlquilerDialog(
            alquiler = uiState.alquiler!!,
            onDismiss = { showEditDialog = false },
            onConfirm = { alquilerActualizado ->
                viewModel.updateAlquiler(alquilerActualizado)
                showEditDialog = false
            },
            isLoading = uiState.isProcessing
        )
    }
}
