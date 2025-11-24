package com.raymi.app.presentation.alquileres

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.raymi.app.core.theme.CustomShapes
import com.raymi.app.core.theme.RaymiColors
import com.raymi.app.core.utils.formatTo
import com.raymi.app.domain.model.Cliente
import com.raymi.app.domain.model.Vestuario
import com.raymi.app.presentation.clientes.ClienteItem
import com.raymi.app.presentation.components.RaymiSearchBar
import com.raymi.app.presentation.vestuarios.VestuarioCard
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAlquilerScreen(
    viewModel: CreateAlquilerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onAlquilerCreated: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Mostrar mensajes
    LaunchedEffect(uiState.error, uiState.successMessage) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessages()
        }
    }

    // Navegar cuando se crea exitosamente
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Alquiler") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Información
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Crear Alquiler",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Completa los datos para registrar un nuevo alquiler",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Sección: Cliente
            Text(
                text = "1. Seleccionar Cliente",
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
                    if (uiState.selectedCliente != null) {
                        // Cliente seleccionado
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.selectedCliente!!.nombreCompleto,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "DNI: ${uiState.selectedCliente!!.dni}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.showClienteDialog() }) {
                                Icon(Icons.Default.Edit, contentDescription = "Cambiar")
                            }
                        }
                    } else {
                        // Botón para seleccionar
                        Button(
                            onClick = { viewModel.showClienteDialog() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Seleccionar Cliente")
                        }
                    }
                }
            }

            // Sección: Vestuario
            Text(
                text = "2. Seleccionar Vestuario",
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
                    if (uiState.selectedVestuario != null) {
                        // Vestuario seleccionado
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.selectedVestuario!!.danza,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Código: ${uiState.selectedVestuario!!.codigo}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = uiState.selectedVestuario!!.precioFormateado,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = { viewModel.showVestuarioDialog() }) {
                                Icon(Icons.Default.Edit, contentDescription = "Cambiar")
                            }
                        }
                    } else {
                        // Botón para seleccionar
                        Button(
                            onClick = { viewModel.showVestuarioDialog() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Checkroom, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Seleccionar Vestuario")
                        }
                    }
                }
            }

            // Sección: Fechas
            Text(
                text = "3. Configurar Fechas",
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
                    // Fecha de inicio
                    OutlinedTextField(
                        value = uiState.fechaInicio?.formatTo("dd/MM/yyyy") ?: "",
                        onValueChange = {},
                        label = { Text("Fecha de Inicio") },
                        leadingIcon = {
                            Icon(Icons.Default.CalendarToday, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = {
                                val calendar = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        calendar.set(year, month, day)
                                        viewModel.setFechaInicio(calendar.time)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }) {
                                Icon(Icons.Default.EditCalendar, contentDescription = "Seleccionar")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true
                    )

                    // Fecha de fin
                    OutlinedTextField(
                        value = uiState.fechaFin?.formatTo("dd/MM/yyyy") ?: "",
                        onValueChange = {},
                        label = { Text("Fecha de Devolución Prevista") },
                        leadingIcon = {
                            Icon(Icons.Default.Event, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = {
                                val calendar = Calendar.getInstance()
                                if (uiState.fechaInicio != null) {
                                    calendar.time = uiState.fechaInicio!!
                                }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        calendar.set(year, month, day)
                                        viewModel.setFechaFin(calendar.time)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }) {
                                Icon(Icons.Default.EditCalendar, contentDescription = "Seleccionar")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true
                    )

                    // Mostrar días
                    if (uiState.diasAlquiler > 0) {
                        Text(
                            text = "Duración: ${uiState.diasAlquiler} día(s)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Sección: Pago
            Text(
                text = "4. Información de Pago",
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
                    OutlinedTextField(
                        value = uiState.precioTotal,
                        onValueChange = {},
                        label = { Text("Precio Total") },
                        leadingIcon = {
                            Icon(Icons.Default.AttachMoney, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        enabled = false
                    )

                    OutlinedTextField(
                        value = uiState.adelanto,
                        onValueChange = { viewModel.onAdelantoChange(it) },
                        label = { Text("Adelanto (opcional)") },
                        leadingIcon = {
                            Icon(Icons.Default.Payments, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Divider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Saldo:",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "S/. ${String.format("%.2f", uiState.saldo)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.saldo > 0) {
                                RaymiColors.Warning
                            } else {
                                RaymiColors.Success
                            }
                        )
                    }
                }
            }

            // Observaciones
            OutlinedTextField(
                value = uiState.observaciones,
                onValueChange = { viewModel.onObservacionesChange(it) },
                label = { Text("Observaciones (opcional)") },
                leadingIcon = {
                    Icon(Icons.Default.Notes, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            // Botón de crear
            Button(
                onClick = { viewModel.createAlquiler() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Crear Alquiler")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Diálogo de selección de cliente
    if (uiState.showClienteDialog) {
        SelectClienteDialog(
            clientes = uiState.clientes,
            searchQuery = uiState.clienteSearchQuery,
            onSearchQueryChange = { viewModel.searchClientes(it) },
            onClienteSelect = { viewModel.selectCliente(it) },
            onDismiss = { viewModel.hideClienteDialog() }
        )
    }

    // Diálogo de selección de vestuario
    if (uiState.showVestuarioDialog) {
        SelectVestuarioDialog(
            vestuarios = uiState.vestuariosDisponibles,
            searchQuery = uiState.vestuarioSearchQuery,
            onSearchQueryChange = { viewModel.searchVestuarios(it) },
            onVestuarioSelect = { viewModel.selectVestuario(it) },
            onDismiss = { viewModel.hideVestuarioDialog() }
        )
    }
}

@Composable
fun SelectClienteDialog(
    clientes: List<Cliente>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClienteSelect: (Cliente) -> Unit,
    onDismiss: () -> Unit
) {
    val filteredClientes = remember(clientes, searchQuery) {
        if (searchQuery.isBlank()) {
            clientes
        } else {
            clientes.filter {
                it.nombreCompleto.contains(searchQuery, ignoreCase = true) ||
                        it.dni.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Cliente") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                RaymiSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholder = "Buscar cliente...",
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn {
                    items(filteredClientes) { cliente ->
                        ClienteItem(
                            cliente = cliente,
                            onClick = { onClienteSelect(cliente) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun SelectVestuarioDialog(
    vestuarios: List<Vestuario>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onVestuarioSelect: (Vestuario) -> Unit,
    onDismiss: () -> Unit
) {
    val filteredVestuarios = remember(vestuarios, searchQuery) {
        if (searchQuery.isBlank()) {
            vestuarios
        } else {
            vestuarios.filter {
                it.codigo.contains(searchQuery, ignoreCase = true) ||
                        it.danza.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Vestuario Disponible") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                RaymiSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholder = "Buscar vestuario...",
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredVestuarios) { vestuario ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onVestuarioSelect(vestuario) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = vestuario.danza,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Código: ${vestuario.codigo}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = vestuario.precioFormateado,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}