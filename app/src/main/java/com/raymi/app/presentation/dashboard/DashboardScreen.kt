package com.raymi.app.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.raymi.app.core.ads.AdManager
import com.raymi.app.presentation.components.*
import java.util.Locale

/**
 * Dashboard Ejecutivo Premium v2.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToClientes: () -> Unit,
    onNavigateToItems: () -> Unit,
    onNavigateToAlquileres: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            uiState.currentWorkspace?.nombre?.uppercase() ?: "RAYMI",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        val planNombre = uiState.currentPlan?.plan?.name ?: "BÁSICO"
                        Text(
                            "Plan $planNombre",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.isLoading && uiState.estadisticas.totalClientes == 0) {
                RaymiLoadingIndicator(message = "Analizando negocio...")
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    ExecutiveSummaryCard(
                        ingresoMes = uiState.estadisticas.ingresosMes,
                        ingresoTotal = uiState.estadisticas.ingresosTotales,
                        variacion = uiState.variacionMensualPct,
                        onExport = { viewModel.exportarResumenFinancieroPdf() }
                    )

                    if (uiState.actividadSemanal.isNotEmpty()) {
                        Text("Actividad últimos 7 días", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        WeeklyActivityChart(uiState.actividadSemanal)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Estado Operativo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OperationsGrid(
                            inventario = uiState.estadisticas.totalItems,
                            alquilados = uiState.estadisticas.alquileresActivos,
                            clientes = uiState.estadisticas.totalClientes,
                            onInventoryClick = onNavigateToItems,
                            onRentalsClick = onNavigateToAlquileres,
                            onClientsClick = onNavigateToClientes
                        )
                    }

                    QuickManagementRow(
                        onNewRental = onNavigateToAlquileres,
                        onNewClient = onNavigateToClientes
                    )

                    if (AdManager.debeMostrarAnuncios(uiState.currentPlan)) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Publicidad", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Spacer(Modifier.height(4.dp))
                                AdBanner()
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
fun WeeklyActivityChart(actividad: Map<String, Int>) {
    val maxVal = actividad.values.maxOrNull()?.coerceAtLeast(1) ?: 1
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth().height(120.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            actividad.forEach { (dia, cantidad) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val barHeight = (cantidad.toFloat() / maxVal.toFloat()) * 80
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(barHeight.dp)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(if (cantidad > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(dia, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ExecutiveSummaryCard(ingresoMes: Double, ingresoTotal: Double, variacion: Double, onExport: () -> Unit) {
    val isPositive = variacion >= 0
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text("Ingresos del Mes", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
                    Text(
                        "S/. ${String.format(Locale.getDefault(), "%,.2f", ingresoMes)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
                IconButton(onClick = onExport) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White)
                }
            }
            
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isPositive) Color(0xFF4ADE80) else Color(0xFFF87171)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${if (isPositive) "+" else ""}${String.format(Locale.getDefault(), "%.1f", variacion)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "Total histórico: S/. ${String.format(Locale.getDefault(), "%,.1f", ingresoTotal)}",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun OperationsGrid(
    inventario: Int, 
    alquilados: Int, 
    clientes: Int,
    onInventoryClick: () -> Unit,
    onRentalsClick: () -> Unit,
    onClientsClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ModernKpiCard(
                title = "Inventario",
                value = inventario.toString(),
                icon = Icons.Default.Inventory2,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                onClick = onInventoryClick
            )
            ModernKpiCard(
                title = "Alquilados",
                value = alquilados.toString(),
                icon = Icons.Default.Key,
                color = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f),
                onClick = onRentalsClick
            )
        }
        ModernKpiCard(
            title = "Clientes Activos",
            value = clientes.toString(),
            icon = Icons.Default.People,
            color = Color(0xFF8B5CF6),
            modifier = Modifier.fillMaxWidth(),
            onClick = onClientsClick
        )
    }
}

@Composable
fun ModernKpiCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(110.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Column {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun QuickManagementRow(onNewRental: () -> Unit, onNewClient: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        QuickActionBtn(
            text = "Nuevo Alquiler",
            icon = Icons.Default.Add,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1.2f),
            onClick = onNewRental
        )
        QuickActionBtn(
            text = "Nuevo Cliente",
            icon = Icons.Default.PersonAdd,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f),
            onClick = onNewClient
        )
    }
}

@Composable
fun QuickActionBtn(text: String, icon: ImageVector, containerColor: Color, contentColor: Color, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        shape = MaterialTheme.shapes.large,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
