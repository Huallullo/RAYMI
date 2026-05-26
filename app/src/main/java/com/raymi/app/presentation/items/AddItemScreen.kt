package com.raymi.app.presentation.items

import androidx.compose.animation.*
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
import com.raymi.app.domain.model.Categoria
import androidx.compose.ui.platform.testTag

/**
 * Pantalla de Registro de Ítem Personalizable (SaaS).
 * El usuario puede clasificar sus productos y añadir campos según el tipo de negocio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    viewModel: AddItemViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAttrDialog by remember { mutableStateOf(false) }
    var newAttrKey by remember { mutableStateOf("") }
    var isCatDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onNavigateBack()
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
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Clasificación
            Text("Categoría", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            ExposedDropdownMenuBox(
                expanded = isCatDropdownExpanded,
                onExpandedChange = { isCatDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.categoriaSeleccionada?.nombre ?: "Sin Categoría",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Selecciona una Categoría") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCatDropdownExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                        .testTag("item_categoria_spinner"),
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

            // 2. Información General
            Text("Información Básica", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = uiState.nombre,
                onValueChange = viewModel::onNombreChange,
                label = { Text("Nombre del Ítem") },
                placeholder = { Text("Ej: Vestido de Gala, Drone 4K, Camioneta") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("item_nombre_input"),
                shape = MaterialTheme.shapes.large
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = uiState.codigo,
                    onValueChange = viewModel::onCodigoChange,
                    label = { Text("Código/SKU") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("item_codigo_input"),
                    shape = MaterialTheme.shapes.large
                )
                OutlinedTextField(
                    value = uiState.precio,
                    onValueChange = viewModel::onPrecioChange,
                    label = { Text("Precio Alquiler") },
                    prefix = { Text("S/. ") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("item_precio_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = MaterialTheme.shapes.large
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 3. Personalización (Campos Dinámicos)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Especificaciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { showAttrDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Añadir Campo")
                }
            }

            if (uiState.atributos.isEmpty()) {
                Text(
                    "Personaliza tu ítem con campos específicos para tu negocio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            uiState.atributos.forEach { (key, value) ->
                val isNumberField = key.endsWith("(N)")
                val displayKey = key.removeSuffix("(N)")

                AttributeEditField(
                    label = displayKey,
                    value = value,
                    onValueChange = { newValue ->
                        if (!isNumberField || newValue.isEmpty() || newValue.all { it.isDigit() || it == '.' }) {
                            viewModel.onAtributoChange(key, newValue)
                        }
                    },
                    onDelete = { viewModel.eliminarAtributo(key) },
                    keyboardOptions = if (isNumberField) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 4. Botón Final
            Button(
                onClick = { viewModel.guardarItem() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("item_guardar_button"),
                shape = MaterialTheme.shapes.extraLarge,
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Guardar en Inventario", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Diálogo para nuevo campo personalizado
    if (showAttrDialog) {
        var isNumberType by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAttrDialog = false },
            title = { Text("Nuevo Campo Personalizado") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newAttrKey,
                        onValueChange = { newAttrKey = it },
                        label = { Text("Nombre (Ej: Talla, Color, Marca, Placa)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp).clickable { isNumberType = !isNumberType }
                    ) {
                        Checkbox(checked = isNumberType, onCheckedChange = { isNumberType = it })
                        Text("Este campo es numérico", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newAttrKey.isNotBlank()) {
                        val finalKey = if (isNumberType) "$newAttrKey(N)" else newAttrKey
                        viewModel.onAtributoChange(finalKey, "")
                        newAttrKey = ""
                        showAttrDialog = false
                    }
                }) { Text("Añadir") }
            },
            dismissButton = { TextButton(onClick = { showAttrDialog = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
fun AttributeEditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDelete: () -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label.replaceFirstChar { it.uppercase() }) },
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = keyboardOptions,
            trailingIcon = {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}