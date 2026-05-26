package com.raymi.app.presentation.categorias

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raymi.app.domain.model.Categoria
import com.raymi.app.presentation.components.RaymiEmptyState
import com.raymi.app.presentation.components.RaymiLoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriasScreen(
    viewModel: CategoriasViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showAddDialog by remember { mutableStateOf(false) }
    var categoriaAEditar by remember { mutableStateOf<Categoria?>(null) }
    var categoriaAEliminar by remember { mutableStateOf<Categoria?>(null) }
    
    var tempNombre by remember { mutableStateOf("") }

    LaunchedEffect(uiState.error, uiState.successMessage) {
        uiState.error?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
        uiState.successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Categorías de Inventario", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Categoría")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                uiState.isLoading -> RaymiLoadingIndicator(message = "Sincronizando...")
                uiState.categorias.isEmpty() -> {
                    RaymiEmptyState(
                        icon = Icons.Default.Category,
                        title = "Sin Categorías",
                        description = "Organiza tu inventario creando categorías.",
                        actionText = "Crear Categoría",
                        onActionClick = { showAddDialog = true }
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.categorias, key = { it.id }) { categoria ->
                            CategoryCard(
                                categoria = categoria,
                                onEdit = { 
                                    categoriaAEditar = categoria
                                    tempNombre = categoria.nombre
                                },
                                onDelete = { categoriaAEliminar = categoria }
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo Añadir
    if (showAddDialog) {
        CategoryDialog(
            title = "Nueva Categoría",
            nombre = tempNombre,
            onNombreChange = { tempNombre = it },
            onDismiss = { showAddDialog = false; tempNombre = "" },
            onConfirm = {
                viewModel.agregarCategoria(tempNombre)
                showAddDialog = false
                tempNombre = ""
            }
        )
    }

    // Diálogo Editar
    if (categoriaAEditar != null) {
        CategoryDialog(
            title = "Editar Categoría",
            nombre = tempNombre,
            onNombreChange = { tempNombre = it },
            onDismiss = { categoriaAEditar = null; tempNombre = "" },
            onConfirm = {
                viewModel.editarCategoria(categoriaAEditar!!, tempNombre)
                categoriaAEditar = null
                tempNombre = ""
            }
        )
    }

    // Diálogo Eliminar
    if (categoriaAEliminar != null) {
        AlertDialog(
            onDismissRequest = { categoriaAEliminar = null },
            title = { Text("¿Eliminar categoría?") },
            text = { Text("Se eliminará '${categoriaAEliminar?.nombre}'. Los ítems en esta categoría no se borrarán, pero quedarán sin clasificación.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eliminarCategoria(categoriaAEliminar!!)
                        categoriaAEliminar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { categoriaAEliminar = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
fun CategoryCard(
    categoria: Categoria,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Text(categoria.nombre, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun CategoryDialog(
    title: String,
    nombre: String,
    onNombreChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = nombre,
                onValueChange = onNombreChange,
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = nombre.isNotBlank()) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
