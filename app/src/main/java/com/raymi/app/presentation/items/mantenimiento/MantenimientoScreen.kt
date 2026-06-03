package com.raymi.app.presentation.items.mantenimiento

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raymi.app.domain.model.Mantenimiento
import com.raymi.app.presentation.components.*
import com.raymi.app.core.utils.formatTo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MantenimientoScreen(
    @Suppress("UNUSED_PARAMETER") itemId: String,
    viewModel: MantenimientoViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = com.raymi.app.core.lang.LocalRaymiStrings.current
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(strings.maintenanceHistory, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = strings.registerMaintenance)
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                uiState.isLoading -> RaymiLoadingIndicator()
                uiState.mantenimientos.isEmpty() -> RaymiEmptyState(icon = Icons.Default.Build, title = strings.noMovements, description = if (strings is com.raymi.app.core.lang.SpanishStrings) "Sin registros de mantenimiento." else "No maintenance records.")
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(uiState.mantenimientos) { maintenance ->
                            MaintenanceCard(maintenance)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddMaintenanceDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { maintenance ->
                viewModel.addMantenimiento(maintenance)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun MaintenanceCard(maintenance: Mantenimiento) {
    val strings = com.raymi.app.core.lang.LocalRaymiStrings.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = maintenance.fecha.formatTo("dd/MM/yyyy"), fontWeight = FontWeight.Bold)
                EstadoBadge(
                    texto = maintenance.estadoFinal,
                    color = when(maintenance.estadoFinal) {
                        "OPERATIVO" -> Color(0xFF4CAF50)
                        "MANTENIMIENTO" -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                )
            }
            Text(text = maintenance.motivo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            if (maintenance.descripcion.isNotBlank()) {
                Text(text = maintenance.descripcion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "${strings.cost}: S/. ${maintenance.costo}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "${strings.responsible.take(4)}: ${maintenance.responsable}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun AddMaintenanceDialog(onDismiss: () -> Unit, onConfirm: (Mantenimiento) -> Unit) {
    val strings = com.raymi.app.core.lang.LocalRaymiStrings.current
    var motivo by remember { mutableStateOf("") }
    var costo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var responsable by remember { mutableStateOf("") }
    var estadoFinal by remember { mutableStateOf("OPERATIVO") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.registerMaintenance, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = motivo, onValueChange = { motivo = it }, label = { Text(strings.reason) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = costo, onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() || c == '.' }) costo = it }, label = { Text("${strings.cost} (S/.)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                OutlinedTextField(value = responsable, onValueChange = { responsable = it }, label = { Text(strings.responsible) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = descripcion, onValueChange = { descripcion = it }, label = { Text(strings.description) }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
                
                Text(strings.finalState, style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("OPERATIVO", "MANTENIMIENTO", "DE_BAJA").forEach { estado ->
                        FilterChip(
                            selected = estadoFinal == estado,
                            onClick = { estadoFinal = estado },
                            label = { Text(estado, fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(Mantenimiento(motivo = motivo, costo = costo.toDoubleOrNull() ?: 0.0, descripcion = descripcion, responsable = responsable, estadoFinal = estadoFinal))
            }, enabled = motivo.isNotBlank()) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

// Dummy import for 10.sp if missing
private val Int.sp get() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)
