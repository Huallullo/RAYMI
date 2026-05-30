package com.raymi.app.presentation.profile.edit

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.raymi.app.presentation.components.RaymiPhoneField
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raymi.app.core.lang.LocalRaymiStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val strings = LocalRaymiStrings.current

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onNavigateBack()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "Editar Perfil" else "Edit Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.nombre,
                onValueChange = viewModel::onNombreChange,
                label = { Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "Nombre" else "Full Name") },
                modifier = Modifier.fillMaxWidth()
            )

            RaymiPhoneField(
                phone = uiState.telefono,
                onPhoneChange = viewModel::onTelefonoChange,
                label = if (strings is com.raymi.app.core.lang.SpanishStrings) "Teléfono Personal" else "Personal Phone"
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = viewModel::saveProfile,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                else Text(strings.save)
            }
        }
    }
}
