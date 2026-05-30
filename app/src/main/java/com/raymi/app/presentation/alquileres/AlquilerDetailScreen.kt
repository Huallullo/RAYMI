package com.raymi.app.presentation.alquileres

import androidx.compose.foundation.horizontalScroll
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
import com.raymi.app.core.utils.formatTo
import com.raymi.app.domain.model.EstadoAlquiler
import com.raymi.app.domain.model.EstadoComprobante
import com.raymi.app.domain.model.MetodoPago
import com.raymi.app.domain.model.Comprobante
import com.raymi.app.presentation.components.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.raymi.app.core.lang.LocalRaymiStrings

/**
 * Detalle de Alquiler Premium.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlquilerDetailScreen(
    alquilerId: String,
    viewModel: AlquilerDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onGenerateComprobante: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val strings = LocalRaymiStrings.current
    var showDevolucionDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var comprobanteParaAnular by remember { mutableStateOf<String?>(null) }
    var showPagoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.successMessage, uiState.error) {
        uiState.successMessage?.let {
            val msg = when(it) {
                "Abono registrado" -> strings.successPayment
                "Alquiler actualizado correctamente" -> strings.successUpdate
                "Devolución registrada correctamente" -> strings.successUpdate
                else -> it
            }
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
        uiState.error?.let {
            if (uiState.alquiler != null) {
                val msg = when {
                    it.contains("saldo pendiente") -> strings.errorPendingBalance
                    it.contains("Stock insuficiente") -> strings.errorInsufficientStock
                    it.contains("red") || it.contains("network") -> strings.errorNetwork
                    else -> it
                }
                snackbarHostState.showSnackbar(msg)
                viewModel.clearMessages()
            }
        }
    }

    LaunchedEffect(alquilerId) {
        viewModel.loadAlquiler()
    }

    // Abrir PDF automáticamente cuando se genera uno nuevo desde la barra superior
    LaunchedEffect(uiState.pdfUri) {
        uiState.pdfUri?.let { uri ->
            viewModel.abrirPdf(uri)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "Detalle de Alquiler" else "Rental Detail", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = strings.edit)
                    }
                    IconButton(onClick = { viewModel.generarPdf() }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export")
                    }
                }
            )
        },
        bottomBar = {
            val estado = uiState.alquiler?.estado
            if (estado == EstadoAlquiler.ACTIVO || estado == EstadoAlquiler.RESERVADO) {
                Surface(tonalElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { showDevolucionDialog = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = MaterialTheme.shapes.extraLarge
                        ) {
                            Icon(Icons.AutoMirrored.Filled.AssignmentReturn, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                            Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "Registrar Devolución" else "Register Return", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { showCancelDialog = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                            Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "Cancelar Alquiler" else "Cancel Rental", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            val alquiler = uiState.alquiler
            if (alquiler != null) {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // 1. Estado y Finanzas
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "COSTO ALQUILER" else "RENTAL COST", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(alquiler.precioFormateado, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        }
                        EstadoBadge(
                            texto = if (alquiler.estaVencido) (if (strings is com.raymi.app.core.lang.SpanishStrings) "VENCIDO" else "OVERDUE") else alquiler.estado.name,
                            color = if (alquiler.estaVencido) Color(0xFFF44336) else Color(0xFF4CAF50)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Surface(modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(strings.guarantee, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text(alquiler.garantiaFormateada, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (alquiler.penalidad > 0) {
                            Surface(modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "PENALIDAD" else "PENALTY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    Text(alquiler.penalidadFormateada, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = if (alquiler.saldoPendienteReal > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f) else Color(0xFFE8F5E9)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    if (alquiler.saldoPendienteReal > 0) strings.balance.uppercase() else (if (strings is com.raymi.app.core.lang.SpanishStrings) "LIQUIDADO" else "SETTLED"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (alquiler.saldoPendienteReal > 0) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                                )
                                Text(
                                    alquiler.saldoFormateado,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = if (alquiler.saldoPendienteReal > 0) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                                )
                            }
                            if (alquiler.saldoPendienteReal > 0) {
                                Button(
                                    onClick = { showPagoDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = MaterialTheme.shapes.medium
                                ) { Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "Abonar" else "Pay", fontWeight = FontWeight.Bold) }
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(32.dp))
                            }
                        }
                    }

                    Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "Información del Cliente" else "Client Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                                Text("ID: ${alquiler.clienteDni}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "Detalle del Ítem" else "Item Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                                Text("${if (strings is com.raymi.app.core.lang.SpanishStrings) "Cant" else "Qty"}: ${alquiler.cantidad}", style = MaterialTheme.typography.bodyMedium)
                                Text("${if (strings is com.raymi.app.core.lang.SpanishStrings) "Unitario" else "Unit"}: ${alquiler.precioUnitario}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = if (alquiler.estaVencido) 0.2f else 0.05f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(20.dp))
                                Text("${if (strings is com.raymi.app.core.lang.SpanishStrings) "Entrega" else "Pickup"}: ${alquiler.fechaInicioFormatted}")
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.NotificationImportant, contentDescription = null, modifier = Modifier.size(20.dp), tint = if (alquiler.estaVencido) Color(0xFFF44336) else MaterialTheme.colorScheme.onSurface)
                                Text("${if (strings is com.raymi.app.core.lang.SpanishStrings) "Retorno" else "Return"}: ${alquiler.fechaFinFormatted}", color = if (alquiler.estaVencido) Color(0xFFF44336) else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    Text(strings.generatedReceipts, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (uiState.comprobantes.isEmpty()) {
                        Text(strings.noReceipts, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.comprobantes.forEach { comprobante ->
                                ComprobanteListItem(
                                    comprobante = comprobante,
                                    onClick = { 
                                        val uriStr = comprobante.pdfUrl
                                        if (!uriStr.isNullOrBlank()) {
                                            try {
                                                viewModel.abrirPdf(android.net.Uri.parse(uriStr))
                                            } catch (e: Exception) {
                                                android.util.Log.e("AlquilerDetail", "Error al abrir PDF: ${e.message}")
                                            }
                                        }
                                    },
                                    onShare = { 
                                        val uriStr = comprobante.pdfUrl
                                        if (!uriStr.isNullOrBlank()) {
                                            viewModel.compartirPdf(android.net.Uri.parse(uriStr))
                                        }
                                    },
                                    onAnular = { comprobanteParaAnular = comprobante.id },
                                    voidLabel = strings.voidReceipt,
                                    shareLabel = strings.shareReceipt
                                )
                            }
                        }
                    }
                    
                    var showDuplicateWarning by remember { mutableStateOf(false) }

                    OutlinedButton(
                        onClick = { 
                            if (uiState.comprobantes.isNotEmpty()) showDuplicateWarning = true
                            else onGenerateComprobante(alquiler.id) 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.generateReceipt)
                    }

                    if (showDuplicateWarning) {
                        AlertDialog(
                            onDismissRequest = { showDuplicateWarning = false },
                            title = { Text(strings.error + "!") },
                            text = { Text(strings.duplicateReceiptWarning) },
                            confirmButton = {
                                Button(onClick = { 
                                    showDuplicateWarning = false
                                    onGenerateComprobante(alquiler.id) 
                                }) { Text(strings.continueText) }
                            },
                            dismissButton = { TextButton(onClick = { showDuplicateWarning = false }) { Text(strings.cancel) } }
                        )
                    }

                    // 6. Historial de Pagos
                    Text(strings.history, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (uiState.pagos.isEmpty()) {
                        Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "Sin abonos registrados" else "No payments registered", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                uiState.pagos.forEach { pago ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Surface(
                                                shape = CircleShape,
                                                color = when(pago.metodoPago) {
                                                    MetodoPago.EFECTIVO -> Color(0xFF4CAF50)
                                                    MetodoPago.YAPE -> Color(0xFF8E24AA)
                                                    MetodoPago.PLIN -> Color(0xFF00BCD4)
                                                    else -> MaterialTheme.colorScheme.primary
                                                }.copy(alpha = 0.1f),
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = when(pago.metodoPago) {
                                                            MetodoPago.EFECTIVO -> Icons.Default.Payments
                                                            MetodoPago.TRANSFERENCIA -> Icons.Default.AccountBalance
                                                            MetodoPago.TARJETA -> Icons.Default.CreditCard
                                                            else -> Icons.Default.PhoneAndroid
                                                        },
                                                        contentDescription = null,
                                                        tint = when(pago.metodoPago) {
                                                            MetodoPago.EFECTIVO -> Color(0xFF4CAF50)
                                                            MetodoPago.YAPE -> Color(0xFF8E24AA)
                                                            MetodoPago.PLIN -> Color(0xFF00BCD4)
                                                            else -> MaterialTheme.colorScheme.primary
                                                        },
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Column {
                                                Text(text = "S/. ${pago.monto}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                Text(
                                                    text = if (pago.referencia.isNotBlank()) "${pago.metodoPago.name} • ${pago.referencia}" else pago.metodoPago.name,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Text(
                                            text = pago.fecha.toDate().formatTo("dd/MM/yyyy HH:mm"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    if (uiState.pagos.last() != pago) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(200.dp))
                }
            } else if (uiState.isLoading) {
                RaymiLoadingIndicator(message = strings.loading)
            } else if (uiState.error != null) {
                RaymiErrorState(message = uiState.error!!, onRetry = { viewModel.loadAlquiler() })
            }
        }
    }

    if (showPagoDialog && uiState.alquiler != null) {
        var montoAbono by remember { mutableStateOf(uiState.alquiler!!.saldo.toString()) }
        var metodoSeleccionado by remember { mutableStateOf(MetodoPago.EFECTIVO) }
        var referencia by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPagoDialog = false },
            title = { Text(strings.registerAbono, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(strings.abonoDesc)
                    OutlinedTextField(
                        value = montoAbono,
                        onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() || c == '.' }) montoAbono = it },
                        label = { Text(strings.price) },
                        modifier = Modifier.fillMaxWidth(),
                        prefix = { Text("S/. ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = MaterialTheme.shapes.large
                    )
                    
                    Text(strings.paymentMethod, style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetodoPago.entries.forEach { metodo ->
                            FilterChip(
                                selected = metodoSeleccionado == metodo,
                                onClick = { metodoSeleccionado = metodo },
                                label = { Text(metodo.name) },
                                leadingIcon = {
                                    if (metodoSeleccionado == metodo) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            )
                        }
                    }

                    if (metodoSeleccionado != MetodoPago.EFECTIVO) {
                        OutlinedTextField(
                            value = referencia,
                            onValueChange = { referencia = it },
                            label = { Text(strings.operationNumber) },
                            placeholder = { Text("Ej: 123456") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val monto = montoAbono.toDoubleOrNull() ?: 0.0
                    if (monto > 0) {
                        viewModel.registrarPago(monto, metodoSeleccionado, referencia)
                        showPagoDialog = false
                    }
                }) { Text(strings.confirmPayment) }
            },
            dismissButton = { TextButton(onClick = { showPagoDialog = false }) { Text(strings.cancel) } }
        )
    }

    if (comprobanteParaAnular != null) {
        AlertDialog(
            onDismissRequest = { comprobanteParaAnular = null },
            title = { Text(strings.voidReceipt) },
            text = { Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "¿Estás seguro de anular este comprobante? Esta acción no se puede deshacer." else "Are you sure you want to void this receipt? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { 
                        comprobanteParaAnular?.let { viewModel.anularComprobante(it) }
                        comprobanteParaAnular = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "Confirmar Anulación" else "Confirm Void") }
            },
            dismissButton = { TextButton(onClick = { comprobanteParaAnular = null }) { Text(strings.back) } }
        )
    }

    if (showDevolucionDialog && uiState.alquiler != null) {
        val alquiler = uiState.alquiler!!
        var penalidadStr by remember { mutableStateOf("0") }
        var garantiaRetenidaStr by remember { mutableStateOf("0") }
        var observacionesDev by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showDevolucionDialog = false },
            title = { Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "Finalizar Alquiler" else "Finish Rental") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (alquiler.saldoPendienteReal > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Column {
                                    Text(
                                        "${strings.balance}: S/. ${String.format(java.util.Locale.US, "%.2f", alquiler.saldoPendienteReal)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        if (strings is com.raymi.app.core.lang.SpanishStrings) "Debes liquidar la deuda antes de registrar la devolución." else "You must settle the debt before registering the return.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    } else {
                        Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "Confirma la recepción del ítem." else "Confirm item reception.")
                        OutlinedTextField(
                            value = penalidadStr,
                            onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() || c == '.' }) penalidadStr = it },
                            label = { Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "Penalidad Extra (S/.)" else "Extra Penalty (S/.)") },
                            modifier = Modifier.fillMaxWidth(),
                            prefix = { Text("S/. ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = MaterialTheme.shapes.large
                        )
                        OutlinedTextField(
                            value = garantiaRetenidaStr,
                            onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() || c == '.' }) garantiaRetenidaStr = it },
                            label = { Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "Descuento de garantía (S/.)" else "Guarantee deduction (S/.)") },
                            modifier = Modifier.fillMaxWidth(),
                            prefix = { Text("S/. ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = MaterialTheme.shapes.large
                        )
                        OutlinedTextField(
                            value = observacionesDev,
                            onValueChange = { observacionesDev = it },
                            label = { Text(strings.notes) },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            shape = MaterialTheme.shapes.large
                        )
                    }
                }
            },
            confirmButton = {
                if (alquiler.saldoPendienteReal > 0) {
                    Button(onClick = {
                        showDevolucionDialog = false
                        showPagoDialog = true
                    }) {
                        Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "Ir a Cobrar" else "Go to Checkout")
                    }
                } else {
                    Button(onClick = { 
                        viewModel.registrarDevolucion(
                            penalidad = penalidadStr.toDoubleOrNull() ?: 0.0,
                            observaciones = observacionesDev,
                            montoGarantiaRetenida = garantiaRetenidaStr.toDoubleOrNull() ?: 0.0
                        ) 
                        showDevolucionDialog = false
                    }) { Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "Confirmar Devolución" else "Confirm Return") }
                }
            },
            dismissButton = { TextButton(onClick = { showDevolucionDialog = false }) { Text(strings.cancel) } }
        )
    }

    if (showCancelDialog) {
        var motivoCancel by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "Cancelar Alquiler" else "Cancel Rental") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "¿Estás seguro de cancelar este alquiler? El stock se liberará." else "Are you sure you want to cancel? Stock will be released.")
                    OutlinedTextField(
                        value = motivoCancel,
                        onValueChange = { motivoCancel = it },
                        label = { Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "Motivo de cancelación" else "Cancellation reason") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = MaterialTheme.shapes.large
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        viewModel.cancelarAlquiler(motivoCancel)
                        showCancelDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "Confirmar Cancelación" else "Confirm Cancellation") }
            },
            dismissButton = { TextButton(onClick = { showCancelDialog = false }) { Text(strings.back) } }
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

@Composable
fun ComprobanteListItem(
    comprobante: Comprobante,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onAnular: () -> Unit,
    voidLabel: String,
    shareLabel: String
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = if (comprobante.estado == EstadoComprobante.ANULADO) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "${comprobante.tipo}: ${comprobante.correlativoCompleto}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (comprobante.estado == EstadoComprobante.ANULADO) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Total: S/. ${comprobante.total}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    EstadoBadge(texto = comprobante.estado.name, color = when(comprobante.estado) { EstadoComprobante.GENERADO -> Color(0xFF4CAF50); EstadoComprobante.COMPARTIDO -> Color(0xFF2196F3); EstadoComprobante.ANULADO -> Color(0xFFF44336); EstadoComprobante.GENERANDO -> Color(0xFFFF9800); else -> MaterialTheme.colorScheme.outline })
                }
            }
            if (comprobante.estado == EstadoComprobante.GENERADO || comprobante.estado == EstadoComprobante.COMPARTIDO || !comprobante.pdfUrl.isNullOrBlank()) {
                if (comprobante.estado != EstadoComprobante.ANULADO) {
                    IconButton(onClick = onAnular) {
                        Icon(Icons.Default.DeleteForever, contentDescription = voidLabel, tint = MaterialTheme.colorScheme.error)
                    }
                    IconButton(onClick = onShare) { 
                        Icon(Icons.Default.Share, contentDescription = shareLabel, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
