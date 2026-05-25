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

/**
 * Diálogo para agregar un nuevo cliente con Diseño Senior.
 * Optimizado para velocidad: Consulta RENIEC integrada y validaciones proactivas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClienteDialog(
    viewModel: ClientesViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
    onConfirm: (Cliente) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoading = uiState.isLoading
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
                        dniError = "DNI no encontrado"
                    }
                }
            }
        } else {
            dniError = "Ingrese 8 dígitos"
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Column {
                Text("Nuevo Registro", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text("Ingresa los datos del cliente", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Identificación y Consulta Rápida
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dni,
                        onValueChange = { if (it.length <= 8) dni = it; dniError = null },
                        label = { Text("DNI") },
                        modifier = Modifier.weight(1f),
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

                // 2. Datos Personales
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it; if(it.isNotBlank()) nombreError = false },
                    label = { Text("Nombres") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = nombreError,
                    shape = MaterialTheme.shapes.large,
                    leadingIcon = { Icon(Icons.Default.Person, null) }
                )

                OutlinedTextField(
                    value = apellidos,
                    onValueChange = { apellidos = it; if(it.isNotBlank()) apellidosError = false },
                    label = { Text("Apellidos") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = apellidosError,
                    shape = MaterialTheme.shapes.large
                )

                // 3. Contacto Directo
                OutlinedTextField(
                    value = telefono,
                    onValueChange = { if (it.length <= 9) { telefono = it; if(it.length >= 9) telefonoError = false } },
                    label = { Text("WhatsApp / Celular") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = telefonoError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = MaterialTheme.shapes.large,
                    leadingIcon = { Icon(Icons.Default.Phone, null) }
                )
                
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (Opcional)") },
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

                    // Validaciones Senior (QA: No vacíos)
                    var hasError = false
                    if (dniLimpio.length != 8) {
                        dniError = "El DNI debe tener 8 dígitos"
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
                    if (telefonoLimpio.length < 9) {
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
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = MaterialTheme.shapes.large
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                else Text("Guardar Cliente", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        shape = CustomShapes.DialogShape
    )
}
