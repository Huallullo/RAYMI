package com.raymi.app.presentation.comprobantes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raymi.app.domain.model.TipoComprobante
import com.raymi.app.presentation.components.RaymiLoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateComprobanteScreen(
    viewModel: GenerateComprobanteViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Generar Comprobante", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isSuccess && uiState.generatedPdfUri != null) {
            SuccessView(
                onShare = { viewModel.compartirPdf() },
                onBack = onNavigateBack
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1. Selección de Tipo
                Text("Tipo de Comprobante", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ComprobanteTypeChip(
                        label = "Ticket",
                        selected = uiState.tipo == TipoComprobante.TICKET,
                        onClick = { viewModel.onTipoChange(TipoComprobante.TICKET) },
                        modifier = Modifier.weight(1f)
                    )
                    ComprobanteTypeChip(
                        label = "Boleta",
                        selected = uiState.tipo == TipoComprobante.BOLETA,
                        onClick = { viewModel.onTipoChange(TipoComprobante.BOLETA) },
                        modifier = Modifier.weight(1f)
                    )
                    ComprobanteTypeChip(
                        label = "Factura",
                        selected = uiState.tipo == TipoComprobante.FACTURA,
                        onClick = { viewModel.onTipoChange(TipoComprobante.FACTURA) },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 2. Datos del Cliente
                Text("Datos del Receptor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                OutlinedTextField(
                    value = uiState.clienteDocumento,
                    onValueChange = viewModel::onDocumentoChange,
                    label = { Text(if (uiState.tipo == TipoComprobante.FACTURA) "RUC" else "DNI / Documento") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (uiState.isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = { viewModel.buscarDocumento() }) {
                                Icon(Icons.Default.Search, contentDescription = "Buscar")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = MaterialTheme.shapes.large
                )

                if (uiState.tipo == TipoComprobante.FACTURA) {
                    OutlinedTextField(
                        value = uiState.razonSocial,
                        onValueChange = viewModel::onRazonSocialChange,
                        label = { Text("Razón Social") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    )
                    OutlinedTextField(
                        value = uiState.direccionFiscal,
                        onValueChange = viewModel::onDireccionChange,
                        label = { Text("Dirección Fiscal") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    )
                } else {
                    OutlinedTextField(
                        value = uiState.clienteNombre,
                        onValueChange = viewModel::onNombreChange,
                        label = { Text("Nombre del Cliente") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { viewModel.generarComprobante() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    enabled = !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Description, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text("Generar y Guardar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ComprobanteTypeChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) },
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = Color.White
        )
    )
}

@Composable
fun SuccessView(onShare: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(100.dp))
        Spacer(Modifier.height(24.dp))
        Text("¡Comprobante Generado!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text("El documento se ha guardado correctamente.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(Modifier.height(48.dp))
        
        Button(
            onClick = onShare,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text("Compartir Comprobante", fontWeight = FontWeight.Bold)
        }
        
        TextButton(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
            Text("Volver al Alquiler")
        }
    }
}
