package com.raymi.app.presentation.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.raymi.app.core.theme.RaymiColors

data class AtributoPersonalizado(
    val nombre: String,
    val etiqueta: String,
    val requerido: Boolean = false,
    val paraBusqueda: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    val uiState by viewModel.uiState.collectAsState()

    // Configuración temporal
    var rubro by remember { mutableStateOf("") }
    var tipoActivoSingular by remember { mutableStateOf("") }
    var tipoActivoPlural by remember { mutableStateOf("") }
    var atributos by remember { mutableStateOf(listOf<AtributoPersonalizado>()) }
    var nuevoAtributoNombre by remember { mutableStateOf("") }
    var nuevoAtributoEtiqueta by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configura tu negocio") },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Indicador de paso
            LinearProgressIndicator(
                progress = (currentStep + 1) / 4f,
                modifier = Modifier.fillMaxWidth(),
                color = RaymiColors.Success
            )

            when (currentStep) {
                0 -> RubroStep(
                    rubro = rubro,
                    onRubroChange = { rubro = it }
                )
                1 -> TipoActivoStep(
                    tipoActivoSingular = tipoActivoSingular,
                    onSingularChange = { tipoActivoSingular = it },
                    tipoActivoPlural = tipoActivoPlural,
                    onPluralChange = { tipoActivoPlural = it }
                )
                2 -> AtributosStep(
                    atributos = atributos,
                    nuevoNombre = nuevoAtributoNombre,
                    nuevoEtiqueta = nuevoAtributoEtiqueta,
                    onNombreChange = { nuevoAtributoNombre = it },
                    onEtiquetaChange = { nuevoAtributoEtiqueta = it },
                    onAgregar = {
                        if (nuevoAtributoNombre.isNotBlank() && nuevoAtributoEtiqueta.isNotBlank()) {
                            atributos = atributos + AtributoPersonalizado(
                                nombre = nuevoAtributoNombre,
                                etiqueta = nuevoAtributoEtiqueta,
                                requerido = true
                            )
                            nuevoAtributoNombre = ""
                            nuevoAtributoEtiqueta = ""
                        }
                    },
                    onEliminar = { atributo ->
                        atributos = atributos - atributo
                    }
                )
                3 -> ResumenStep(
                    rubro = rubro,
                    tipoActivoSingular = tipoActivoSingular,
                    tipoActivoPlural = tipoActivoPlural,
                    atributos = atributos,
                    isLoading = uiState.isLoading,
                    onConfirm = {
                        viewModel.guardarConfiguracion(
                            rubro = rubro,
                            tipoActivoSingular = tipoActivoSingular,
                            tipoActivoPlural = tipoActivoPlural,
                            atributos = atributos
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Botones siguiente/anterior
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentStep > 0) {
                    OutlinedButton(onClick = { currentStep-- }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Anterior")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        if (currentStep < 3) {
                            currentStep++
                        }
                    },
                    enabled = when (currentStep) {
                        0 -> rubro.isNotBlank()
                        1 -> tipoActivoSingular.isNotBlank() && tipoActivoPlural.isNotBlank()
                        2 -> true // atributos opcionales
                        3 -> true
                        else -> true
                    }
                ) {
                    Text(if (currentStep == 3) "Finalizar" else "Siguiente")
                    if (currentStep < 3) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }

    // Efecto para cerrar cuando se complete
    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            onComplete()
        }
    }
}

@Composable
fun RubroStep(
    rubro: String,
    onRubroChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Business,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "¿Qué vas a alquilar?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
        Text(
            text = "Define el rubro principal de tu negocio (ej: Herramientas, Vestuarios, Equipos de sonido)",
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedTextField(
            value = rubro,
            onValueChange = onRubroChange,
            label = { Text("Rubro / tipo de negocio") },
            placeholder = { Text("Ej: Herramientas") },
            leadingIcon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
fun TipoActivoStep(
    tipoActivoSingular: String,
    onSingularChange: (String) -> Unit,
    tipoActivoPlural: String,
    onPluralChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Category,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "¿Cómo llamarás a tus artículos?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
        OutlinedTextField(
            value = tipoActivoSingular,
            onValueChange = onSingularChange,
            label = { Text("Nombre singular") },
            placeholder = { Text("Ej: herramienta, vestuario, equipo") },
            leadingIcon = { Icon(Icons.Default.Label, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = tipoActivoPlural,
            onValueChange = onPluralChange,
            label = { Text("Nombre plural") },
            placeholder = { Text("Ej: herramientas, vestuarios, equipos") },
            leadingIcon = { Icon(Icons.Default.Label, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
fun AtributosStep(
    atributos: List<AtributoPersonalizado>,
    nuevoNombre: String,
    nuevoEtiqueta: String,
    onNombreChange: (String) -> Unit,
    onEtiquetaChange: (String) -> Unit,
    onAgregar: () -> Unit,
    onEliminar: (AtributoPersonalizado) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Atributos de tus artículos",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
        Text(
            text = "Agrega campos adicionales como marca, modelo, talla, etc.",
            style = MaterialTheme.typography.bodyMedium
        )

        // Lista de atributos actuales
        if (atributos.isNotEmpty()) {
            Text("Atributos actuales:", style = MaterialTheme.typography.titleSmall)
            atributos.forEach { attr ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(attr.etiqueta, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Text(attr.nombre, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { onEliminar(attr) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = RaymiColors.Error)
                        }
                    }
                }
            }
        }

        // Agregar nuevo atributo
        OutlinedTextField(
            value = nuevoNombre,
            onValueChange = onNombreChange,
            label = { Text("Nombre interno (ej: marca)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = nuevoEtiqueta,
            onValueChange = onEtiquetaChange,
            label = { Text("Etiqueta visible (ej: Marca)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Button(
            onClick = onAgregar,
            enabled = nuevoNombre.isNotBlank() && nuevoEtiqueta.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Agregar atributo")
        }
    }
}

@Composable
fun ResumenStep(
    rubro: String,
    tipoActivoSingular: String,
    tipoActivoPlural: String,
    atributos: List<AtributoPersonalizado>,
    isLoading: Boolean,
    onConfirm: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = RaymiColors.Success
        )
        Text(
            text = "¡Todo listo!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Resumen de tu negocio:", style = MaterialTheme.typography.titleMedium)
                Text("• Rubro: $rubro")
                Text("• Artículos: $tipoActivoSingular / $tipoActivoPlural")
                if (atributos.isNotEmpty()) {
                    Text("• Atributos personalizados:")
                    atributos.forEach { attr ->
                        Text("  - ${attr.etiqueta} (${attr.nombre})")
                    }
                } else {
                    Text("• Sin atributos personalizados")
                }
            }
        }

        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Ingresar a RAYMI")
            }
        }
    }
}