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
import com.google.accompanist.permissions.*

import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.provider.Settings

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun BusinessSettingsScreen(
    viewModel: BusinessSettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val strings = LocalRaymiStrings.current
    val context = LocalContext.current

    val logoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.subirLogo(it) }
    }

    if (uiState.isSuccess) {
        LaunchedEffect(Unit) { 
            onNavigateBack() 
            viewModel.clearMessages()
        }
    }

    // DIÁLOGO PARA GPS DESACTIVADO
    if (uiState.showGpsDisabledAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissGpsAlert() },
            title = { Text(strings.gpsDisabled, fontWeight = FontWeight.Black) },
            text = { 
                Text(strings.gpsDisabledDesc) 
            },
            confirmButton = {
                Button(onClick = {
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    context.startActivity(intent)
                    viewModel.dismissGpsAlert()
                }) {
                    Text(strings.openSettings)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissGpsAlert() }) {
                    Text(strings.close)
                }
            },
            icon = { Icon(Icons.Default.LocationOff, null, tint = MaterialTheme.colorScheme.error) },
            shape = MaterialTheme.shapes.extraLarge
        )
    }

    // DIÁLOGO PARA SIN INTERNET
    if (uiState.showNoInternetAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissNoInternetAlert() },
            title = { Text(strings.noConnection, fontWeight = FontWeight.Black) },
            text = { 
                Text(strings.noConnectionDesc) 
            },
            confirmButton = {
                Button(onClick = { viewModel.dismissNoInternetAlert() }) {
                    Text(strings.understood)
                }
            },
            icon = { Icon(Icons.Default.WifiOff, null, tint = MaterialTheme.colorScheme.error) },
            shape = MaterialTheme.shapes.extraLarge
        )
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
            // 1. Imagen de Marca
            SettingsSection(title = strings.brandIdentity) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .clickable { logoLauncher.launch("image/*") }
                    ) {
                        if (uiState.logoUrl != null) {
                            AsyncImage(
                                model = uiState.logoUrl,
                                contentDescription = strings.appName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.padding(25.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(strings.businessLogo, style = MaterialTheme.typography.labelMedium)
                }
            }

            // 2. Información del Negocio
            SettingsSection(title = if (strings is com.raymi.app.core.lang.SpanishStrings) "Información del Negocio" else "Business Information") {
                OutlinedTextField(
                    value = uiState.nombre,
                    onValueChange = viewModel::onNombreChange,
                    label = { Text(strings.businessNameInternal) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )
                
                OutlinedTextField(
                    value = uiState.nombreComercial,
                    onValueChange = viewModel::onNombreComercialChange,
                    label = { Text(strings.tradeName) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) }
                )

                OutlinedTextField(
                    value = uiState.ruc,
                    onValueChange = viewModel::onRucChange,
                    label = { Text(strings.taxId) },
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
                    value = uiState.descripcion,
                    onValueChange = viewModel::onDescripcionChange,
                    label = { Text(strings.businessDescription) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    maxLines = 5
                )

                OutlinedTextField(
                    value = uiState.googleMapsUrl,
                    onValueChange = viewModel::onGoogleMapsUrlChange,
                    label = { Text(strings.googleMapsLink) },
                    placeholder = { Text("https://maps.google.com/...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    leadingIcon = { Icon(Icons.Default.Map, contentDescription = null) },
                    trailingIcon = {
                        val locationPermissionState = rememberPermissionState(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                        IconButton(onClick = {
                            if (locationPermissionState.status.isGranted) {
                                viewModel.captureLocation()
                            } else {
                                locationPermissionState.launchPermissionRequest()
                            }
                        }) {
                            Icon(Icons.Default.MyLocation, contentDescription = "Capturar Ubicación")
                        }
                    },
                    singleLine = true
                )
            }

            // 3. Finanzas
            SettingsSection(title = strings.financeRegion) {
                var expanded by remember { mutableStateOf(false) }
                val currencies = listOf(
                    "PEN" to "🇵🇪 Sol Peruano",
                    "USD" to "🇺🇸 Dólar Estadounidense",
                    "MXN" to "🇲🇽 Peso Mexicano",
                    "COP" to "🇨🇴 Peso Colombiano",
                    "CLP" to "🇨🇱 Peso Chileno",
                    "ARS" to "🇦🇷 Peso Argentino",
                    "BRL" to "🇧🇷 Real Brasileño",
                    "BOB" to "🇧🇴 Boliviano",
                    "PYG" to "🇵🇾 Guaraní",
                    "UYU" to "🇺🇾 Peso Uruguayo"
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = currencies.find { it.first == uiState.moneda }?.second ?: uiState.moneda,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(strings.businessCurrency) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = MaterialTheme.shapes.large,
                        leadingIcon = { Icon(Icons.Default.Paid, null) }
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        currencies.forEach { (code, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    viewModel.onMonedaChange(code)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // 4. Legal y Políticas
            SettingsSection(title = strings.legalPolicies) {
                val terminosDefault = if (strings is com.raymi.app.core.lang.SpanishStrings) 
                    "Al utilizar este servicio, el cliente acepta entregar el equipo en las mismas condiciones. El retraso genera penalidades según la política vigente." 
                    else "By using this service, the client agrees to return the equipment in the same conditions. Delays generate penalties according to current policy."

                val politicaDefault = if (strings is com.raymi.app.core.lang.SpanishStrings)
                    "Retraso por día: 20% del valor del alquiler. Daños menores: Costo de reparación. Daño total/Extravío: Valor comercial del equipo."
                    else "Delay per day: 20% of the rental value. Minor damage: Repair cost. Total loss: Commercial value of the equipment."

                 OutlinedTextField(
                    value = uiState.terminosCondiciones,
                    onValueChange = viewModel::onTerminosChange, // ✅ Editable
                    label = { Text(strings.termsConditions) },
                    placeholder = { Text(terminosDefault) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    maxLines = 5
                )
                OutlinedTextField(
                    value = uiState.politicaPenalidades,
                    onValueChange = viewModel::onPoliticaChange, // ✅ Editable
                    label = { Text(strings.penaltyPolicy) },
                    placeholder = { Text(politicaDefault) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    maxLines = 5
                )
                Text(
                    text = strings.internalPolicyMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

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
