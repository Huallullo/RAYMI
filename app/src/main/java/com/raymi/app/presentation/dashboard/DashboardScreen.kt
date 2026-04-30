package com.raymi.app.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.raymi.app.R
import com.raymi.app.core.theme.RaymiColors
import com.raymi.app.presentation.components.RaymiErrorState
import com.raymi.app.presentation.components.RaymiLoadingIndicator
import com.raymi.app.presentation.components.StatCard
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToClientes: () -> Unit,
    onNavigateToVestuarios: () -> Unit,
    onNavigateToAlquileres: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    val months = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    //val years = (currentYear - 3..currentYear + 1).toList()
    val years = remember(uiState.selectedYear, currentYear) {
        val minYear = uiState.selectedYear - 3
        val maxYear = minOf(currentYear + 1, uiState.selectedYear + 1)
        (minYear..maxYear).toList().reversed()
    }

    var monthExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_raymi_logo),
                            contentDescription = "Logo RAYMI",
                            modifier = Modifier.size(28.dp),
                            tint = Color.Unspecified
                        )

                        Column {
                            Text(
                                text = "Inicio",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Panel de control",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LaunchedEffect(uiState.error, uiState.successMessage) {
            uiState.error?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.clearMessages()
            }
            uiState.successMessage?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.clearMessages()
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    RaymiLoadingIndicator(message = "Cargando dashboard...")
                }

                uiState.error != null -> {
                    RaymiErrorState(
                        message = uiState.error ?: "Error desconocido",
                        onRetry = { viewModel.loadDashboardData() }
                    )
                }

                else -> {
                    val totalVestuarios = uiState.estadisticas.totalVestuarios.coerceAtLeast(1)
                    val activos = uiState.estadisticas.alquileresActivos
                    val disponibles = uiState.estadisticas.vestuariosDisponibles
                    val vencidos = uiState.estadisticas.alquileresVencidos
                    val ingresosMes = uiState.estadisticas.ingresosMes
                    val ingresosTotales = uiState.estadisticas.ingresosTotales

                    val ocupacionPct = (activos * 100f / totalVestuarios).coerceIn(0f, 100f)
                    val disponibilidadPct = (disponibles * 100f / totalVestuarios).coerceIn(0f, 100f)
                    val moraPct = if (activos > 0) (vencidos * 100f / activos).coerceIn(0f, 100f) else 0f

                    val now = Calendar.getInstance()
                    val dayOfMonth = now.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
                    val daysInMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
                    val monthProgress = (dayOfMonth.toFloat() / daysInMonth.toFloat()).coerceIn(0f, 1f)

                    val promedioPorAlquilerActivo = if (activos > 0) ingresosMes / activos else 0.0
                    val proyeccionMes = (ingresosMes / dayOfMonth) * daysInMonth

                    val variacion = uiState.variacionMensualPct
                    val variacionPositiva = variacion >= 0.0
                    val variacionSigno = if (variacionPositiva) "+" else ""

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Periodo de análisis",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ExposedDropdownMenuBox(
                                expanded = monthExpanded,
                                onExpandedChange = { monthExpanded = !monthExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = months[uiState.selectedMonth],
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Mes") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded)
                                    },
                                    modifier = Modifier
                                        .menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true)
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = monthExpanded,
                                    onDismissRequest = { monthExpanded = false }
                                ) {
                                    months.forEachIndexed { index, name ->
                                        DropdownMenuItem(
                                            text = { Text(name) },
                                            onClick = {
                                                viewModel.onMonthSelected(index)
                                                monthExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            ExposedDropdownMenuBox(
                                expanded = yearExpanded,
                                onExpandedChange = { yearExpanded = !yearExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = uiState.selectedYear.toString(),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Año") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded)
                                    },
                                    modifier = Modifier
                                        .menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true)
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = yearExpanded,
                                    onDismissRequest = { yearExpanded = false }
                                ) {
                                    years.forEach { year ->
                                        DropdownMenuItem(
                                            text = { Text(year.toString()) },
                                            onClick = {
                                                viewModel.onYearSelected(year)
                                                yearExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                title = "Clientes",
                                value = "${uiState.estadisticas.totalClientes}",
                                icon = Icons.Filled.People,
                                iconTint = RaymiColors.Info,
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToClientes
                            )
                            StatCard(
                                title = "Vestuarios",
                                value = "${uiState.estadisticas.totalVestuarios}",
                                icon = Icons.Filled.Checkroom,
                                iconTint = RaymiColors.PurpleLight,
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToVestuarios
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                title = "Disponibles",
                                value = "${uiState.estadisticas.vestuariosDisponibles}",
                                icon = Icons.Filled.CheckCircle,
                                iconTint = RaymiColors.Success,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Activos",
                                value = "${uiState.estadisticas.alquileresActivos}",
                                icon = Icons.Filled.ShoppingCart,
                                iconTint = RaymiColors.Warning,
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToAlquileres
                            )
                        }

                        if (uiState.estadisticas.alquileresVencidos > 0) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = RaymiColors.Error.copy(alpha = 0.1f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Filled.Warning, contentDescription = null, tint = RaymiColors.Error)
                                    Text(
                                        text = "Tienes ${uiState.estadisticas.alquileresVencidos} alquiler(es) vencidos.",
                                        color = RaymiColors.Error,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AttachMoney,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Resumen financiero",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = "Periodo: ${months[uiState.selectedMonth]} ${uiState.selectedYear}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Mes anterior: S/. ${String.format(Locale.getDefault(), "%.2f", uiState.ingresoMesAnterior)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$variacionSigno${String.format(Locale.getDefault(), "%.1f", variacion)}%",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (variacionPositiva) RaymiColors.Success else RaymiColors.Error
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(Modifier.padding(12.dp)) {
                                            Text("Ingreso periodo", style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                "S/. ${String.format(Locale.getDefault(), "%.2f", ingresosMes)}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(Modifier.padding(12.dp)) {
                                            Text("Acumulado", style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                "S/. ${String.format(Locale.getDefault(), "%.2f", ingresosTotales)}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(Modifier.padding(12.dp)) {
                                            Text("Promedio por activo", style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                "S/. ${String.format(Locale.getDefault(), "%.2f", promedioPorAlquilerActivo)}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(Modifier.padding(12.dp)) {
                                            Text("Proyección mensual", style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                "S/. ${String.format(Locale.getDefault(), "%.2f", proyeccionMes)}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "Avance del mes: $dayOfMonth / $daysInMonth días",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                LinearProgressIndicator(
                                    progress = { monthProgress },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = { viewModel.exportarResumenFinancieroPdf() },
                                    enabled = !uiState.isExportingPdf,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        if (uiState.isExportingPdf) {
                                            "Generando PDF..."
                                        } else {
                                            "Exportar resumen en PDF"
                                        }
                                    )
                                }
                                Button(
                                    onClick = { viewModel.compartirResumenFinancieroPorWhatsApp() },
                                    enabled = !uiState.isExportingPdf && uiState.pdfResumenUri != null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Compartir PDF por WhatsApp")
                                }
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Estado operativo",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Text("Ocupación: ${String.format(Locale.getDefault(), "%.0f", ocupacionPct)}%")
                                LinearProgressIndicator(
                                    progress = { ocupacionPct / 100f },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Text("Disponibilidad: ${String.format(Locale.getDefault(), "%.0f", disponibilidadPct)}%")
                                LinearProgressIndicator(
                                    progress = { disponibilidadPct / 100f },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Text("Riesgo de mora: ${String.format(Locale.getDefault(), "%.0f", moraPct)}%")
                                LinearProgressIndicator(
                                    progress = { moraPct / 100f },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = if (moraPct >= 30f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Text(
                            text = "Acciones rápidas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onNavigateToClientes,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.PersonAdd, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Nuevo Cliente")
                            }

                            Button(
                                onClick = onNavigateToAlquileres,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Nuevo Alquiler")
                            }
                        }


                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
