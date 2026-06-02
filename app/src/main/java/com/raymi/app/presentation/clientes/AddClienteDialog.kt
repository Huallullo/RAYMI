package com.raymi.app.presentation.clientes

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.Timestamp
import com.raymi.app.core.theme.CustomShapes
import com.raymi.app.domain.model.Cliente
import com.raymi.app.domain.model.Resource
import com.raymi.app.presentation.components.RaymiPhoneField
import com.raymi.app.presentation.components.PhotoCaptureField
import androidx.compose.ui.platform.testTag
import com.raymi.app.core.lang.LocalRaymiStrings
import android.content.Intent
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClienteDialog(
    viewModel: ClientesViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoading = uiState.isLoading
    val strings = LocalRaymiStrings.current
    val context = LocalContext.current

    var dni by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }

    // Uris para fotos de seguridad
    var dniFrontUri by remember { mutableStateOf<Uri?>(null) }
    var dniBackUri by remember { mutableStateOf<Uri?>(null) }
    var faceUri by remember { mutableStateOf<Uri?>(null) }

    // Launchers de Cámara
    var tempUri by remember { mutableStateOf<Uri?>(null) }
    var captureTarget by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            when (captureTarget) {
                "front" -> dniFrontUri = tempUri
                "back" -> dniBackUri = tempUri
                "face" -> faceUri = tempUri
            }
        }
    }

    fun startCapture(target: String) {
        try {
            val file = File.createTempFile("client_${target}_", ".jpg", context.cacheDir)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            tempUri = uri
            captureTarget = target
            cameraLauncher.launch(uri)
        } catch (_: Exception) {
            // Error al crear archivo temporal
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            captureTarget?.let { startCapture(it) }
        } else {
            viewModel.showCameraPermissionAlert()
        }
    }

    fun checkAndStartCapture(target: String) {
        captureTarget = target
        val permission = android.Manifest.permission.CAMERA
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            startCapture(target)
        } else {
            permissionLauncher.launch(permission)
        }
    }

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
                        dniError = resource.message ?: strings.idNotFound
                    }
                }
            }
        } else {
            dniError = strings.errorDniLength
        }
    }

    if (uiState.showCameraPermissionAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissCameraPermissionAlert() },
            title = { Text(strings.permissionDenied, fontWeight = FontWeight.Black) },
            text = { Text(strings.cameraPermissionDesc) },
            confirmButton = {
                Button(onClick = {
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                    viewModel.dismissCameraPermissionAlert()
                }) {
                    Text(strings.openSettings)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissCameraPermissionAlert() }) {
                    Text(strings.close)
                }
            },
            icon = { Icon(Icons.Default.CameraAlt, null, tint = MaterialTheme.colorScheme.error) }
        )
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Column {
                Text(strings.addClient, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(strings.identityBackup, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // DATOS PERSONALES
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dni,
                        onValueChange = { if (it.length <= 8) dni = it; dniError = null },
                        label = { Text(strings.dni) },
                        modifier = Modifier.weight(1f).testTag("cliente_dni_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
                        isError = dniError != null,
                        supportingText = {
                            if (dniError != null) {
                                Text(dniError!!, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        shape = MaterialTheme.shapes.large,
                        leadingIcon = { Icon(Icons.Default.Badge, null) }
                    )

                    IconButton(
                        onClick = { 
                            android.util.Log.d("AddCliente", "Consultando DNI: $dni")
                            consultarReniec() 
                        },
                        modifier = Modifier.size(56.dp).padding(top = 8.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        enabled = !isConsultingReniec && dni.length == 8
                    ) {
                        if (isConsultingReniec) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it; if(it.isNotBlank()) nombreError = false },
                    label = { Text(strings.names) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = nombreError,
                    supportingText = { if(nombreError) Text(strings.errorNamesRequired, color = MaterialTheme.colorScheme.error) },
                    shape = MaterialTheme.shapes.large,
                    leadingIcon = { Icon(Icons.Default.Person, null) }
                )

                OutlinedTextField(
                    value = apellidos,
                    onValueChange = { apellidos = it; if(it.isNotBlank()) apellidosError = false },
                    label = { Text(strings.surnames) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = apellidosError,
                    supportingText = { if(apellidosError) Text(strings.errorSurnamesRequired, color = MaterialTheme.colorScheme.error) },
                    shape = MaterialTheme.shapes.large
                )

                RaymiPhoneField(
                    phone = telefono, 
                    onPhoneChange = { telefono = it; telefonoError = false }, 
                    isError = telefonoError, 
                    label = strings.phone
                )
                if (telefonoError) {
                    Text(strings.errorPhoneLength, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 16.dp))
                }

                // SECCIÓN DE SEGURIDAD (FOTOS) - OPCIONAL
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(strings.identityBackup.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PhotoCaptureField(
                        label = strings.idFront,
                        imageUri = dniFrontUri,
                        onCaptureClick = { checkAndStartCapture("front") },
                        onClearClick = { dniFrontUri = null },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AddAPhoto
                    )
                    PhotoCaptureField(
                        label = strings.idBack,
                        imageUri = dniBackUri,
                        onCaptureClick = { checkAndStartCapture("back") },
                        onClearClick = { dniBackUri = null },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AddAPhoto
                    )
                }

                PhotoCaptureField(
                    label = strings.facePhoto,
                    imageUri = faceUri,
                    onCaptureClick = { checkAndStartCapture("face") },
                    onClearClick = { faceUri = null },
                    icon = Icons.Default.Face
                )

                // MENSAJE DE ERROR GENERAL (Ej: Límite de Plan)
                uiState.error?.let {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
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
                    if (dniLimpio.length != 8) { dniError = strings.errorDniLength; hasError = true }
                    if (nombreLimpio.isBlank()) { nombreError = true; hasError = true }
                    if (apellidosLimpio.isBlank()) { apellidosError = true; hasError = true }
                    if (telefonoLimpio.length != 9) { telefonoError = true; hasError = true }

                    if (hasError) return@Button

                    viewModel.addCliente(
                        cliente = Cliente(
                            dni = dniLimpio,
                            nombre = nombreLimpio,
                            apellidos = apellidosLimpio,
                            telefono = telefonoLimpio,
                            email = email.trim(),
                            direccion = direccion.trim()
                        ),
                        dniFront = dniFrontUri,
                        dniBack = dniBackUri,
                        face = faceUri
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = MaterialTheme.shapes.large,
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                else Text(strings.saveClient, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text(strings.cancel) }
        },
        shape = CustomShapes.DialogShape
    )
}
