package com.raymi.app.presentation.clientes

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.Timestamp
import com.raymi.app.core.theme.CustomShapes
import com.raymi.app.core.utils.Validators
import com.raymi.app.domain.model.Cliente
import com.raymi.app.domain.model.Resource
import com.raymi.app.data.remote.ReniecData
import com.raymi.app.presentation.components.RaymiPhoneField
import androidx.compose.ui.platform.testTag
import com.raymi.app.core.lang.LocalRaymiStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClienteDialog(
    viewModel: ClientesViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
    onConfirm: (Cliente) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoading = uiState.isLoading
    val strings = LocalRaymiStrings.current

    var dni by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }

    var dniError by remember { mutableStateOf<String?>(null) }
    var nombreError by remember { mutableStateOf(false) }
    var apellidosError by remember { mutableStateOf(false) }
    var telefonoError by remember { mutableStateOf(false) }

    var isConsultingReniec by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    fun consultarReniec() {
        if (dni.length == 8) {
            viewModel.consultarReniec(dni) { resource ->
                when (resource) {
                    is Resource.Loading -> isConsultingReniec = true
                    is Resource.Success -> {
                        isConsultingReniec = false
                        resource.data?.let { data ->
                            nombre = data.nombres
                            apellidos = "${data.apellidoPaterno} ${data.apellidoMaterno}"
                            dniError = null
                            nombreError = false
                            apellidosError = false
                        }
                    }
                    is Resource.Error -> {
                        isConsultingReniec = false
                        dniError = if (strings is com.raymi.app.core.lang.SpanishStrings) "DNI no encontrado" else "ID not found"
                    }
                }
            }
        } else {
            dniError = strings.errorDniLength
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Column {
                Text(strings.addClient, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "Ingresa los datos del cliente" else "Enter client data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dni,
                        onValueChange = { if (it.length <= 8) dni = it; dniError = null },
                        label = { Text(strings.dni) },
                        modifier = Modifier.weight(1f).testTag("cliente_dni_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
                        isError = dniError != null,
                        shape = MaterialTheme.shapes.large,
                        leadingIcon = { Icon(Icons.Default.Badge, null) }
                    )

                    IconButton(
                        onClick = { consultarReniec() },
                        modifier = Modifier.size(56.dp).padding(top = 8.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        enabled = !isConsultingReniec
                    ) {
                        if (isConsultingReniec) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                if (dniError != null) Text(dniError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it; if(it.isNotBlank()) nombreError = false },
                    label = { Text(strings.names) },
                    modifier = Modifier.fillMaxWidth().testTag("cliente_nombre_input"),
                    isError = nombreError,
                    shape = MaterialTheme.shapes.large,
                    leadingIcon = { Icon(Icons.Default.Person, null) }
                )

                OutlinedTextField(
                    value = apellidos,
                    onValueChange = { apellidos = it; if(it.isNotBlank()) apellidosError = false },
                    label = { Text(strings.surnames) },
                    modifier = Modifier.fillMaxWidth().testTag("cliente_apellidos_input"),
                    isError = apellidosError,
                    shape = MaterialTheme.shapes.large
                )

                RaymiPhoneField(
                    phone = telefono,
                    onPhoneChange = { telefono = it; telefonoError = false },
                    isError = telefonoError,
                    label = strings.phone
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("${strings.email} ${strings.optional}") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    leadingIcon = { Icon(Icons.Default.MailOutline, null) }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dniLimpio = dni.trim()
                    val nombreLimpio = nombre.trim()
                    val apellidosLimpio = apellidos.trim()
                    val telefonoLimpio = telefono.trim()

                    var hasError = false
                    if (dniLimpio.length != 8) {
                        dniError = strings.errorDniLength
                        hasError = true
                    }
                    if (nombreLimpio.isBlank()) {
                        nombreError = true
                        hasError = true
                    }
                    if (apellidosLimpio.isBlank()) {
                        apellidosError = true
                        hasError = true
                    }
                    if (telefonoLimpio.length != 9) {
                        telefonoError = true
                        hasError = true
                    }

                    if (hasError) return@Button

                    onConfirm(Cliente(
                        dni = dniLimpio,
                        nombre = nombreLimpio,
                        apellidos = apellidosLimpio,
                        telefono = telefonoLimpio,
                        email = email.trim(),
                        direccion = direccion.trim(),
                        createdAt = Timestamp.now()
                    ))
                },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("cliente_guardar_button"),
                shape = MaterialTheme.shapes.large
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                else Text(strings.saveClient, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        },
        shape = CustomShapes.DialogShape
    )
}
