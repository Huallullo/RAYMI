package com.raymi.app.presentation.alquileres

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.raymi.app.core.theme.CustomShapes

/**
 * Pantalla para crear un nuevo alquiler
 * Permite seleccionar cliente, vestuario y configurar el alquiler
 *
 * NOTA: Esta es una versión simplificada para mostrar la UI.
 * La versión completa requeriría un ViewModel con lógica completa.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAlquilerScreen(
    onNavigateBack: () -> Unit,
    onAlquilerCreated: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { /* TODO: Mostrar diálogo de selección */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Buscar Cliente")
                    }

                    Text(
                        text = "Selecciona un cliente existente o crea uno nuevo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { /* TODO: Mostrar diálogo de selección */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Checkroom, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Buscar Vestuario")
                    }

                    Text(
                        text = "Solo se mostrarán vestuarios disponibles",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        label = { Text("Fecha de Inicio") },
                        leadingIcon = {
                            Icon(Icons.Default.CalendarToday, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true
                    )

                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        label = { Text("Fecha de Devolución Prevista") },
                        leadingIcon = {
                            Icon(Icons.Default.Event, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true
                    )
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
                        value = "",
                        onValueChange = {},
                        label = { Text("Precio Total") },
                        leadingIcon = {
                            Icon(Icons.Default.AttachMoney, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true
                    )

                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        label = { Text("Adelanto (opcional)") },
                        leadingIcon = {
                            Icon(Icons.Default.Payments, contentDescription = null)
                        },
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
                            text = "S/. 0.00",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Observaciones
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("Observaciones (opcional)") },
                leadingIcon = {
                    Icon(Icons.Default.Notes, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            // Botón de crear
            Button(
                onClick = {
                    // TODO: Validar y crear alquiler
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Crear Alquiler")
            }

            // Espaciado final
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}