package com.raymi.app.presentation.clientes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.raymi.app.domain.model.Cliente
import com.raymi.app.presentation.components.*
import java.util.Locale
import androidx.compose.ui.window.Dialog

/**
 * Detalle del Cliente Premium.
 * Diseño Senior: Cabecera elegante, métricas de fidelización y Respaldo de Identidad (Seguridad).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClienteDetailScreen(
    clienteId: String,
    viewModel: ClienteDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToAlquiler: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val strings = com.raymi.app.core.lang.LocalRaymiStrings.current
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.successMessage, uiState.error) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(clienteId) {
        viewModel.loadClienteData()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(strings.clientProfile, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = strings.edit)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                uiState.isLoading -> RaymiLoadingIndicator(message = "Consultando datos...")
                uiState.error != null -> RaymiErrorState(message = uiState.error!!, onRetry = { viewModel.loadClienteData() })
                uiState.cliente != null -> {
                    val cliente = uiState.cliente!!
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // 1. Cabecera de Identidad
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            AvatarWithInitials(
                                initials = cliente.iniciales,
                                size = 100,
                                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                                textColor = MaterialTheme.colorScheme.primary
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(cliente.nombreCompleto, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                                Text("Cliente desde: ${cliente.createdAtFormatted}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // 2. KPIs de Fidelización
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            InfoSquareSmall(
                                label = strings.rentals,
                                value = uiState.totalAlquileres.toString(),
                                icon = Icons.Default.Repeat,
                                modifier = Modifier.weight(1f)
                            )
                            InfoSquareSmall(
                                label = if (strings is com.raymi.app.core.lang.SpanishStrings) "Inversión" else "Investment",
                                value = "S/. ${String.format(Locale.getDefault(), "%,.2f", uiState.totalGastado)}",
                                icon = Icons.Default.Payments,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // 3. Respaldo de Identidad (Seguridad Crítica)
                        Text(strings.identityBackup, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                SecurityPhotoCard(
                                    label = strings.idFront,
                                    url = cliente.fotoDniFrontUrl,
                                    modifier = Modifier.weight(1f)
                                )
                                SecurityPhotoCard(
                                    label = strings.idBack,
                                    url = cliente.fotoDniBackUrl,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            SecurityPhotoCard(
                                label = strings.facePhoto,
                                url = cliente.fotoRostroUrl,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // 4. Información de Contacto
                        Text(strings.contactData, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                ContactRow(icon = Icons.Default.Badge, label = strings.dni, value = cliente.dni)
                                ContactRow(icon = Icons.Default.Phone, label = strings.phone, value = cliente.telefono)
                                if (cliente.email.isNotBlank()) {
                                    ContactRow(icon = Icons.Default.Mail, label = strings.email, value = cliente.email)
                                }
                                if (cliente.direccion.isNotBlank()) {
                                    ContactRow(icon = Icons.Default.LocationOn, label = strings.address, value = cliente.direccion)
                                }
                            }
                        }

                        // 5. Historial Reciente
                        Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "Historial Reciente" else "Recent History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (uiState.alquileres.isEmpty()) {
                            RaymiEmptyState(
                                icon = Icons.Default.History,
                                title = strings.noMovements,
                                description = if (strings is com.raymi.app.core.lang.SpanishStrings) "Este cliente aún no tiene alquileres registrados." else "This client has no rental records yet."
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                uiState.alquileres.take(5).forEach { alquiler ->
                                    AlquilerItem(
                                        alquiler = alquiler,
                                        onClick = { onNavigateToAlquiler(alquiler.id) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    if (showEditDialog && uiState.cliente != null) {
        EditClienteDialog(
            cliente = uiState.cliente!!,
            onDismiss = { showEditDialog = false },
            onConfirm = { clienteActualizado, front, back, face ->
                viewModel.updateCliente(clienteActualizado, front, back, face)
                showEditDialog = false
            },
            isLoading = uiState.isLoading
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(strings.deleteClient) },
            text = { Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "¿Estás seguro de eliminar a este cliente? Esta acción no se puede deshacer y borrará su historial y fotos de seguridad." else "Are you sure you want to delete this client? This action cannot be undone and will delete history and security photos.") },
            confirmButton = {
                Button(
                    onClick = { 
                        viewModel.eliminarCliente { onNavigateBack() }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
fun SecurityPhotoCard(label: String, url: String?, modifier: Modifier = Modifier) {
    var showFullscreen by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clickable(enabled = !url.isNullOrBlank()) { showFullscreen = true },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            if (!url.isNullOrBlank()) {
                AsyncImage(
                    model = url,
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.NoPhotography, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                }
            }
        }
    }

    if (showFullscreen && !url.isNullOrBlank()) {
        Dialog(onDismissRequest = { showFullscreen = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(16.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = Color.Black
            ) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
fun InfoSquareSmall(label: String, value: String, icon: ImageVector, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ContactRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}
