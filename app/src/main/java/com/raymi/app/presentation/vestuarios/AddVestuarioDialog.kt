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
import com.google.firebase.Timestamp
import com.raymi.app.core.theme.CustomShapes
import com.raymi.app.core.utils.Constants
import com.raymi.app.core.utils.GeneradorCodigo
import com.raymi.app.core.utils.Validators
import com.raymi.app.domain.model.EstadoVestuario
import com.raymi.app.domain.model.Vestuario

/**
 * Diálogo para agregar un nuevo vestuario
 * Incluye validación de campos
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVestuarioDialog(
    onDismiss: () -> Unit,
    onConfirm: (Vestuario) -> Unit,
    isLoading: Boolean = false
) {

    var danza by remember { mutableStateOf("") }
    var departamento by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var talla by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var expandedTalla by remember { mutableStateOf(false) }


    var danzaError by remember { mutableStateOf<String?>(null) }
    var precioError by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    /**
     * Valida todos los campos
     */
    fun validateFields(): Boolean {
        var isValid = true


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
                Icon(Icons.Filled.Checkroom, contentDescription = null)
                Text("Agregar Vestuario")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Danza
                OutlinedTextField(
                    value = danza,
                    onValueChange = {
                        danza = it
                        danzaError = null
                    },
                    label = { Text("Nombre de la Danza *") },
                    leadingIcon = {
                        Icon(Icons.Filled.MusicNote, contentDescription = null)
                    },
                    isError = danzaError != null,
                    supportingText = {
                        danzaError?.let { Text(it) }
                    },
                    placeholder = { Text("Ej: Marinera Norteña") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                // Departamento (Dropdown)
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded && !isLoading }
                ) {
                    OutlinedTextField(
                        value = departamento,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Departamento *") },
                        leadingIcon = {
                            Icon(Icons.Filled.Place, contentDescription = null)
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true),
                        enabled = !isLoading
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        Constants.DEPARTAMENTOS_PERU.forEach { depto ->
                            DropdownMenuItem(
                                text = { Text(depto) },
                                onClick = {
                                    departamento = depto
                                    expanded = false
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
                            Icon(Icons.Filled.Straighten, contentDescription = null)
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTalla)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true),
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
                        Icon(Icons.Filled.AttachMoney, contentDescription = null)
                    },
                    isError = precioError != null,
                    supportingText = {
                        precioError?.let { Text(it) }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    placeholder = { Text("150.00") },
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
                        Icon(Icons.Filled.Description, contentDescription = null)
                    },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
            }
        },
        // En AddVestuarioDialog.kt
// Busca la sección "confirmButton" y reemplázala completamente:

        confirmButton = {
            Button(
                onClick = {
                    if (validateFields()) {
                        onConfirm(
                            Vestuario(
                                codigo = GeneradorCodigo.generarCodigoVestuario(),  // ✅ AUTO-GENERADO
                                danza = danza.trim(),
                                departamento = departamento,
                                descripcion = descripcion.trim(),
                                talla = talla,
                                precio = precio.toDouble(),
                                estado = EstadoVestuario.DISPONIBLE,
                                createdAt = Timestamp.now()
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
                    Text("Guardar")
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
