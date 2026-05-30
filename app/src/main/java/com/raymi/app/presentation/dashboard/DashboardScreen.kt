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
import com.raymi.app.core.navigation.*
import com.raymi.app.core.lang.LocalRaymiStrings
import androidx.compose.ui.window.Dialog
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
    val strings = LocalRaymiStrings.current
    var showLanguageDialog by remember { mutableStateOf(false) }

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
                },
                actions = {
                    IconButton(onClick = { showLanguageDialog = true }) {
                        val currentLang = uiState.currentWorkspace?.idioma ?: "es"
                        Text(
                            text = if (currentLang == "es") "ES" else "EN",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.isLoading && uiState.estadisticas.totalClientes == 0) {
                RaymiLoadingIndicator(message = strings.loading)
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
                        labelMes = strings.monthlyEarnings,
                        labelTotal = strings.totalEarnings,
                        onExport = { viewModel.exportarResumenFinancieroPdf() }
                    )

                    // Sección de "Hoy" (SaaS Operativo)
                    TodayOperationsRow(
                        entregas = uiState.estadisticas.entregasHoy,
                        devoluciones = uiState.estadisticas.devolucionesHoy,
                        pagosPendientes = uiState.estadisticas.pagosPendientesCount,
                        labelEntregas = strings.todayDeliveries,
                        labelRetornos = strings.todayReturns,
                        labelCobros = strings.pendingPayments
                    )

                    if (uiState.actividadSemanal.isNotEmpty()) {
                        Text(strings.weeklyActivity, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        WeeklyActivityChart(uiState.actividadSemanal)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(strings.operationalStatus, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OperationsGrid(
                            inventario = uiState.estadisticas.totalItems,
                            alquilados = uiState.estadisticas.alquileresActivos,
                            clientes = uiState.estadisticas.totalClientes,
                            labelInventario = strings.inventory,
                            labelAlquilados = strings.rented,
                            labelClientes = strings.activeClients,
                            onInventoryClick = onNavigateToItems,
                            onRentalsClick = onNavigateToAlquileres,
                            onClientsClick = onNavigateToClientes
                        )
                    }

                    QuickManagementRow(
                        onNewRental = onNavigateToAlquileres,
                        onNewClient = onNavigateToClientes,
                        labelNewRental = strings.newRental,
                        labelNewClient = strings.newClient
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
                                Text(strings.adTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
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

    if (showLanguageDialog) {
        LanguageSelectorDialog(
            currentLang = uiState.currentWorkspace?.idioma ?: "es",
            onDismiss = { showLanguageDialog = false },
            onSelect = { lang ->
                viewModel.cambiarIdioma(lang)
                showLanguageDialog = false
            }
        )
    }
}

@Composable
fun TodayOperationsRow(
    entregas: Int, 
    devoluciones: Int, 
    pagosPendientes: Int,
    labelEntregas: String,
    labelRetornos: String,
    labelCobros: String
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TodayMetricChip(
            label = labelEntregas,
            count = entregas,
            color = Color(0xFF3B82F6),
            modifier = Modifier.weight(1f)
        )
        TodayMetricChip(
            label = labelRetornos,
            count = devoluciones,
            color = Color(0xFF10B981),
            modifier = Modifier.weight(1f)
        )
        TodayMetricChip(
            label = labelCobros,
            count = pagosPendientes,
            color = Color(0xFFEF4444),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun TodayMetricChip(label: String, count: Int, color: Color, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color.copy(alpha = 0.8f),
                maxLines = 1
            )
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
fun ExecutiveSummaryCard(
    ingresoMes: Double, 
    ingresoTotal: Double, 
    variacion: Double, 
    labelMes: String,
    labelTotal: String,
    onExport: () -> Unit
) {
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
                    Text(labelMes, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
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
                    "$labelTotal: S/. ${String.format(Locale.getDefault(), "%,.1f", ingresoTotal)}",
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
    labelInventario: String,
    labelAlquilados: String,
    labelClientes: String,
    onInventoryClick: () -> Unit,
    onRentalsClick: () -> Unit,
    onClientsClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ModernKpiCard(
                title = labelInventario,
                value = inventario.toString(),
                icon = Icons.Default.Inventory2,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                onClick = onInventoryClick
            )
            ModernKpiCard(
                title = labelAlquilados,
                value = alquilados.toString(),
                icon = Icons.Default.Key,
                color = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f),
                onClick = onRentalsClick
            )
        }
        ModernKpiCard(
            title = labelClientes,
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
fun QuickManagementRow(
    onNewRental: () -> Unit, 
    onNewClient: () -> Unit,
    labelNewRental: String,
    labelNewClient: String
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        QuickActionBtn(
            text = labelNewRental,
            icon = Icons.Default.Add,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1.2f),
            onClick = onNewRental
        )
        QuickActionBtn(
            text = labelNewClient,
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

@Composable
fun LanguageSelectorDialog(
    currentLang: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Seleccionar Idioma", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("Select Language", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                
                Spacer(Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LanguageOption(
                        label = "Español",
                        flag = "🇵🇪",
                        selected = currentLang == "es",
                        onClick = { onSelect("es") },
                        modifier = Modifier.weight(1f)
                    )
                    LanguageOption(
                        label = "English",
                        flag = "🇺🇸",
                        selected = currentLang == "en",
                        onClick = { onSelect("en") },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Cerrar / Close")
                }
            }
        }
    }
}

@Composable
fun LanguageOption(label: String, flag: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(flag, fontSize = 32.sp)
            Spacer(Modifier.height(8.dp))
            Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        }
    }
}
