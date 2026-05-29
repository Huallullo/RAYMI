package com.raymi.app.presentation.items

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
import com.raymi.app.presentation.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemScreen(
    @Suppress("UNUSED_PARAMETER") itemId: String,
    viewModel: EditItemViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var showAttrDialog by remember { mutableStateOf(false) }
    var newAttrKey by remember { mutableStateOf("") }
    var isCatDropdownExpanded by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.onImageSelected(uri)
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onNavigateBack()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Editar Producto", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.isLoading) {
                RaymiLoadingIndicator()
            } else if (uiState.error != null) {
                RaymiErrorState(message = uiState.error!!, onRetry = { })
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Surface(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (uiState.newImageUri != null) {
                                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("Nueva imagen lista", modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp))
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(48.dp))
                                    Text("Cambiar Foto", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }

                    Text("Categoría", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    ExposedDropdownMenuBox(expanded = isCatDropdownExpanded, onExpandedChange = { isCatDropdownExpanded = it }) {
                        OutlinedTextField(
                            value = uiState.categoriaSeleccionada?.nombre ?: "Sin Categoría",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Mover a Categoría") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCatDropdownExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            leadingIcon = { Icon(Icons.Default.Folder, null) }
                        )
                        ExposedDropdownMenu(expanded = isCatDropdownExpanded, onDismissRequest = { isCatDropdownExpanded = false }) {
                            uiState.categorias.forEach { categoria ->
                                DropdownMenuItem(text = { Text(categoria.nombre) }, onClick = { viewModel.onCategoriaChange(categoria); isCatDropdownExpanded = false })
                            }
                        }
                    }

                    Text("Datos Principales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = uiState.nombre, onValueChange = viewModel::onNombreChange, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = uiState.codigo,
                            onValueChange = { viewModel.onCodigoChange(it) },
                            label = { Text("Código/SKU") },
                            trailingIcon = {
                                IconButton(onClick = { showBarcodeScanner = true }) {
                                    Icon(Icons.Default.QrCodeScanner, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            modifier = Modifier.weight(1.5f),
                            shape = MaterialTheme.shapes.large
                        )
                        OutlinedTextField(value = uiState.precio, onValueChange = viewModel::onPrecioChange, label = { Text("Precio") }, prefix = { Text("S/. ") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = MaterialTheme.shapes.large)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Especificaciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { showAttrDialog = true }) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Nuevo Campo")
                        }
                    }

                    uiState.atributos.forEach { (key, value) ->
                        AttributeEditField(label = key, value = value, onValueChange = { viewModel.onAtributoChange(key, it) }, onDelete = { viewModel.eliminarAtributo(key) })
                    }

                    Button(onClick = { viewModel.actualizarItem() }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = MaterialTheme.shapes.extraLarge, enabled = !uiState.isSaving) {
                        if (uiState.isSaving) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        else Text("Guardar Cambios", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showAttrDialog) {
        AlertDialog(onDismissRequest = { showAttrDialog = false }, title = { Text("Nuevo Campo") }, text = { OutlinedTextField(value = newAttrKey, onValueChange = { newAttrKey = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth()) }, confirmButton = { Button(onClick = { if (newAttrKey.isNotBlank()) { viewModel.onAtributoChange(newAttrKey, ""); newAttrKey = ""; showAttrDialog = false } }) { Text("Añadir") } }, dismissButton = { TextButton(onClick = { showAttrDialog = false }) { Text("Cancelar") } })
    }

    if (showBarcodeScanner) {
        BarcodeScannerDialog(onDismiss = { showBarcodeScanner = false }, onScan = { code -> viewModel.onCodigoChange(code); showBarcodeScanner = false })
    }
}
