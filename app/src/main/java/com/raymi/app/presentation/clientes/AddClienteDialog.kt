package com.raymi.app.presentation.clientes


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.Timestamp
import com.raymi.app.core.theme.CustomShapes
import com.raymi.app.core.utils.Validators
import com.raymi.app.domain.model.Cliente
import com.raymi.app.presentation.clientes.ClientesViewModel

/**
 * Diálogo para agregar un nuevo cliente
 * Incluye validación de campos y consulta a RENIEC
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClienteDialog(
    viewModel: ClientesViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
    onConfirm: (Cliente) -> Unit,
    isLoading: Boolean = false
) {
    var dni by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }

    var dniError by remember { mutableStateOf<String?>(null) }
    var nombreError by remember { mutableStateOf<String?>(null) }
    var apellidosError by remember { mutableStateOf<String?>(null) }
    var telefonoError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }

    var isConsultingReniec by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    /**
     * Consulta datos en RENIEC
     */
    fun consultarReniec() {
        if (dni.length == 8) {
            isConsultingReniec = true
            viewModel.consultarReniec(dni) { result ->
                isConsultingReniec = false
                result.onSuccess { reniecData ->
                    nombre = reniecData.nombres
                    apellidos = "${reniecData.apellidoPaterno} ${reniecData.apellidoMaterno}"
                    nombreError = null
                    apellidosError = null
                }.onFailure { error ->
                    dniError = error.message ?: "Error al consultar RENIEC"
                }
            }
        }
    }

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
                Icon(Icons.Filled.PersonAdd, contentDescription = null)
                Text("Agregar Cliente")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // DNI
                OutlinedTextField(
                    value = dni,
                    onValueChange = {
                        if (it.length <= 8) dni = it
                        dniError = null
                    },
                    label = { Text("DNI *") },
                    leadingIcon = {
                        Icon(Icons.Filled.Badge, contentDescription = null)
                    },
                    isError = dniError != null,
                    supportingText = {
                        dniError?.let { Text(it) }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
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

                // Botón para consultar RENIEC
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            dniError = null
                            consultarReniec()
                        },
                        enabled = !isLoading && !isConsultingReniec
                    ) {
                        if (isConsultingReniec) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Filled.Search, contentDescription = null)
                            Text("Consultar RENIEC")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validateFields()) {
                        onConfirm(
                            Cliente(
                                dni = dni,
                                nombre = nombre.trim(),
                                apellidos = apellidos.trim(),
                                telefono = telefono,
                                email = email.trim(),
                                direccion = direccion.trim(),
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