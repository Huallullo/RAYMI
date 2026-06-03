package com.raymi.app.presentation.workspace

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raymi.app.domain.model.Workspace
import androidx.compose.ui.res.painterResource
import com.raymi.app.R
import androidx.compose.ui.platform.testTag
import com.raymi.app.core.lang.LocalRaymiStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceSelectionScreen(
    viewModel: WorkspaceSelectionViewModel = hiltViewModel(),
    onWorkspaceSelected: () -> Unit,
    onCreateWorkspace: () -> Unit,
    onNavigateToPlans: () -> Unit,
    onLogout: () -> Unit,
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalRaymiStrings.current

    // OPTIMIZACIÓN: Se quitó el double fetch de loadWorkspaces() que ya se ejecuta en el init del ViewModel
    LaunchedEffect(uiState.workspaceSelected) { if (uiState.workspaceSelected) onWorkspaceSelected() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(
                            onClick = onNavigateBack,
                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                        }
                    }
                },
                actions = { 
                    TextButton(onClick = { viewModel.logout { onLogout() } }) { 
                        Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "Cerrar Sesión" else "Logout") 
                    } 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Button(
                onClick = { viewModel.onCreateNewWorkspace(onCreateWorkspace) },
                modifier = Modifier.fillMaxWidth().padding(24.dp).height(56.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(strings.registerNewBusiness, fontWeight = FontWeight.Bold)
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(20.dp))
            Icon(painter = painterResource(id = R.drawable.ic_raymi_logo), contentDescription = "Logo", modifier = Modifier.size(70.dp), tint = Color.Unspecified)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "RAYMI", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.primary))
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = strings.welcome, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp))
            Text(text = strings.selectBusiness, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(strokeWidth = 3.dp) }
            } else if (uiState.workspaces.isEmpty()) {
                EmptyWorkspacesView(strings)
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
                    items(uiState.workspaces) { workspace ->
                        PremiumWorkspaceCard(workspace = workspace, onClick = { viewModel.selectWorkspace(workspace) })
                    }
                }
            }
        }
    }

    if (uiState.showLimitDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissLimitDialog() },
            title = { Text(strings.limitReachedTitle) },
            text = { Text(strings.limitReachedDesc) },
            confirmButton = { Button(onClick = { viewModel.dismissLimitDialog(); onNavigateToPlans() }) { Text(strings.viewProPlans) } },
            dismissButton = { TextButton(onClick = { viewModel.dismissLimitDialog() }) { Text(strings.cancel) } }
        )
    }
}

@Composable
fun PremiumWorkspaceCard(workspace: Workspace, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().testTag("workspace_card"), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Default.BusinessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = workspace.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = workspace.tipoNegocio, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun EmptyWorkspacesView(strings: com.raymi.app.core.lang.RaymiStrings) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Storefront, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(16.dp))
        Text(strings.noBusinessesYet, fontWeight = FontWeight.Bold)
        Text(strings.startManagingToday, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
