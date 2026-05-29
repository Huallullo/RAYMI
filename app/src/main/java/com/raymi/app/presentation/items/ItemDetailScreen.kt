package com.raymi.app.presentation.items

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raymi.app.core.utils.QrCodeGenerator
import androidx.compose.ui.graphics.asImageBitmap
import com.raymi.app.domain.model.Item
import com.raymi.app.presentation.components.EstadoBadge
import com.raymi.app.presentation.components.RaymiErrorState
import com.raymi.app.presentation.components.RaymiLoadingIndicator
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    itemId: String,
    viewModel: ItemDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onEditItem: (String) -> Unit,
    onRentItem: (String) -> Unit,
    onNavigateToMaintenance: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }

    // Observar mensajes
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(itemId) {
        viewModel.loadItem(itemId)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = { Text(uiState.item?.nombre ?: "Detalle del Producto", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { onEditItem(itemId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = { showQrDialog = true }) {
                        Icon(Icons.Default.QrCode, contentDescription = "Ver QR")
                    }
                    IconButton(onClick = { onNavigateToMaintenance(itemId) }) {
                        Icon(Icons.Default.Build, contentDescription = "Mantenimiento")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.item != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Button(
                        onClick = { onRentItem(itemId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .height(56.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        enabled = uiState.item?.estado == "DISPONIBLE"
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Alquilar Ahora", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                uiState.isLoading -> RaymiLoadingIndicator(message = "Consultando disponibilidad...")
                uiState.error != null -> RaymiErrorState(message = uiState.error!!, onRetry = { viewModel.loadItem(itemId) })
                uiState.item != null -> ItemDetailContent(uiState.item!!)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar Producto") },
            text = { Text("¿Estás seguro de eliminar este producto? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = { 
                        viewModel.eliminarItem { onNavigateBack() }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showQrDialog && uiState.item != null) {
        val qrBitmap = remember(uiState.item?.codigo) {
            QrCodeGenerator.generateQrCode(uiState.item!!.codigo)
        }
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            title = { Text("QR del Ítem", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    if (qrBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Código QR",
                            modifier = Modifier.size(200.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(uiState.item!!.codigo, fontWeight = FontWeight.ExtraBold)
                    } else {
                        Text("Error al generar QR")
                    }
                }
            },
            confirmButton = { Button(onClick = { showQrDialog = false }) { Text("Cerrar") } }
        )
    }
}

@Composable
fun ItemDetailContent(item: Item) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 0. Imagen del Producto
        if (item.imagenUrl != null) {
            AsyncImage(
                model = item.imagenUrl,
                contentDescription = item.nombre,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(MaterialTheme.shapes.extraLarge),
                contentScale = ContentScale.Crop
            )
        }

        // 1. Encabezado de Estado y SKU
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("CÓDIGO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(item.codigo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            }
            EstadoBadge(
                texto = item.estado,
                color = when(item.estado) {
                    "DISPONIBLE" -> Color(0xFF4CAF50)
                    "ALQUILADO" -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                }
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // 2. Grid de Información Financiera/Stock
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            InfoSquare(
                label = "Precio Alquiler",
                value = "S/. ${item.precio}",
                icon = Icons.Default.AttachMoney,
                modifier = Modifier.weight(1f)
            )
            InfoSquare(
                label = "Stock Total",
                value = "${item.cantidad} und",
                icon = Icons.Default.Inventory,
                modifier = Modifier.weight(1f)
            )
        }

        // 3. Atributos Dinámicos (El corazón del SaaS)
        if (item.atributos.isNotEmpty()) {
            Text("Especificaciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item.atributos.forEach { (clave, valor) ->
                    AttributeRow(label = clave.replaceFirstChar { it.uppercase() }, value = valor)
                }
            }
        }

        // 4. Descripción
        if (item.descripcion.isNotBlank()) {
            Text("Descripción", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                item.descripcion,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 24.sp
            )
        }

        Spacer(modifier = Modifier.height(100.dp)) // Espacio para el botón inferior
    }
}

@Composable
fun InfoSquare(label: String, value: String, icon: ImageVector, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AttributeRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                value,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
