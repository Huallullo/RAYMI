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
import androidx.compose.ui.platform.testTag
import com.raymi.app.domain.model.Categoria
import com.raymi.app.presentation.components.RaymiEmptyState
import com.raymi.app.presentation.components.RaymiLoadingIndicator
import com.raymi.app.core.lang.LocalRaymiStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriasScreen(
    viewModel: CategoriasViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val strings = LocalRaymiStrings.current
    
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
                title = { Text(strings.categories, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("fab_add_categoria")
            ) {
                Icon(Icons.Default.Add, contentDescription = strings.addItem)
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                uiState.isLoading -> RaymiLoadingIndicator(message = strings.loading)
                uiState.categorias.isEmpty() -> {
                    RaymiEmptyState(
                        icon = Icons.Default.Category,
                        title = strings.categories,
                        description = if (strings is com.raymi.app.core.lang.SpanishStrings) "No se encontraron categorías." else "No categories found.",
                        actionText = strings.newItem,
                        onActionClick = { showAddDialog = true }
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp, start = 24.dp, end = 24.dp, top = 24.dp), // Espacio para el FAB y cápsula
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.categorias, key = { it.id }) { categoria ->
                            CategoryCard(
                                categoria = categoria,
                                onEdit = { 
                                    categoriaAEditar = categoria
                                    tempNombre = categoria.nombre
                                },
                                onDelete = { categoriaAEliminar = categoria },
                                editLabel = strings.edit,
                                deleteLabel = strings.delete
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
            title = strings.newItem,
            nombre = tempNombre,
            onNombreChange = { tempNombre = it },
            onDismiss = { showAddDialog = false; tempNombre = "" },
            onConfirm = {
                viewModel.agregarCategoria(tempNombre)
                showAddDialog = false
                tempNombre = ""
            },
            saveLabel = strings.save,
            cancelLabel = strings.cancel
        )
    }

    // Diálogo Editar
    if (categoriaAEditar != null) {
        CategoryDialog(
            title = strings.edit,
            nombre = tempNombre,
            onNombreChange = { tempNombre = it },
            onDismiss = { categoriaAEditar = null; tempNombre = "" },
            onConfirm = {
                viewModel.editarCategoria(categoriaAEditar!!, tempNombre)
                categoriaAEditar = null
                tempNombre = ""
            },
            saveLabel = strings.save,
            cancelLabel = strings.cancel
        )
    }

    // Diálogo Eliminar
    if (categoriaAEliminar != null) {
        AlertDialog(
            onDismissRequest = { categoriaAEliminar = null },
            title = { Text(strings.delete + "?") },
            text = { Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "¿Estás seguro? Los productos en esta categoría quedarán sin clasificar." else "Are you sure? Items in this category will become unclassified.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eliminarCategoria(categoriaAEliminar!!)
                        categoriaAEliminar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(strings.delete) }
            },
            dismissButton = { TextButton(onClick = { categoriaAEliminar = null }) { Text(strings.cancel) } }
        )
    }
}

@Composable
fun CategoryCard(
    categoria: Categoria,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    editLabel: String,
    deleteLabel: String
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
                Icon(Icons.Default.Edit, contentDescription = editLabel, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = deleteLabel, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
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
    onConfirm: () -> Unit,
    saveLabel: String,
    cancelLabel: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = nombre,
                onValueChange = onNombreChange,
                label = { Text(if (LocalRaymiStrings.current is com.raymi.app.core.lang.SpanishStrings) "Nombre" else "Name") },
                modifier = Modifier.fillMaxWidth().testTag("categoria_nombre_input"),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm, 
                enabled = nombre.isNotBlank(),
                modifier = Modifier.testTag("categoria_guardar_button")
            ) { Text(saveLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(cancelLabel) } }
    )
}
