package com.raymi.app.presentation.vestuarios

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.raymi.app.core.theme.CustomShapes
import com.raymi.app.core.utils.Constants
import com.raymi.app.core.utils.Validators
import com.raymi.app.domain.model.EstadoVestuario
import com.raymi.app.domain.model.Vestuario

/**
 * Diálogo para editar un vestuario existente
 * Incluye validación de campos
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditVestuarioDialog(
    vestuario: Vestuario,
    onDismiss: () -> Unit,
    onConfirm: (Vestuario) -> Unit,
    isLoading: Boolean = false
) {
    var codigo by remember { mutableStateOf(vestuario.codigo) }
    var danza by remember { mutableStateOf(vestuario.danza) }
    var departamento by remember { mutableStateOf(vestuario.departamento) }
    var descripcion by remember { mutableStateOf(vestuario.descripcion) }
    var talla by remember { mutableStateOf(vestuario.talla) }
    var precio by remember { mutableStateOf(vestuario.precio.toString()) }
    var estado by remember { mutableStateOf(vestuario.estado) }

    var expandedDpto by remember { mutableStateOf(false) }
    var expandedTalla by remember { mutableStateOf(false) }
    var expandedEstado by remember { mutableStateOf(false) }

    var codigoError by remember { mutableStateOf<String?>(null) }
    var danzaError by remember { mutableStateOf<String?>(null) }
    var precioError by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    /**
     * Valida todos los campos
     */
    fun validateFields(): Boolean {
        var isValid = true

        // Validar código
        val codigoValidation = Validators.validateCodigo(codigo)
        if (!codigoValidation.isValid) {
            codigoError = codigoValidation.errorMessage
            isValid = false
        } else {
            codigoError = null
        }

        // Validar danza
        val danzaValidation = Validators.validateDanza(danza)
        if (!danzaValidation.isValid) {
            danzaError = danzaValidation.errorMessage
            isValid = false
        } else {
            danzaError = null
        }

        // Validar precio
        val precioValidation = Validators.validatePrecioText(precio)
        if (!precioValidation.isValid) {
            precioError = precioValidation.errorMessage
            isValid = false
        } else {
            precioError = null
        }

        // Validar campos requeridos
        if (departamento.isBlank()) {
            isValid = false
        }

        if (talla.isBlank()) {
            isValid = false
        }

        return isValid
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Text("Editar Vestuario")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Código (solo lectura)
                OutlinedTextField(
                    value = codigo,
                    onValueChange = {},
                    label = { Text("Código") },
                    leadingIcon = {
                        Icon(Icons.Default.Tag, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Text(
                    text = "El código no se puede modificar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Danza
                OutlinedTextField(
                    value = danza,
                    onValueChange = {
                        danza = it
                        danzaError = null
                    },
                    label = { Text("Nombre de la Danza *") },
                    leadingIcon = {
                        Icon(Icons.Default.MusicNote, contentDescription = null)
                    },
                    isError = danzaError != null,
                    supportingText = {
                        danzaError?.let { Text(it) }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                // Departamento (Dropdown)
                ExposedDropdownMenuBox(
                    expanded = expandedDpto,
                    onExpandedChange = { expandedDpto = !expandedDpto && !isLoading }
                ) {
                    OutlinedTextField(
                        value = departamento,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Departamento *") },
                        leadingIcon = {
                            Icon(Icons.Default.Place, contentDescription = null)
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDpto)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !isLoading
                    )

                    ExposedDropdownMenu(
                        expanded = expandedDpto,
                        onDismissRequest = { expandedDpto = false }
                    ) {
                        Constants.DEPARTAMENTOS_PERU.forEach { depto ->
                            DropdownMenuItem(
                                text = { Text(depto) },
                                onClick = {
                                    departamento = depto
                                    expandedDpto = false
                                }
                            )
                        }
                    }
                }

                // Talla (Dropdown)
                ExposedDropdownMenuBox(
                    expanded = expandedTalla,
                    onExpandedChange = { expandedTalla = !expandedTalla && !isLoading }
                ) {
                    OutlinedTextField(
                        value = talla,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Talla *") },
                        leadingIcon = {
                            Icon(Icons.Default.Straighten, contentDescription = null)
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTalla)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !isLoading
                    )

                    ExposedDropdownMenu(
                        expanded = expandedTalla,
                        onDismissRequest = { expandedTalla = false }
                    ) {
                        Constants.TALLAS.forEach { size ->
                            DropdownMenuItem(
                                text = { Text(size) },
                                onClick = {
                                    talla = size
                                    expandedTalla = false
                                }
                            )
                        }
                    }
                }

                // Estado (Dropdown)
                ExposedDropdownMenuBox(
                    expanded = expandedEstado,
                    onExpandedChange = { expandedEstado = !expandedEstado && !isLoading }
                ) {
                    OutlinedTextField(
                        value = estado.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Estado *") },
                        leadingIcon = {
                            Icon(Icons.Default.Info, contentDescription = null)
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedEstado)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !isLoading
                    )

                    ExposedDropdownMenu(
                        expanded = expandedEstado,
                        onDismissRequest = { expandedEstado = false }
                    ) {
                        EstadoVestuario.values().forEach { estadoVest ->
                            DropdownMenuItem(
                                text = { Text(estadoVest.name) },
                                onClick = {
                                    estado = estadoVest
                                    expandedEstado = false
                                }
                            )
                        }
                    }
                }

                // Precio
                OutlinedTextField(
                    value = precio,
                    onValueChange = {
                        if (it.isEmpty() || it.toDoubleOrNull() != null) {
                            precio = it
                            precioError = null
                        }
                    },
                    label = { Text("Precio por Día (S/.) *") },
                    leadingIcon = {
                        Icon(Icons.Default.AttachMoney, contentDescription = null)
                    },
                    isError = precioError != null,
                    supportingText = {
                        precioError?.let { Text(it) }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                // Descripción (opcional)
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción (opcional)") },
                    leadingIcon = {
                        Icon(Icons.Default.Description, contentDescription = null)
                    },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validateFields()) {
                        onConfirm(
                            vestuario.copy(
                                danza = danza.trim(),
                                departamento = departamento,
                                descripcion = descripcion.trim(),
                                talla = talla,
                                precio = precio.toDouble(),
                                estado = estado
                            )
                        )
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Guardar Cambios")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancelar")
            }
        },
        shape = CustomShapes.DialogShape
    )
}