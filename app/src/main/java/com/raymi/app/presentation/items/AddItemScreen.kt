package com.raymi.app.presentation.items

import androidx.compose.foundation.clickable
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.testTag
import com.raymi.app.presentation.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    viewModel: AddItemViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToPlans: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAttrDialog by remember { mutableStateOf(false) }
    var newAttrKey by remember { mutableStateOf("") }
    var isCatDropdownExpanded by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        viewModel.onImageSelected(uri)
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onNavigateBack()
    }

    LaunchedEffect(uiState.shouldNavigateToPlans) {
        if (uiState.shouldNavigateToPlans) onNavigateToPlans()
    }

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
                title = { Text("Nuevo Producto", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(scrollState).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Surface(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth().height(180.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                if (uiState.selectedImageUri != null) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Imagen lista", modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp))
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Añadir Foto del Producto", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Text("Categoría", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            ExposedDropdownMenuBox(expanded = isCatDropdownExpanded, onExpandedChange = { isCatDropdownExpanded = it }) {
                OutlinedTextField(
                    value = uiState.categoriaSeleccionada?.nombre ?: "Sin Categoría",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Selecciona una Categoría") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCatDropdownExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth().testTag("item_categoria_spinner"),
                    shape = MaterialTheme.shapes.large,
                    leadingIcon = { Icon(Icons.Default.Folder, null) }
                )
                ExposedDropdownMenu(expanded = isCatDropdownExpanded, onDismissRequest = { isCatDropdownExpanded = false }) {
                    uiState.categorias.forEach { categoria ->
                        DropdownMenuItem(text = { Text(categoria.nombre) }, onClick = { viewModel.onCategoriaChange(categoria); isCatDropdownExpanded = false })
                    }
                }
            }

            Text("Información Básica", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = uiState.nombre, onValueChange = viewModel::onNombreChange, label = { Text("Nombre del Ítem") }, modifier = Modifier.fillMaxWidth().testTag("item_nombre_input"), shape = MaterialTheme.shapes.large)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = uiState.codigo,
                    onValueChange = { viewModel.onCodigoChange(it) },
                    label = { Text("Código/SKU") },
                    trailingIcon = {
                        Row {
                            IconButton(onClick = { showBarcodeScanner = true }) { Icon(Icons.Default.QrCodeScanner, null, tint = MaterialTheme.colorScheme.primary) }
                            IconButton(onClick = { viewModel.onCodigoChange(com.raymi.app.core.utils.GeneradorCodigo.generarCodigoItem()) }) { Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary) }
                        }
                    },
                    modifier = Modifier.weight(1.5f).testTag("item_codigo_input"),
                    shape = MaterialTheme.shapes.large
                )
                OutlinedTextField(value = uiState.precio, onValueChange = viewModel::onPrecioChange, label = { Text("Precio") }, prefix = { Text("S/. ") }, modifier = Modifier.weight(1f).testTag("item_precio_input"), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = MaterialTheme.shapes.large)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Especificaciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { showAttrDialog = true }) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Añadir Campo")
                }
            }

            uiState.atributos.forEach { (key, value) ->
                AttributeEditField(label = key.removeSuffix("(N)"), value = value, onValueChange = { viewModel.onAtributoChange(key, it) }, onDelete = { viewModel.eliminarAtributo(key) }, keyboardOptions = if (key.endsWith("(N)")) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default)
            }

            Button(onClick = { viewModel.guardarItem() }, modifier = Modifier.fillMaxWidth().height(56.dp).testTag("item_guardar_button"), shape = MaterialTheme.shapes.extraLarge, enabled = !uiState.isLoading) {
                if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                else Text("Guardar en Inventario", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showAttrDialog) {
        var isNumberType by remember { mutableStateOf(false) }
        AlertDialog(onDismissRequest = { showAttrDialog = false }, title = { Text("Nuevo Campo") }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { OutlinedTextField(value = newAttrKey, onValueChange = { newAttrKey = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth()); Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = isNumberType, onCheckedChange = { isNumberType = it }); Text("Es numérico") } } }, confirmButton = { Button(onClick = { if (newAttrKey.isNotBlank()) { viewModel.onAtributoChange(if (isNumberType) "$newAttrKey(N)" else newAttrKey, ""); newAttrKey = ""; showAttrDialog = false } }) { Text("Añadir") } }, dismissButton = { TextButton(onClick = { showAttrDialog = false }) { Text("Cancelar") } })
    }

    if (showBarcodeScanner) {
        BarcodeScannerDialog(onDismiss = { showBarcodeScanner = false }, onScan = { code -> viewModel.onCodigoChange(code); showBarcodeScanner = false })
    }
}

@Composable
fun AttributeEditField(label: String, value: String, onValueChange: (String) -> Unit, onDelete: () -> Unit, keyboardOptions: KeyboardOptions = KeyboardOptions.Default) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label.replaceFirstChar { it.uppercase() }) }, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.medium, keyboardOptions = keyboardOptions, trailingIcon = { IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) } })
    }
}
