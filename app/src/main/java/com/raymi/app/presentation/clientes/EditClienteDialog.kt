package com.raymi.app.presentation.clientes

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
import com.raymi.app.core.utils.Validators
import com.raymi.app.domain.model.Cliente

/**
 * Diálogo para editar un cliente existente
 * Incluye validación de campos
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditClienteDialog(
    cliente: Cliente,
    onDismiss: () -> Unit,
    onConfirm: (Cliente) -> Unit,
    isLoading: Boolean = false
) {
    var dni by remember { mutableStateOf(cliente.dni) }
    var nombre by remember { mutableStateOf(cliente.nombre) }
    var apellidos by remember { mutableStateOf(cliente.apellidos) }
    var telefono by remember { mutableStateOf(cliente.telefono) }
    var email by remember { mutableStateOf(cliente.email) }
    var direccion by remember { mutableStateOf(cliente.direccion) }

    var dniError by remember { mutableStateOf<String?>(null) }
    var nombreError by remember { mutableStateOf<String?>(null) }
    var apellidosError by remember { mutableStateOf<String?>(null) }
    var telefonoError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    /**
     * Valida todos los campos
     */
    fun validateFields(): Boolean {
        var isValid = true

        // Validar DNI
        val dniValidation = Validators.validateDni(dni)
        if (!dniValidation.isValid) {
            dniError = dniValidation.errorMessage
            isValid = false
        } else {
            dniError = null
        }

        // Validar nombre
        val nombreValidation = Validators.validateNombre(nombre)
        if (!nombreValidation.isValid) {
            nombreError = nombreValidation.errorMessage
            isValid = false
        } else {
            nombreError = null
        }

        // Validar apellidos
        val apellidosValidation = Validators.validateApellidos(apellidos)
        if (!apellidosValidation.isValid) {
            apellidosError = apellidosValidation.errorMessage
            isValid = false
        } else {
            apellidosError = null
        }

        // Validar teléfono
        val telefonoValidation = Validators.validateTelefono(telefono)
        if (!telefonoValidation.isValid) {
            telefonoError = telefonoValidation.errorMessage
            isValid = false
        } else {
            telefonoError = null
        }

        // Validar email (opcional)
        val emailValidation = Validators.validateEmail(email, isRequired = false)
        if (!emailValidation.isValid) {
            emailError = emailValidation.errorMessage
            isValid = false
        } else {
            emailError = null
        }

        return isValid
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null)
                Text("Editar Cliente")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // DNI (solo lectura para evitar duplicados)
                OutlinedTextField(
                    value = dni,
                    onValueChange = {},
                    label = { Text("DNI") },
                    leadingIcon = {
                        Icon(Icons.Filled.Badge, contentDescription = null)
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
                    text = "El DNI no se puede modificar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Nombre
                OutlinedTextField(
                    value = nombre,
                    onValueChange = {
                        nombre = it
                        nombreError = null
                    },
                    label = { Text("Nombre *") },
                    leadingIcon = {
                        Icon(Icons.Filled.Person, contentDescription = null)
                    },
                    isError = nombreError != null,
                    supportingText = {
                        nombreError?.let { Text(it) }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                // Apellidos
                OutlinedTextField(
                    value = apellidos,
                    onValueChange = {
                        apellidos = it
                        apellidosError = null
                    },
                    label = { Text("Apellidos *") },
                    leadingIcon = {
                        Icon(Icons.Filled.Person, contentDescription = null)
                    },
                    isError = apellidosError != null,
                    supportingText = {
                        apellidosError?.let { Text(it) }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                // Teléfono
                OutlinedTextField(
                    value = telefono,
                    onValueChange = {
                        if (it.length <= 9) telefono = it
                        telefonoError = null
                    },
                    label = { Text("Teléfono *") },
                    leadingIcon = {
                        Icon(Icons.Filled.Phone, contentDescription = null)
                    },
                    isError = telefonoError != null,
                    supportingText = {
                        telefonoError?.let { Text(it) }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                // Email (opcional)
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = null
                    },
                    label = { Text("Email (opcional)") },
                    leadingIcon = {
                        Icon(Icons.Filled.Email, contentDescription = null)
                    },
                    isError = emailError != null,
                    supportingText = {
                        emailError?.let { Text(it) }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                // Dirección (opcional)
                OutlinedTextField(
                    value = direccion,
                    onValueChange = { direccion = it },
                    label = { Text("Dirección (opcional)") },
                    leadingIcon = {
                        Icon(Icons.Filled.Home, contentDescription = null)
                    },
                    maxLines = 2,
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
                            cliente.copy(
                                nombre = nombre.trim(),
                                apellidos = apellidos.trim(),
                                telefono = telefono,
                                email = email.trim(),
                                direccion = direccion.trim()
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
