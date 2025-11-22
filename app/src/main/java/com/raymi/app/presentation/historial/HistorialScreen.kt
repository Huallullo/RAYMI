package com.raymi.app.presentation.historial

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.raymi.app.presentation.components.RaymiEmptyState

/**
 * Pantalla de historial de operaciones
 * Muestra un registro de todas las actividades del sistema
 *
 * NOTA: Esta es una versión simplificada de placeholder.
 * La versión completa requeriría un ViewModel y lógica de historial.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Filtros */ }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filtrar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Estado vacío por ahora
            RaymiEmptyState(
                icon = Icons.Default.History,
                title = "Sin historial",
                description = "El historial de operaciones aparecerá aquí"
            )
        }
    }
}