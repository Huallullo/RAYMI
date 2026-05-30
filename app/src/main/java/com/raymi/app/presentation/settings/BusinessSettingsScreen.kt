package com.raymi.app.presentation.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.raymi.app.core.lang.LocalRaymiStrings
import com.raymi.app.presentation.components.RaymiPhoneField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessSettingsScreen(
    viewModel: BusinessSettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val strings = LocalRaymiStrings.current

    val logoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.subirLogo(it) }
    }

    val sloganLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.subirSloganImagen(it) }
    }

    if (uiState.isSuccess) {
        LaunchedEffect(Unit) { 
            onNavigateBack() 
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(strings.myBusiness, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
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
            // 1. Identidad Visual
            SettingsSection(title = if (strings is com.raymi.app.core.lang.SpanishStrings) "Imagen de Marca" else "Brand Identity") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .clickable { logoLauncher.launch("image/*") }
                        ) {
                            if (uiState.logoUrl != null) {
                                AsyncImage(
                                    model = uiState.logoUrl,
                                    contentDescription = "Logo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.padding(20.dp))
                                }
                            }
                        }
                        Text("Logo Principal", style = MaterialTheme.typography.labelSmall)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { sloganLauncher.launch("image/*") }
                        ) {
                            if (uiState.sloganImageUrl != null) {
                                AsyncImage(
                                    model = uiState.sloganImageUrl,
                                    contentDescription = "Banner",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.Image, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Subir Banner/Slogan", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                        Text("Imagen de Eslogan / Banner", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // 2. Identidad y Datos Fiscales
            SettingsSection(title = if (strings is com.raymi.app.core.lang.SpanishStrings) "Información del Negocio" else "Business Information") {
                OutlinedTextField(
                    value = uiState.nombre,
                    onValueChange = viewModel::onNombreChange,
                    label = { Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "Nombre del Negocio (Interno)" else "Business Name (Internal)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )
                
                OutlinedTextField(
                    value = uiState.nombreComercial,
                    onValueChange = viewModel::onNombreComercialChange,
                    label = { Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "Nombre Comercial (Para Comprobantes)" else "Trade Name (For Receipts)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) }
                )

                OutlinedTextField(
                    value = uiState.ruc,
                    onValueChange = viewModel::onRucChange,
                    label = { Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "RUC del Negocio" else "Tax ID / RUC") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) }
                )

                OutlinedTextField(
                    value = uiState.direccion,
                    onValueChange = viewModel::onDireccionChange,
                    label = { Text(strings.address) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
                )

                RaymiPhoneField(
                    phone = uiState.telefono,
                    onPhoneChange = viewModel::onTelefonoChange,
                    label = strings.phone
                )

                OutlinedTextField(
                    value = uiState.slogan,
                    onValueChange = viewModel::onSloganChange,
                    label = { Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "Eslogan Publicitario" else "Marketing Slogan") },
                    placeholder = { Text("Ej: Tu mejor opción en vestuarios") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = uiState.descripcion,
                    onValueChange = viewModel::onDescripcionChange,
                    label = { Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "Descripción Detallada" else "Detailed Description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    minLines = 3
                )
            }

            // 3. Comprobantes
            SettingsSection(title = if (strings is com.raymi.app.core.lang.SpanishStrings) "Configuración de Comprobantes" else "Receipt Configuration") {
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

            // 4. Regionalización y Finanzas
            SettingsSection(title = if (strings is com.raymi.app.core.lang.SpanishStrings) "Finanzas y Región" else "Finance & Region") {
                OutlinedTextField(
                    value = uiState.moneda,
                    onValueChange = viewModel::onMonedaChange,
                    label = { Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "Moneda (Símbolo o Código)" else "Currency (Symbol or Code)") },
                    placeholder = { Text("PEN, USD, S/.") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    leadingIcon = { Icon(Icons.Default.Paid, contentDescription = null) }
                )
            }

            // 5. Términos y Condiciones
            SettingsSection(title = if (strings is com.raymi.app.core.lang.SpanishStrings) "Legal y Políticas" else "Legal & Policies") {
                 OutlinedTextField(
                    value = uiState.terminosCondiciones,
                    onValueChange = viewModel::onTerminosChange,
                    label = { Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "Términos y Condiciones" else "Terms and Conditions") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    minLines = 3
                )
                OutlinedTextField(
                    value = uiState.politicaPenalidades,
                    onValueChange = viewModel::onPoliticaChange,
                    label = { Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "Política de Penalidades" else "Penalty Policy") },
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
                    Text(strings.save, fontWeight = FontWeight.Bold)
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
