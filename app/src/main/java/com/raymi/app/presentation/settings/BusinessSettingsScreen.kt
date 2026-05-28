package com.raymi.app.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessSettingsScreen(
    viewModel: BusinessSettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    if (uiState.isSuccess) {
        LaunchedEffect(Unit) { onNavigateBack() }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Ajustes del Negocio", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
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
            // 1. Identidad del Negocio
            SettingsSection(title = "Identidad y Datos Fiscales") {
                OutlinedTextField(
                    value = uiState.nombre,
                    onValueChange = viewModel::onNombreChange,
                    label = { Text("Nombre del Negocio (Interno)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )
                
                OutlinedTextField(
                    value = uiState.nombreComercial,
                    onValueChange = viewModel::onNombreComercialChange,
                    label = { Text("Nombre Comercial (Para Comprobantes)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) }
                )

                OutlinedTextField(
                    value = uiState.ruc,
                    onValueChange = viewModel::onRucChange,
                    label = { Text("RUC del Negocio") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) }
                )

                OutlinedTextField(
                    value = uiState.direccion,
                    onValueChange = viewModel::onDireccionChange,
                    label = { Text("Dirección Fiscal") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
                )

                OutlinedTextField(
                    value = uiState.telefono,
                    onValueChange = viewModel::onTelefonoChange,
                    label = { Text("Teléfono de Contacto") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) }
                )
                
                OutlinedTextField(
                    value = uiState.descripcion,
                    onValueChange = viewModel::onDescripcionChange,
                    label = { Text("Descripción o Eslogan") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    minLines = 2
                )
            }

            // 2. Comprobantes
            SettingsSection(title = "Configuración de Comprobantes") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uiState.serieTicket,
                        onValueChange = viewModel::onSerieTicketChange,
                        label = { Text("Serie Ticket") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large
                    )
                    OutlinedTextField(
                        value = uiState.serieBoleta,
                        onValueChange = viewModel::onSerieBoletaChange,
                        label = { Text("Serie Boleta") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large
                    )
                    OutlinedTextField(
                        value = uiState.serieFactura,
                        onValueChange = viewModel::onSerieFacturaChange,
                        label = { Text("Serie Factura") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large
                    )
                }
            }

            // 3. Regionalización y Finanzas
            SettingsSection(title = "Finanzas y Región") {
                OutlinedTextField(
                    value = uiState.moneda,
                    onValueChange = viewModel::onMonedaChange,
                    label = { Text("Moneda (Símbolo o Código)") },
                    placeholder = { Text("PEN, USD, S/.") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    leadingIcon = { Icon(Icons.Default.Paid, contentDescription = null) }
                )
            }

            // 4. Términos y Condiciones
            SettingsSection(title = "Legal y Políticas") {
                 OutlinedTextField(
                    value = uiState.terminosCondiciones,
                    onValueChange = viewModel::onTerminosChange,
                    label = { Text("Términos y Condiciones") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    minLines = 3
                )
                OutlinedTextField(
                    value = uiState.politicaPenalidades,
                    onValueChange = viewModel::onPoliticaChange,
                    label = { Text("Política de Penalidades") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    minLines = 3
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Botón de Guardado
            Button(
                onClick = { viewModel.guardarCambios() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("Aplicar Cambios", fontWeight = FontWeight.Bold)
                }
            }

            if (uiState.error != null) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        content()
    }
}
