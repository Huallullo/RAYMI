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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raymi.app.presentation.components.RaymiErrorState
import com.raymi.app.presentation.components.RaymiLoadingIndicator

/**
 * Pantalla de Edición de Producto.
 * Permite al usuario modificar cualquier aspecto del ítem, incluyendo sus campos personalizados.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemScreen(
    itemId: String,
    viewModel: EditItemViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var showAttrDialog by remember { mutableStateOf(false) }
    var newAttrKey by remember { mutableStateOf("") }
    var isCatDropdownExpanded by remember { mutableStateOf(false) }

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
                RaymiLoadingIndicator(message = "Recuperando información...")
            } else if (uiState.error != null) {
                RaymiErrorState(message = uiState.error!!, onRetry = { /* Reintentar cargar */ })
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // 1. Categoría
                    Text("Categoría", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    ExposedDropdownMenuBox(
                        expanded = isCatDropdownExpanded,
                        onExpandedChange = { isCatDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = uiState.categoriaSeleccionada?.nombre ?: "Sin Categoría",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Mover a Categoría") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCatDropdownExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) }
                        )
                        
                        ExposedDropdownMenu(
                            expanded = isCatDropdownExpanded,
                            onDismissRequest = { isCatDropdownExpanded = false }
                        ) {
                            uiState.categorias.forEach { categoria ->
                                DropdownMenuItem(
                                    text = { Text(categoria.nombre) },
                                    onClick = {
                                        viewModel.onCategoriaChange(categoria)
                                        isCatDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // 2. Datos Principales
                    Text("Datos Principales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    OutlinedTextField(
                        value = uiState.nombre,
                        onValueChange = viewModel::onNombreChange,
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = uiState.codigo,
                            onValueChange = viewModel::onCodigoChange,
                            label = { Text("Código") },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.large
                        )
                        OutlinedTextField(
                            value = uiState.precio,
                            onValueChange = viewModel::onPrecioChange,
                            label = { Text("Precio") },
                            prefix = { Text("S/. ") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = MaterialTheme.shapes.large
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // 3. Características Personalizadas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Especificaciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { showAttrDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Nuevo Campo")
                        }
                    }

                    uiState.atributos.forEach { (key, value) ->
                        AttributeEditField(
                            label = key,
                            value = value,
                            onValueChange = { viewModel.onAtributoChange(key, it) },
                            onDelete = { viewModel.eliminarAtributo(key) }
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // 4. Botón Actualizar
                    Button(
                        onClick = { viewModel.actualizarItem() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text("Guardar Cambios", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Diálogo para nuevo campo
    if (showAttrDialog) {
        AlertDialog(
            onDismissRequest = { showAttrDialog = false },
            title = { Text("Nuevo Campo") },
            text = {
                OutlinedTextField(
                    value = newAttrKey,
                    onValueChange = { newAttrKey = it },
                    label = { Text("Ej: Talla, Marca, Kilometraje") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newAttrKey.isNotBlank()) {
                        viewModel.onAtributoChange(newAttrKey, "")
                        newAttrKey = ""
                        showAttrDialog = false
                    }
                }) { Text("Añadir") }
            },
            dismissButton = { TextButton(onClick = { showAttrDialog = false }) { Text("Cancelar") } }
        )
    }
}
