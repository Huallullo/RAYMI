package com.raymi.app.presentation.comprobantes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.raymi.app.core.lang.LocalRaymiStrings
import com.raymi.app.presentation.components.RaymiLoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateComprobanteScreen(
    viewModel: GenerateComprobanteViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToPlans: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val strings = LocalRaymiStrings.current
    val isSpanish = strings is com.raymi.app.core.lang.SpanishStrings
    var showUpgradeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            if (it.contains("PLAN PRO")) {
                showUpgradeDialog = true
            } else {
                snackbarHostState.showSnackbar(it)
            }
            viewModel.clearMessages()
        }
    }

    if (showUpgradeDialog) {
        AlertDialog(
            onDismissRequest = { showUpgradeDialog = false },
            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(if (isSpanish) "Función Premium 🔒" else "Premium Feature 🔒") },
            text = { 
                Text(if (isSpanish) 
                    "La emisión de Boletas y Facturas electrónicas es parte del Plan PRO. \n\n✅ Boletas/Facturas ilimitadas\n✅ Sin anuncios\n✅ Reportes financieros avanzados" 
                    else "E-billing is part of the PRO Plan. \n\n✅ Unlimited Bills/Invoices\n✅ No ads\n✅ Advanced financial reports") 
            },
            confirmButton = {
                Button(onClick = { 
                    showUpgradeDialog = false
                    onNavigateToPlans() 
                }) { Text(strings.viewProPlans) }
            },
            dismissButton = {
                TextButton(onClick = { showUpgradeDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(strings.generateReceipt, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isSuccess && uiState.generatedPdfUri != null) {
            SuccessView(
                onShare = { viewModel.compartirPdf() },
                onBack = onNavigateBack,
                strings = strings
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
                Text(strings.receiptType, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ComprobanteTypeChip(
                        label = strings.ticket,
                        selected = uiState.tipo == TipoComprobante.TICKET,
                        onClick = { viewModel.onTipoChange(TipoComprobante.TICKET) },
                        modifier = Modifier.weight(1f)
                    )
                    ComprobanteTypeChip(
                        label = strings.bill,
                        selected = uiState.tipo == TipoComprobante.BOLETA,
                        onClick = { viewModel.onTipoChange(TipoComprobante.BOLETA) },
                        isPro = true,
                        hasPro = uiState.userPlan?.plan == com.raymi.app.domain.model.PlanType.PRO,
                        modifier = Modifier.weight(1f)
                    )
                    ComprobanteTypeChip(
                        label = strings.invoice,
                        selected = uiState.tipo == TipoComprobante.FACTURA,
                        onClick = { viewModel.onTipoChange(TipoComprobante.FACTURA) },
                        isPro = true,
                        hasPro = uiState.userPlan?.plan == com.raymi.app.domain.model.PlanType.PRO,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 2. Datos del Cliente
                Text(if (isSpanish) "Datos del Receptor" else "Receiver Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                OutlinedTextField(
                    value = uiState.clienteDocumento,
                    onValueChange = viewModel::onDocumentoChange,
                    label = { Text(if (uiState.tipo == TipoComprobante.FACTURA) "RUC" else strings.idDocument) },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (uiState.isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = { viewModel.buscarDocumento() }) {
                                Icon(Icons.Default.Search, contentDescription = strings.search)
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
                        label = { Text(if (isSpanish) "Razón Social" else "Business Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    )
                    OutlinedTextField(
                        value = uiState.direccionFiscal,
                        onValueChange = viewModel::onDireccionChange,
                        label = { Text(if (isSpanish) "Dirección Fiscal" else "Tax Address") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    )
                } else {
                    OutlinedTextField(
                        value = uiState.clienteNombre,
                        onValueChange = viewModel::onNombreChange,
                        label = { Text(strings.names) },
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
                        Text(if (isSpanish) "Generar y Guardar" else "Generate & Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ComprobanteTypeChip(
    label: String, 
    selected: Boolean, 
    onClick: () -> Unit, 
    modifier: Modifier,
    isPro: Boolean = false,
    hasPro: Boolean = false
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                if (isPro && !hasPro) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.Lock, contentDescription = "PRO", modifier = Modifier.size(12.dp))
                }
            }
        },
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = if (isPro && !hasPro) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
            selectedLabelColor = Color.White
        )
    )
}

@Composable
fun SuccessView(onShare: () -> Unit, onBack: () -> Unit, strings: com.raymi.app.core.lang.RaymiStrings) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFFE8F5E9),
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.PictureAsPdf, 
                    contentDescription = null, 
                    tint = Color(0xFF4CAF50), 
                    modifier = Modifier.size(60.dp)
                )
            }
        }
        
        Spacer(Modifier.height(32.dp))
        val isSpanish = strings is com.raymi.app.core.lang.SpanishStrings
        Text(if (isSpanish) "¡Comprobante Listo!" else "Receipt Ready!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text(if (isSpanish) "El PDF ha sido generado con éxito." else "PDF generated successfully.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(Modifier.height(48.dp))
        
        Button(
            onClick = onShare,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(strings.shareReceipt, fontWeight = FontWeight.Bold)
        }
        
        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Text(if (isSpanish) "Volver al Alquiler" else "Back to Rental")
        }
    }
}
