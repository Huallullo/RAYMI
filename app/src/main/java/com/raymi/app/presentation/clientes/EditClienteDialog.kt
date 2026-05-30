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
import com.raymi.app.presentation.components.RaymiPhoneField
import com.raymi.app.core.lang.LocalRaymiStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditClienteDialog(
    cliente: Cliente,
    onDismiss: () -> Unit,
    onConfirm: (Cliente) -> Unit,
    isLoading: Boolean = false
) {
    val strings = LocalRaymiStrings.current
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

    fun validateFields(): Boolean {
        var isValid = true

        val dniValidation = Validators.validateDni(dni)
        if (!dniValidation.isValid) {
            dniError = if (strings is com.raymi.app.core.lang.SpanishStrings) dniValidation.errorMessage else strings.errorDniLength
            isValid = false
        } else {
            dniError = null
        }

        val nombreValidation = Validators.validateNombre(nombre)
        if (!nombreValidation.isValid) {
            nombreError = if (strings is com.raymi.app.core.lang.SpanishStrings) nombreValidation.errorMessage else strings.errorNamesRequired
            isValid = false
        } else {
            nombreError = null
        }

        val apellidosValidation = Validators.validateApellidos(apellidos)
        if (!apellidosValidation.isValid) {
            apellidosError = if (strings is com.raymi.app.core.lang.SpanishStrings) apellidosValidation.errorMessage else strings.errorSurnamesRequired
            isValid = false
        } else {
            apellidosError = null
        }

        val telefonoValidation = Validators.validateTelefono(telefono)
        if (!telefonoValidation.isValid) {
            telefonoError = if (strings is com.raymi.app.core.lang.SpanishStrings) telefonoValidation.errorMessage else strings.errorPhoneLength
            isValid = false
        } else {
            telefonoError = null
        }

        return isValid
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Edit, contentDescription = null)
                Text(strings.editClient)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = dni,
                    onValueChange = {},
                    label = { Text(strings.dni) },
                    leadingIcon = { Icon(Icons.Filled.Badge, null) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                )

                Text(
                    text = if (strings is com.raymi.app.core.lang.SpanishStrings) "El DNI no se puede modificar" else "ID cannot be modified",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it; nombreError = null },
                    label = { Text(strings.names + " *") },
                    leadingIcon = { Icon(Icons.Filled.Person, null) },
                    isError = nombreError != null,
                    supportingText = { nombreError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                OutlinedTextField(
                    value = apellidos,
                    onValueChange = { apellidos = it; apellidosError = null },
                    label = { Text(strings.surnames + " *") },
                    leadingIcon = { Icon(Icons.Filled.Person, null) },
                    isError = apellidosError != null,
                    supportingText = { apellidosError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                RaymiPhoneField(
                    phone = telefono,
                    onPhoneChange = { telefono = it; telefonoError = null },
                    isError = telefonoError != null,
                    supportingText = telefonoError,
                    label = strings.phone
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; emailError = null },
                    label = { Text("${strings.email} ${strings.optional}") },
                    leadingIcon = { Icon(Icons.Filled.Email, null) },
                    isError = emailError != null,
                    supportingText = { emailError?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                OutlinedTextField(
                    value = direccion,
                    onValueChange = { direccion = it },
                    label = { Text("${strings.address} ${strings.optional}") },
                    leadingIcon = { Icon(Icons.Filled.Home, null) },
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
                        onConfirm(cliente.copy(
                            nombre = nombre.trim(),
                            apellidos = apellidos.trim(),
                            telefono = telefono,
                            email = email.trim(),
                            direccion = direccion.trim()
                        ))
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text(strings.cancel) }
        },
        shape = CustomShapes.DialogShape
    )
}
