package com.raymi.app.presentation.alquileres

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raymi.app.core.theme.CustomShapes
import com.raymi.app.core.utils.formatTo
import com.raymi.app.domain.model.Cliente
import com.raymi.app.domain.model.Item
import com.raymi.app.presentation.clientes.ModernClienteItem
import com.raymi.app.presentation.components.*
import java.util.*

/**
 * Pantalla de Creación de Alquiler Premium.
 * Diseño Senior: Proceso guiado, cálculos automatizados y estética moderna.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAlquilerScreen(
    viewModel: CreateAlquilerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    // QA Fix: Feedback visual proactivo
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.error, uiState.successMessage) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Registrar Alquiler", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Selección de Actores (Cliente e Ítem)
            SelectionSection(
                title = "Quién y Qué",
                content = {
                    SelectionTile(
                        label = "Cliente",
                        value = uiState.selectedCliente?.nombreCompleto ?: "Seleccionar Cliente",
                        icon = Icons.Default.Person,
                        isSelected = uiState.selectedCliente != null,
                        onClick = { viewModel.showClienteDialog() }
                    )
                    
                    SelectionTile(
                        label = "Producto / Servicio",
                        value = uiState.selectedItem?.nombre ?: "Seleccionar Ítem",
                        icon = Icons.Default.Inventory2,
                        isSelected = uiState.selectedItem != null,
                        onClick = { viewModel.showItemDialog() }
                    )
                }
            )

            // 2. Tiempos y Duración
            SelectionSection(
                title = "Periodo de Alquiler",
                content = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DatePickerField(
                            label = "Entrega",
                            date = uiState.fechaInicio,
                            modifier = Modifier.weight(1f),
                            onDateSelected = { viewModel.setFechaInicio(it) }
                        )
                        DatePickerField(
                            label = "Devolución",
                            date = uiState.fechaFin,
                            modifier = Modifier.weight(1f),
                            onDateSelected = { viewModel.setFechaFin(it) }
                        )
                    }
                    if (uiState.diasAlquiler > 0) {
                        Text(
                            "Duración estimada: ${uiState.diasAlquiler} días",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )

            // 3. Resumen Económico
            SelectionSection(
                title = "Liquidación y Pagos",
                content = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = uiState.cantidad,
                                onValueChange = viewModel::onCantidadChange,
                                label = { Text("Cantidad") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = MaterialTheme.shapes.large
                            )
                            OutlinedTextField(
                                value = String.format(Locale.getDefault(), "%.2f", uiState.precioTotal),
                                onValueChange = {},
                                label = { Text("Monto Total") },
                                modifier = Modifier.weight(1f),
                                readOnly = true,
                                prefix = { Text("S/. ") },
                                shape = MaterialTheme.shapes.large,
                                colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                            )
                        }
                        
                        OutlinedTextField(
                            value = uiState.adelanto,
                            onValueChange = viewModel::onAdelantoChange,
                            label = { Text("Adelanto Recibido") },
                            modifier = Modifier.fillMaxWidth(),
                            prefix = { Text("S/. ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = MaterialTheme.shapes.large
                        )
                        
                        if (uiState.saldo > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Saldo pendiente: S/. ${String.format(Locale.getDefault(), "%.2f", uiState.saldo)}",
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            )

            // 4. Notas Finales
            OutlinedTextField(
                value = uiState.observaciones,
                onValueChange = viewModel::onObservacionesChange,
                label = { Text("Notas adicionales") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = MaterialTheme.shapes.large
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.crearAlquiler() },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = MaterialTheme.shapes.extraLarge,
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.TaskAlt, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("Confirmar Alquiler", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            
            if (uiState.error != null) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }

    if (uiState.showClienteDialog) {
        GenericSelectionDialog(
            title = "Seleccionar Cliente",
            items = uiState.clientes,
            onDismiss = { viewModel.hideClienteDialog() },
            itemContent = { cliente ->
                ModernClienteItem(cliente = cliente, onClick = { viewModel.seleccionarCliente(cliente) })
            }
        )
    }

    if (uiState.showItemDialog) {
        GenericSelectionDialog(
            title = "Seleccionar Producto",
            items = uiState.itemsDisponibles,
            onDismiss = { viewModel.hideItemDialog() },
            header = {
                // QA: Filtro por categoría dentro del diálogo
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = uiState.categoriaFiltro == null,
                            onClick = { viewModel.filtrarPorCategoria(null) },
                            label = { Text("Todos") },
                            shape = CircleShape
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
            },
            itemContent = { item ->
                Surface(
                    onClick = { viewModel.seleccionarItem(item) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(item.nombre, fontWeight = FontWeight.Bold)
                            Text("S/. ${item.precio} • SKU: ${item.codigo}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun SelectionSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
fun SelectionTile(label: String, value: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun DatePickerField(label: String, date: Date?, modifier: Modifier, onDateSelected: (Date) -> Unit) {
    val context = LocalContext.current
    OutlinedTextField(
        value = date?.formatTo("dd/MM/yyyy") ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        modifier = modifier.clickable {
            val cal = Calendar.getInstance()
            date?.let { cal.time = it }
            DatePickerDialog(context, { _, y, m, d ->
                cal.set(y, m, d)
                onDateSelected(cal.time)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        },
        enabled = false,
        shape = MaterialTheme.shapes.large,
        colors = TextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant)
    )
}

@Composable
fun <T> GenericSelectionDialog(
    title: String, 
    items: List<T>, 
    onDismiss: () -> Unit, 
    header: @Composable (() -> Unit)? = null, // QA Fix: Añadir header para filtros
    itemContent: @Composable (T) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                header?.invoke()
                
                if (items.isEmpty()) {
                    Text("No hay elementos disponibles en este momento.", style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(items) { itemContent(it) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}
