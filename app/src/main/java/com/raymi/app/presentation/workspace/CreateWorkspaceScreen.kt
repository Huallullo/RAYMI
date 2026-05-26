package com.raymi.app.presentation.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Pantalla para registrar un nuevo negocio (Workspace).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWorkspaceScreen(
    viewModel: CreateWorkspaceViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // Si se creó con éxito, volvemos a la selección
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar Negocio") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Cuéntanos sobre tu nuevo negocio",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // Nombre del negocio
            OutlinedTextField(
                value = uiState.nombre,
                onValueChange = viewModel::onNombreChange,
                label = { Text("Nombre del Negocio") },
                placeholder = { Text("Ej: Alquileres Raymi") },
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.error != null,
                supportingText = {
                    if (uiState.error != null) {
                        Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            )

            // Descripción
            OutlinedTextField(
                value = uiState.descripcion,
                onValueChange = viewModel::onDescripcionChange,
                label = { Text("Descripción (Opcional)") },
                placeholder = { Text("¿Qué alquilas principalmente?") },
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            // Selección de Rubro (Tipo de Negocio)
            Text(
                text = "Tipo de Negocio",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.align(Alignment.Start)
            )

            val tipos = listOf("VESTUARIOS", "EQUIPOS", "VEHICULOS", "HERRAMIENTAS", "OTRO")
            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = uiState.tipoNegocio,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Selecciona un Rubro") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    tipos.forEach { tipo ->
                        DropdownMenuItem(
                            text = { Text(tipo) },
                            onClick = {
                                viewModel.onTipoNegocioChange(tipo)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón de registro
            Button(
                onClick = { viewModel.registrarNegocio() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Crear Mi Negocio")
                }
            }
        }
    }
}
