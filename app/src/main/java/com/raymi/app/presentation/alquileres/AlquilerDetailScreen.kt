package com.raymi.app.presentation.alquileres

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.raymi.app.core.theme.CustomShapes
import com.raymi.app.core.theme.RaymiColors
import com.raymi.app.domain.model.EstadoAlquiler
import com.raymi.app.presentation.components.EstadoBadge
import com.raymi.app.presentation.components.InfoRow
import com.raymi.app.presentation.components.RaymiErrorState
import com.raymi.app.presentation.components.RaymiLoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlquilerDetailScreen(
    alquilerId: String,
    viewModel: AlquilerDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDevolucionDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showPagoDialog by remember { mutableStateOf(false) }


    // Mostrar mensajes
    LaunchedEffect(uiState.error, uiState.successMessage) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessages()
            // Solo navegar atrás si no es mensaje de PDF generado
            if (!message.contains("PDF generado correctamente")) {
                onNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del Alquiler") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (uiState.alquiler?.estado == EstadoAlquiler.ACTIVO) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar")
                        }
                    }
                }

            )
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
                    RaymiLoadingIndicator(message = "Cargando alquiler...")
                }

                uiState.alquiler == null -> {
                    RaymiErrorState(
                        message = "No se pudo cargar el alquiler",
                        onRetry = { /* Recargar */ }
                    )
                }

                else -> {
                    val alquiler = uiState.alquiler!!

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Cabecera con estado
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = CustomShapes.CardShape,
                            colors = CardDefaults.cardColors(
                                containerColor = when (alquiler.estado) {
                                    EstadoAlquiler.ACTIVO -> MaterialTheme.colorScheme.primaryContainer
                                    EstadoAlquiler.DEVUELTO -> RaymiColors.Success.copy(alpha = 0.1f)
                                    EstadoAlquiler.VENCIDO -> RaymiColors.Error.copy(alpha = 0.1f)
                                    EstadoAlquiler.CANCELADO -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )

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
                        }

                        // Cliente
                        Text(
                            text = "Cliente",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = CustomShapes.CardShape
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )

                                Text(
                                    text = alquiler.clienteNombre,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Vestuario
                        Text(
                            text = "Vestuario",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = CustomShapes.CardShape
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Checkroom,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )

                                    Column {
                                        Text(
                                            text = alquiler.vestuarioNombre,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${alquiler.cantidad}x ${alquiler.vestuarioNombre}",  // ✅
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Código: ${alquiler.vestuarioCodigo}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Cantidad: ${alquiler.cantidad} unidad(es)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Fechas
                        Text(
                            text = "Fechas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = CustomShapes.CardShape
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                InfoRow(
                                    icon = Icons.Default.CalendarToday,
                                    label = "Fecha de Inicio",
                                    value = alquiler.fechaInicioFormatted
                                )

                                HorizontalDivider()

                                InfoRow(
                                    icon = Icons.Default.Event,
                                    label = "Fecha de Devolución Prevista",
                                    value = alquiler.fechaFinFormatted
                                )

                                if (alquiler.estado == EstadoAlquiler.ACTIVO) {
                                    HorizontalDivider()

                                    if (alquiler.estaVencido) {
                                        InfoRow(
                                            icon = Icons.Default.Warning,
                                            label = "Estado",
                                            value = "Vencido hace ${-alquiler.diasRestantes} día(s)",
                                            valueColor = RaymiColors.Error
                                        )
                                    } else {
                                        InfoRow(
                                            icon = Icons.Default.Info,
                                            label = "Días Restantes",
                                            value = "${alquiler.diasRestantes} día(s)",
                                            valueColor = if (alquiler.diasRestantes <= 2) {
                                                RaymiColors.Warning
                                            } else {
                                                RaymiColors.Success
                                            }
                                        )
                                        // Hora de creación
                                        val horaCreacion = alquiler.createdAt?.let { timestamp ->
                                            val formatter = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                                            formatter.timeZone = java.util.TimeZone.getDefault()
                                            formatter.format(timestamp.toDate())
                                        } ?: ""

                                        if (horaCreacion.isNotBlank()) {
                                            HorizontalDivider()
                                            InfoRow(
                                                icon = Icons.Filled.Info,
                                                label = "Creado",
                                                value = "${alquiler.fechaInicioFormatted} $horaCreacion",
                                                valueColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Información de pago
                        Text(
                            text = "Información de Pago",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = CustomShapes.CardShape
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Precio Unitario:",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = alquiler.precioUnitarioFormateado,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Cantidad:",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "${alquiler.cantidad} unidad(es)",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                HorizontalDivider()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Precio Total:",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = alquiler.precioFormateado,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Adelanto:",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = alquiler.adelantoFormateado,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = RaymiColors.Success
                                    )
                                }

                                HorizontalDivider()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Saldo:",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = alquiler.saldoFormateado,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (alquiler.saldo > 0) {
                                            RaymiColors.Warning
                                        } else {
                                            RaymiColors.Success
                                        }
                                    )
                                }
                            }
                        }

                        // Observaciones
                        if (alquiler.observaciones.isNotBlank()) {
                            Text(
                                text = "Observaciones",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = CustomShapes.CardShape
                            ) {
                                Text(
                                    text = alquiler.observaciones,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        // Acciones
                        if (alquiler.estado == EstadoAlquiler.ACTIVO || alquiler.estado == EstadoAlquiler.CANCELADO) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Si hay deuda (solo si está ACTIVO, porque CANCELADO ya no tiene deuda)
                                if (alquiler.saldo > 0 && alquiler.estado == EstadoAlquiler.ACTIVO) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = RaymiColors.Warning.copy(alpha = 0.1f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = RaymiColors.Warning,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Deuda Pendiente",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = RaymiColors.Warning
                                                )
                                                Text(
                                                    text = "Saldo: ${alquiler.saldoFormateado}",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = { showPagoDialog = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp),
                                        enabled = !uiState.isProcessing,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = RaymiColors.Warning
                                        )
                                    ) {
                                        Icon(Icons.Default.Payments, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Registrar Pago")
                                    }
                                } else if (alquiler.estado == EstadoAlquiler.CANCELADO) {
                                    // Mensaje informativo de que está pagado y pendiente de devolución
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = RaymiColors.Success.copy(alpha = 0.1f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = RaymiColors.Success,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Pago Completo",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = RaymiColors.Success
                                                )
                                                Text(
                                                    text = "El cliente ha pagado la totalidad. Esperando devolución del vestuario.",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                } else if (alquiler.saldo == 0.0 && alquiler.estado == EstadoAlquiler.ACTIVO) {
                                    // Caso raro: saldo cero pero aún ACTIVO (por si no se actualizó automáticamente)
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = RaymiColors.Success.copy(alpha = 0.1f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = RaymiColors.Success,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Saldo Cero",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = RaymiColors.Success
                                                )
                                                Text(
                                                    text = "El cliente ya pagó todo. Puedes registrar la devolución.",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                }

                                // Botón de devolución: visible si está ACTIVO o CANCELADO (y si no está procesando)
                                Button(
                                    onClick = { showDevolucionDialog = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    enabled = !uiState.isProcessing,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (alquiler.estado == EstadoAlquiler.CANCELADO) RaymiColors.Success else MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    if (uiState.isProcessing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            if (alquiler.estado == EstadoAlquiler.CANCELADO) "Registrar Devolución (Pago completo)"
                                            else "Registrar Devolución"
                                        )
                                    }
                                }
                            }
                        }

                        // Botones para PDF y WhatsApp
                        Text(
                            text = "Exportar y Compartir",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.generarPdf() },
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isProcessing
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Generar PDF")
                            }

                            Button(
                                onClick = { viewModel.compartirPdfPorWhatsApp() },
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isProcessing && uiState.pdfUri != null,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RaymiColors.Info
                                )
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Enviar por WhatsApp")
                            }
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
                        if (showPagoDialog && uiState.alquiler != null) {
                            RegistrarPagoDialog(
                                alquiler = uiState.alquiler!!,
                                onDismiss = { showPagoDialog = false },
                                onConfirm = { montoPago ->
                                    viewModel.registrarPago(montoPago)
                                    showPagoDialog = false
                                },
                                isLoading = uiState.isProcessing
                            )
                        }
                        if (showDevolucionDialog && uiState.alquiler != null) {
                            val alquiler = uiState.alquiler!!

                            AlertDialog(
                                onDismissRequest = { showDevolucionDialog = false },
                                icon = {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                                },
                                title = {
                                    Text("Registrar Devolución")
                                },
                                text = {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        if (alquiler.saldo > 0) {
                                            // Si hay deuda, mostrar advertencia
                                            Text(
                                                "⚠️ El cliente aún tiene un saldo pendiente de ${alquiler.saldoFormateado}.",
                                                color = RaymiColors.Warning
                                            )
                                            Text(
                                                "¿Deseas registrar la devolución de todas formas? Se marcará el alquiler como devuelto pero la deuda quedará registrada."
                                            )
                                        } else {
                                            Text(
                                                "¿Confirmas que el vestuario ha sido devuelto? Esta acción marcará el alquiler como completado."
                                            )
                                        }
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            showDevolucionDialog = false
                                            viewModel.registrarDevolucion()
                                        }
                                    ) {
                                        Text("Confirmar Devolución")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDevolucionDialog = false }) {
                                        Text("Cancelar")
                                    }
                                }
                            )
                        }
                    }
                }
            }

        }

    }

}
