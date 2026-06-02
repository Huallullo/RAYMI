package com.raymi.app.presentation.clientes

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import android.content.Intent
import com.raymi.app.core.theme.CustomShapes
import com.raymi.app.core.utils.Validators
import com.raymi.app.domain.model.Cliente
import com.raymi.app.presentation.components.RaymiPhoneField
import com.raymi.app.presentation.components.PhotoCaptureField
import com.raymi.app.core.lang.LocalRaymiStrings
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditClienteDialog(
    cliente: Cliente,
    onDismiss: () -> Unit,
    onConfirm: (Cliente, Uri?, Uri?, Uri?) -> Unit,
    isLoading: Boolean = false
) {
    val strings = LocalRaymiStrings.current
    val context = LocalContext.current

    var nombre by remember { mutableStateOf(cliente.nombre) }
    var apellidos by remember { mutableStateOf(cliente.apellidos) }
    var telefono by remember { mutableStateOf(cliente.telefono) }
    var email by remember { mutableStateOf(cliente.email) }
    var direccion by remember { mutableStateOf(cliente.direccion) }

    // Nuevas Uris (si el usuario decide cambiar las fotos existentes)
    var dniFrontUri by remember { mutableStateOf<Uri?>(null) }
    var dniBackUri by remember { mutableStateOf<Uri?>(null) }
    var faceUri by remember { mutableStateOf<Uri?>(null) }

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
        val file = File.createTempFile("edit_client_${target}_", ".jpg", context.cacheDir)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        tempUri = uri
        captureTarget = target
        cameraLauncher.launch(uri)
    }

    // ✅ SEC 4 FIX: Add Camera Permission Check
    var showPermissionAlert by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            captureTarget?.let { startCapture(it) }
        } else {
            showPermissionAlert = true
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

    if (showPermissionAlert) {
        AlertDialog(
            onDismissRequest = { showPermissionAlert = false },
            title = { Text(strings.permissionDenied, fontWeight = FontWeight.Black) },
            text = { Text(strings.cameraPermissionDesc) },
            confirmButton = {
                Button(onClick = {
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                    showPermissionAlert = false
                }) {
                    Text(strings.openSettings)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionAlert = false }) { Text(strings.close) }
            },
            icon = { Icon(Icons.Default.CameraAlt, null, tint = MaterialTheme.colorScheme.error) }
        )
    }

    var nombreError by remember { mutableStateOf<String?>(null) }
    var apellidosError by remember { mutableStateOf<String?>(null) }
    var telefonoError by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    fun validateFields(): Boolean {
        var isValid = true
        if (nombre.isBlank()) { nombreError = strings.errorNamesRequired; isValid = false }
        if (apellidos.isBlank()) { apellidosError = strings.errorSurnamesRequired; isValid = false }
        if (telefono.length != 9) { telefonoError = strings.errorPhoneLength; isValid = false }
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
                OutlinedTextField(value = cliente.dni, onValueChange = {}, label = { Text(strings.dni) }, leadingIcon = { Icon(Icons.Filled.Badge, null) }, modifier = Modifier.fillMaxWidth(), enabled = false)

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it; nombreError = null },
                    label = { Text(strings.names) },
                    isError = nombreError != null,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                OutlinedTextField(
                    value = apellidos,
                    onValueChange = { apellidos = it; apellidosError = null },
                    label = { Text(strings.surnames) },
                    isError = apellidosError != null,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                RaymiPhoneField(phone = telefono, onPhoneChange = { telefono = it; telefonoError = null }, isError = telefonoError != null, label = strings.phone)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("ACTUALIZAR RESPALDO VISUAL (OPCIONAL)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PhotoCaptureField(
                        label = "DNI Frontal",
                        imageUri = dniFrontUri ?: (if (cliente.fotoDniFrontUrl != null) Uri.parse(cliente.fotoDniFrontUrl) else null),
                        onCaptureClick = { checkAndStartCapture("front") },
                        onClearClick = { dniFrontUri = null },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AddAPhoto
                    )
                    PhotoCaptureField(
                        label = "DNI Posterior",
                        imageUri = dniBackUri ?: (if (cliente.fotoDniBackUrl != null) Uri.parse(cliente.fotoDniBackUrl) else null),
                        onCaptureClick = { checkAndStartCapture("back") },
                        onClearClick = { dniBackUri = null },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AddAPhoto
                    )
                }

                PhotoCaptureField(
                    label = "Rostro del Cliente",
                    imageUri = faceUri ?: (if (cliente.fotoRostroUrl != null) Uri.parse(cliente.fotoRostroUrl) else null),
                    onCaptureClick = { checkAndStartCapture("face") },
                    onClearClick = { faceUri = null },
                    icon = Icons.Default.Face
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
                            ),
                            dniFrontUri,
                            dniBackUri,
                            faceUri
                        )
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
