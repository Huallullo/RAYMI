package com.raymi.app.presentation.profile.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import com.raymi.app.core.lang.LocalRaymiStrings

/**
 * Manual de Usuario Detallado - RAYMI SaaS Master Guide.
 * Proporciona instrucciones paso a paso para el dominio total de la plataforma.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpCenterScreen(
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val strings = LocalRaymiStrings.current
    val isSpanish = strings is com.raymi.app.core.lang.SpanishStrings

    fun contactarSoporte() {
        val phoneNumber = "51988461129"
        val message = if (isSpanish) "Hola RAYMI, necesito soporte técnico con mi negocio." else "Hello RAYMI, I need technical support for my business."
        val url = "https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(strings.helpCenter, fontWeight = FontWeight.Black) },
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
                .padding(24.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                if (isSpanish) "Guía Maestra de RAYMI" else "RAYMI Master Guide",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                if (isSpanish) "Sigue estas instrucciones detalladas para maximizar la eficiencia de tu negocio." 
                else "Follow these detailed instructions to maximize your business efficiency.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            // 1. DASHBOARD
            HelpCategory(
                title = if (isSpanish) "1. Dashboard (Panel de Control)" else "1. Dashboard (Control Panel)",
                icon = Icons.Default.Dashboard,
                description = if (isSpanish) "Interpretación de métricas en tiempo real." else "Interpreting real-time metrics.",
                details = if (isSpanish) listOf(
                    "Actividad Semanal: Gráfico de barras que muestra el volumen de transacciones diarias. Ideal para identificar tus días de mayor demanda.",
                    "Entregas de Hoy: Lista prioritaria de clientes que deben recoger productos hoy. Pulsa el botón para ver el contrato rápido.",
                    "Retornos Esperados: Indica cuántos artículos deben reingresar al inventario hoy para evitar quiebres de stock.",
                    "Estado Operativo: (Inventario) Total de ítems registrados. (Alquilados) Ítems fuera de tienda. (Clientes) Base de datos activa.",
                    "Ingresos: El resumen financiero muestra 'Ingresos del Mes' (Efectivo real cobrado) vs 'Total Histórico'."
                ) else listOf(
                    "Weekly Activity: Bar chart showing daily transaction volume. Great for identifying peak demand days.",
                    "Today's Deliveries: Priority list of customers picking up items today. Click for quick contract view.",
                    "Expected Returns: Shows items due back today to prevent stock shortages.",
                    "Operational Status: (Inventory) Total registered items. (Rented) Items out of shop. (Clients) Active database.",
                    "Earnings: Financial summary shows 'Monthly Income' (Actual cash collected) vs 'Total Historical'."
                )
            )

            // 2. GESTIÓN DE CLIENTES
            HelpCategory(
                title = if (isSpanish) "2. Clientes y Validación" else "2. Customers & Validation",
                icon = Icons.Default.People,
                description = if (isSpanish) "Registro profesional con consulta RENIEC." else "Professional registration with ID lookup.",
                details = if (isSpanish) listOf(
                    "Búsqueda Inteligente: Filtra por Nombre o DNI desde la barra superior. El sistema busca mientras escribes.",
                    "Consulta RENIEC: En el formulario de 'Nuevo Cliente', ingresa los 8 dígitos del DNI y toca la LUPA. Los nombres se cargarán automáticamente.",
                    "Validación de Contacto: El número de teléfono debe tener 9 dígitos. Esto permite que el botón de WhatsApp funcione correctamente.",
                    "Ficha de Cliente: Al entrar a un cliente, verás su historial completo de alquileres pasados y deudas pendientes."
                ) else listOf(
                    "Smart Search: Filter by Name or ID from the top bar. Results appear as you type.",
                    "ID Lookup (Peru): In the 'New Client' form, enter the 8-digit ID and tap the MAGNIFYING GLASS to auto-fill names.",
                    "Contact Validation: Phone numbers must be 9 digits. This ensures the WhatsApp integration works perfectly.",
                    "Client File: View complete rental history and any outstanding balances for specific customers."
                )
            )

            // 3. INVENTARIO FLEXIBLE
            HelpCategory(
                title = if (isSpanish) "3. Inventario y Atributos" else "3. Inventory & Attributes",
                icon = Icons.Default.Inventory,
                description = if (isSpanish) "Configuración de productos y stock." else "Setting up products and stock.",
                details = if (isSpanish) listOf(
                    "Categorías: Debes crear al menos una categoría (ej: 'Vestidos') antes de agregar productos.",
                    "Código SKU/QR: Cada producto tiene un código único. Puedes usar el generador automático o escanear una etiqueta física.",
                    "Atributos Dinámicos: Si alquilas vestuarios, añade 'Talla' o 'Color'. Si alquilas equipos, añade 'Marca' o 'Serial'.",
                    "Control de Cantidad: Si tienes 10 unidades de un mismo código, el sistema controlará la disponibilidad automáticamente."
                ) else listOf(
                    "Categories: You must create at least one category (e.g., 'Dresses') before adding products.",
                    "SKU/QR Code: Each product has a unique code. Use the auto-generator or scan a physical label.",
                    "Dynamic Attributes: For clothing, add 'Size' or 'Color'. For equipment, add 'Brand' or 'Serial'.",
                    "Quantity Control: If you have 10 units of the same code, the system manages availability automatically."
                )
            )

            // 4. EL CICLO DE ALQUILER
            HelpCategory(
                title = if (isSpanish) "4. Ciclo de Operación" else "4. Operational Cycle",
                icon = Icons.Default.ShoppingCart,
                description = if (isSpanish) "Desde la reserva hasta la liquidación." else "From reservation to settlement.",
                details = if (isSpanish) listOf(
                    "Creación de Alquiler: 1. Selecciona Cliente. 2. Añade productos. 3. Define fechas. El sistema calcula días y total.",
                    "Adelanto y Saldo: Registra cuánto paga el cliente al inicio. El sistema calculará el 'Saldo Pendiente' automáticamente.",
                    "Garantía: Registra el monto de seguridad. Este monto no cuenta como ingreso, es reembolsable.",
                    "Estado Reserva vs Activo: Usa 'RESERVA' si el cliente aún no se lleva el producto. Cámbialo a 'ACTIVO' en la entrega."
                ) else listOf(
                    "Creating a Rental: 1. Select Client. 2. Add products. 3. Set dates. System calculates days and total.",
                    "Advance & Balance: Record initial payment. The system calculates 'Balance Due' automatically.",
                    "Security Deposit: Record the safety amount. This is not counted as income; it's refundable.",
                    "Reserved vs Active: Use 'RESERVED' if the item is still in shop. Switch to 'ACTIVE' upon delivery."
                )
            )

            // 5. COMPROBANTES Y PAGOS
            HelpCategory(
                title = if (isSpanish) "5. Pagos y Comprobantes" else "5. Payments & Receipts",
                icon = Icons.Default.Description,
                description = if (isSpanish) "Gestión financiera y facturación." else "Financial management and billing.",
                details = if (isSpanish) listOf(
                    "Registro de Abonos: Puedes recibir múltiples pagos (Yape, Efectivo, etc.) hasta que el saldo sea S/. 0.00.",
                    "Generación de PDF: Crea Tickets o Facturas profesionales con código QR de validación.",
                    "Compartir por WhatsApp: Al generar un PDF, toca 'Compartir' para enviarlo al instante sin guardar el archivo.",
                    "Penalidades: En la devolución, si hay retraso o daño, añade una 'Penalidad' para ajustar el cobro final."
                ) else listOf(
                    "Recording Payments: Accept multiple partial payments (Cash, App transfer, etc.) until balance is zero.",
                    "PDF Generation: Create professional Tickets or Invoices with QR validation codes.",
                    "WhatsApp Sharing: Once a PDF is generated, tap 'Share' to send it instantly without saving the file.",
                    "Penalties: Upon return, if there is a delay or damage, add a 'Penalty' to adjust the final charge."
                )
            )

            // 6. MULTI-NEGOCIO (PRO)
            HelpCategory(
                title = if (isSpanish) "6. Multi-Negocio (SaaS PRO)" else "6. Multi-Business (SaaS PRO)",
                icon = Icons.Default.BusinessCenter,
                description = if (isSpanish) "Gestión de múltiples sucursales." else "Managing multiple branches.",
                details = if (isSpanish) listOf(
                    "Cambiar Negocio: Accede desde Perfil -> Cambiar Negocio para ver tus locales.",
                    "Inventarios Independientes: Cada negocio tiene sus propios clientes, productos y finanzas.",
                    "Límites: El Plan FREE permite 1 negocio. El Plan PRO permite ilimitados locales de forma centralizada."
                ) else listOf(
                    "Switch Business: Access via Profile -> Switch Business to view your locations.",
                    "Independent Inventories: Each business has its own clients, products, and finances.",
                    "Limits: FREE Plan allows 1 business. PRO Plan allows unlimited locations centrally managed."
                )
            )

            // SOPORTE
            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SupportAgent, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(if (isSpanish) "¿Necesitas ayuda experta?" else "Need expert help?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (isSpanish) "Si tienes problemas con tus pagos PRO o necesitas una configuración personalizada, contacta a un agente." 
                        else "If you have issues with PRO payments or need custom configuration, contact an agent.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { contactarSoporte() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(if (isSpanish) "Hablar con un Humano" else "Talk to a Human")
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun HelpCategory(
    title: String,
    icon: ImageVector,
    description: String,
    details: List<String>
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (expanded) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = if (expanded) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null,
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon, 
                    contentDescription = null, 
                    tint = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.bodyLarge)
                    if (!expanded) {
                        Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, maxLines = 1)
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp, start = 8.dp)) {
                    Text(description, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(12.dp))
                    details.forEach { detail ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text("•", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                            Text(detail, style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp)
                        }
                    }
                }
            }
        }
    }
}
