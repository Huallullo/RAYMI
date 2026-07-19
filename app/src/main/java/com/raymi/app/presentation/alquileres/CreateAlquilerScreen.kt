package com.raymi.app.presentation.alquileres

import android.app.DatePickerDialog
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.raymi.app.core.ads.AdInterstitialManager
import com.raymi.app.core.utils.formatTo
import com.raymi.app.domain.model.Cliente
import com.raymi.app.domain.model.Item
import com.raymi.app.presentation.clientes.ModernClienteItem
import com.raymi.app.presentation.components.*
import java.util.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.testTag
import com.raymi.app.core.lang.LocalRaymiStrings
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAlquilerScreen(
    adInterstitialManager: com.raymi.app.core.ads.AdInterstitialManager,
    viewModel: CreateAlquilerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val strings = LocalRaymiStrings.current
    val context = LocalContext.current

    LaunchedEffect(uiState.shouldShowInterstitial) {
        if (uiState.shouldShowInterstitial) {
            val activity = context as? android.app.Activity
            activity?.let {
                adInterstitialManager.showAd(it) {
                    viewModel.onInterstitialShown()
                }
            }
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onNavigateBack()
    }

    LaunchedEffect(uiState.error, uiState.successMessage) {
        uiState.error?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
        uiState.successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(strings.createRental, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.search)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(scrollState).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SelectionSection(
                title = strings.clients,
                content = {
                    SelectionTile(
                        label = strings.clients,
                        value = uiState.selectedCliente?.nombreCompleto ?: strings.selectClient,
                        icon = Icons.Default.Person,
                        isSelected = uiState.selectedCliente != null,
                        onClick = { viewModel.showClienteDialog() },
                        modifier = Modifier.testTag("alquiler_select_cliente")
                    )
                }
            )

            SelectionSection(
                title = strings.inventory,
                content = {
                    uiState.selectedItems.forEachIndexed { index, item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.itemNombre, fontWeight = FontWeight.Bold)
                                    Text("${strings.units}: ${item.cantidad} • ${strings.subtotal}: S/. ${item.subtotal}", style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { viewModel.removerItem(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    TextButton(
                        onClick = { viewModel.showItemDialog() }, 
                        modifier = Modifier.fillMaxWidth().testTag("alquiler_add_item"),
                        enabled = uiState.fechaFin != null
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (uiState.fechaFin == null) (if (strings is com.raymi.app.core.lang.SpanishStrings) "Primero elige fechas" else "Select dates first") else strings.selectItem)
                    }
                }
            )

            SelectionSection(
                title = strings.rentalPeriod,
                content = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DatePickerField(strings.startDate, uiState.fechaInicio, Modifier.weight(1f)) { viewModel.setFechaInicio(it) }
                        DatePickerField(strings.endDate, uiState.fechaFin, Modifier.weight(1f).testTag("alquiler_fecha_fin")) { viewModel.setFechaFin(it) }
                    }
                    if (uiState.diasAlquiler > 0) {
                        Text("${strings.rentals}: ${uiState.diasAlquiler} ${strings.all}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            )

            SelectionSection(
                title = strings.subscription, // Using subscription for lack of generic finance label
                content = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = String.format(Locale.getDefault(), "%.2f", uiState.precioTotal),
                            onValueChange = {},
                            label = { Text(strings.totalRental) },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            prefix = { Text("S/. ") },
                            shape = MaterialTheme.shapes.large,
                            colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = uiState.adelanto,
                                onValueChange = viewModel::onAdelantoChange,
                                label = { Text(strings.advance) },
                                modifier = Modifier.weight(1f).testTag("alquiler_adelanto_input"),
                                prefix = { Text("S/. ") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = MaterialTheme.shapes.large
                            )
                            OutlinedTextField(
                                value = uiState.garantia,
                                onValueChange = viewModel::onGarantiaChange,
                                label = { Text(strings.guarantee) },
                                modifier = Modifier.weight(1f),
                                prefix = { Text("S/. ") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = MaterialTheme.shapes.large
                            )
                        }
                        if (uiState.saldo > 0) {
                            Surface(color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f), shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                                Text("${strings.balance}: S/. ${String.format(Locale.getDefault(), "%,.2f", uiState.saldo)}", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(strings.paymentMethod, style = MaterialTheme.typography.labelSmall)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            com.raymi.app.domain.model.MetodoPago.entries.forEach { metodo ->
                                FilterChip(
                                    selected = uiState.metodoPago == metodo,
                                    onClick = { viewModel.onMetodoPagoChange(metodo) },
                                    label = { Text(metodo.name) }
                                )
                            }
                        }
                    }
                }
            )

            SelectionSection(
                title = strings.initialStatus,
                content = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = uiState.estadoInicial == com.raymi.app.domain.model.EstadoAlquiler.ACTIVO,
                            onClick = { viewModel.setEstadoInicial(com.raymi.app.domain.model.EstadoAlquiler.ACTIVO) },
                            label = { Text(strings.active) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = uiState.estadoInicial == com.raymi.app.domain.model.EstadoAlquiler.RESERVADO,
                            onClick = { viewModel.setEstadoInicial(com.raymi.app.domain.model.EstadoAlquiler.RESERVADO) },
                            label = { Text(strings.reserved) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            )

            OutlinedTextField(value = uiState.observaciones, onValueChange = viewModel::onObservacionesChange, label = { Text(strings.notes) }, modifier = Modifier.fillMaxWidth(), minLines = 2, shape = MaterialTheme.shapes.large)
            Button(onClick = { viewModel.crearAlquiler() }, modifier = Modifier.fillMaxWidth().height(58.dp).testTag("alquiler_confirmar_button"), shape = MaterialTheme.shapes.extraLarge, enabled = !uiState.isLoading) {
                if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                else { Icon(Icons.Default.TaskAlt, null); Spacer(Modifier.width(12.dp)); Text(strings.confirmRental, fontWeight = FontWeight.Bold) }
            }
        }
    }

    if (uiState.showClienteDialog) {
        GenericSelectionDialog(strings.selectClient, uiState.clientes, { viewModel.hideClienteDialog() }) { cliente ->
            ModernClienteItem(
                cliente = cliente, 
                onClick = { viewModel.seleccionarCliente(cliente) },
                modifier = Modifier.testTag("client_option")
            )
        }
    }

    if (uiState.showItemDialog) {
        GenericSelectionDialog(
            title = strings.selectItem,
            items = uiState.itemsDisponibles,
            onDismiss = { viewModel.hideItemDialog() },
            header = {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    item { FilterChip(selected = uiState.categoriaFiltro == null, onClick = { viewModel.filtrarPorCategoria(null) }, label = { Text(strings.all) }) }
                    items(uiState.categorias) { categoria -> FilterChip(selected = uiState.categoriaFiltro?.id == categoria.id, onClick = { viewModel.filtrarPorCategoria(categoria) }, label = { Text(categoria.nombre) }) }
                }
            },
            itemContent = { item ->
                var cant by remember { mutableStateOf("1") }
                Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Category, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.nombre, fontWeight = FontWeight.Bold)
                                Text("S/. ${item.precio}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            OutlinedTextField(value = cant, onValueChange = { if (it.all { c -> c.isDigit() }) cant = it }, label = { Text(strings.units) }, modifier = Modifier.width(80.dp).testTag("item_quantity_input"), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            Spacer(Modifier.width(12.dp))
                            Button(
                                onClick = { viewModel.agregarItem(item, cant.toIntOrNull() ?: 1) }, 
                                modifier = Modifier.weight(1f).testTag("add_item_confirm_button")
                            ) {
                                Text(strings.add) 
                            }
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
fun SelectionTile(label: String, value: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
            }
            Spacer(modifier = Modifier.weight(1f)); Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun DatePickerField(label: String, date: Date?, modifier: Modifier, onDateSelected: (Date) -> Unit) {
    val context = LocalContext.current
    OutlinedTextField(value = date?.formatTo("dd/MM/yyyy") ?: "", onValueChange = {}, readOnly = true, label = { Text(label) }, modifier = modifier.clickable {
        val cal = Calendar.getInstance(); date?.let { cal.time = it }
        DatePickerDialog(context, { _, y, m, d -> cal.set(y, m, d); onDateSelected(cal.time) }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }, enabled = false, shape = MaterialTheme.shapes.large, colors = TextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant))
}

@Composable
fun <T> GenericSelectionDialog(title: String, items: List<T>, onDismiss: () -> Unit, header: @Composable (() -> Unit)? = null, itemContent: @Composable (T) -> Unit) {
    val strings = com.raymi.app.core.lang.LocalRaymiStrings.current
    AlertDialog(
        onDismissRequest = onDismiss, 
        title = { Text(title, fontWeight = FontWeight.Bold) }, 
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { header?.invoke(); if (items.isEmpty()) Text(strings.searchNoResults, style = MaterialTheme.typography.bodySmall) else LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(items) { itemContent(it) } } } }, 
        confirmButton = { 
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("close_selection_button")) { 
                Text(strings.close) 
            } 
        }
    )
}

@Preview(showBackground = true, name = "Evidencia T4 - Crear Alquiler (Phone)")
@Composable
fun PreviewCreateAlquilerPhone() {
    MaterialTheme {
        Surface {
            // Versión simplificada para la captura de pantalla del informe
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Crear Alquiler", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                SelectionTile("Cliente", "Juan Pérez", Icons.Default.Person, true, {})
                Spacer(Modifier.height(16.dp))
                Text("Periodo de Alquiler", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = "05/06/2026", onValueChange = {}, label = { Text("Inicio") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = "08/06/2026", onValueChange = {}, label = { Text("Fin") }, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("Confirmar Alquiler")
                }
            }
        }
    }
}

@Preview(device = "spec:width=1280dp,height=800dp,dpi=240", showBackground = true, name = "Evidencia Hallazgo - Tablet Layout")
@Composable
fun PreviewCreateAlquilerTablet() {
    MaterialTheme {
        Surface {
            // Esta captura servirá para tu informe (punto 3.2.1) 
            // demostrando que el diseño no se adapta y se estira demasiado.
            PreviewCreateAlquilerPhone()
        }
    }
}
