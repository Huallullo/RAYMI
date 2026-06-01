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
 * Manual de Usuario Detallado - RAYMI v2.5.
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
                if (isSpanish) "Guía Maestra RAYMI v2.5" else "RAYMI Master Guide v2.5",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                if (isSpanish) "Sigue estas instrucciones detalladas para maximizar la eficiencia y seguridad de tu negocio." 
                else "Follow these detailed instructions to maximize your business efficiency and security.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            // 1. SEGURIDAD Y ACCESO (Identidad Verificada)
            HelpCategory(
                title = if (isSpanish) "1. Seguridad y Validación RENIEC" else "1. Security & RENIEC Validation",
                icon = Icons.Default.Security,
                description = if (isSpanish) "Protección y registro seguro de clientes." else "Protection and secure client registration.",
                details = if (isSpanish) listOf(
                    "Búsqueda por DNI: Al registrar un cliente, simplemente ingresa su número de DNI y toca el icono de la lupa. RAYMI se conectará con los servidores de RENIEC para obtener nombres y apellidos exactos automáticamente.",
                    "Soporte Offline: Si pierdes la conexión, verás un banner amarillo. Puedes seguir trabajando; RAYMI guardará los datos localmente y los subirá a la nube apenas recuperes internet.",
                    "Fotos de Respaldo: Recomendamos tomar fotos del DNI frontal y posterior. Estas se guardan de forma privada y encriptada en tu espacio de trabajo.",
                    "Aislamiento de Datos: Cada negocio (Workspace) es una caja fuerte independiente. Tus clientes y productos jamás se mezclarán con los de otros usuarios."
                ) else listOf(
                    "DNI Search: When registering a client, just enter their DNI and tap the magnifying glass. RAYMI connects to RENIEC servers to get exact names and surnames automatically.",
                    "Offline Support: If you lose connection, a yellow banner appears. You can keep working; RAYMI saves data locally and uploads to the cloud as soon as internet is restored.",
                    "Backup Photos: We recommend taking photos of the DNI (front and back). These are stored privately and encrypted in your workspace.",
                    "Data Isolation: Each business (Workspace) is an independent safe. Your clients and products will never mix with those of other users."
                )
            )

            // 2. DASHBOARD Y FINANZAS (Caja Real)
            HelpCategory(
                title = if (isSpanish) "2. Dashboard y Control de Caja" else "2. Dashboard & Cash Control",
                icon = Icons.Default.MonetizationOn,
                description = if (isSpanish) "Entiende tus números y el flujo de dinero." else "Understand your numbers and money flow.",
                details = if (isSpanish) listOf(
                    "Ingresos del Mes: Este número refleja el dinero que ha entrado físicamente a tu caja HOY o en este mes, sin importar cuándo empiece el alquiler.",
                    "Por Cobrar (Saldo Pendiente): Es la suma de todo el dinero que tus clientes aún te deben por alquileres activos. Aparece resaltado a la derecha en tu tarjeta principal.",
                    "Auto-Auditoría: Si ves que tus números no cuadran (ej: dice S/ 0.00 pero tienes alquileres), desliza la pantalla hacia abajo para 'Refrescar'. RAYMI hará un recuento automático de todos tus recibos y corregirá los totales.",
                    "Estado Operativo: Conteo rápido de ítems en almacén (Inventario) vs ítems que están con clientes (Alquilados)."
                ) else listOf(
                    "Monthly Income: This number reflects the money that physically entered your cash box TODAY or this month, regardless of when the rental starts.",
                    "To Collect (Balance): Sum of all money clients still owe you for active rentals. It appears highlighted on the right of your main card.",
                    "Auto-Audit: If numbers don't match (e.g., shows $0.00 but you have rentals), swipe down to 'Refresh'. RAYMI will recount all your receipts and fix the totals.",
                    "Operational Status: Quick count of items in stock (Inventory) vs items with clients (Rented)."
                )
            )

            // 3. GESTIÓN DE ALQUILERES (Precios y Devoluciones)
            HelpCategory(
                title = if (isSpanish) "3. Alquileres, Precios y Penalidades" else "3. Rentals, Pricing & Penalties",
                icon = Icons.Default.Receipt,
                description = if (isSpanish) "Lógica de cobros, devoluciones y mora." else "Pricing, returns, and penalty logic.",
                details = if (isSpanish) listOf(
                    "Lógica de Precio: El precio se calcula por CANTIDAD de prendas (Precio x Unidades). No multiplicamos por días a menos que tú lo ajustes manualmente, para darte flexibilidad.",
                    "Devoluciones Parciales: Si un cliente lleva 25 prendas y solo devuelve 10, puedes registrarlo. El alquiler seguirá 'ACTIVO' y el stock de las 10 prendas regresará al inventario automáticamente.",
                    "Penalidad Automática: Al devolver, si el cliente llega tarde, RAYMI calculará una sugerencia de mora del 3% diario. Verás el monto en azul para que decidas si cobrarlo o no.",
                    "Ticket VIP WhatsApp: Al terminar, puedes reenviar un recibo profesional por WhatsApp con un solo toque, incluyendo el link de Google Maps de tu local."
                ) else listOf(
                    "Pricing Logic: Price is calculated by QUANTITY of items (Price x Units). We don't multiply by days unless you manually adjust it, giving you flexibility.",
                    "Partial Returns: If a client takes 25 items and only returns 10, you can register it. The rental stays 'ACTIVE' and the stock for those 10 items returns to inventory automatically.",
                    "Automatic Penalty: Upon return, if the client is late, RAYMI calculates a 3% daily late fee suggestion. You'll see the amount in blue to decide whether to charge it.",
                    "WhatsApp VIP Ticket: Once finished, you can resend a professional receipt via WhatsApp with one tap, including your business Google Maps link."
                )
            )

            // 4. INVENTARIO E INTELIGENCIA (Categorías)
            HelpCategory(
                title = if (isSpanish) "4. Inventario y Categorización" else "4. Inventory & Categorization",
                icon = Icons.Default.Inventory,
                description = if (isSpanish) "Organiza tus activos por tipo y marca." else "Organize your assets by type and brand.",
                details = if (isSpanish) listOf(
                    "Importancia de Categorías: Antes de crear productos, crea categorías (ej: Vestidos, Ternos). Esto activa los filtros rápidos en el inventario.",
                    "Stock Inteligente: Verás una barra que dice '15 / 25 und.'. El primer número es lo disponible para alquilar hoy, el segundo es tu inversión total.",
                    "QR del Ítem: Cada producto genera un código único. Puedes imprimirlo o mostrarlo desde el celular para que el cliente sepa que está llevando el ítem correcto.",
                    "Optimización de Fotos: RAYMI reduce el peso de tus fotos automáticamente al subirlas, consumiendo menos de tus datos móviles y cargando más rápido."
                ) else listOf(
                    "Importance of Categories: Before creating products, create categories (e.g., Dresses, Suits). This activates quick filters in the inventory.",
                    "Smart Stock: You'll see a bar saying '15 / 25 units'. The first number is available to rent today, the second is your total investment.",
                    "Item QR: Each product generates a unique code. You can print it or show it from your phone so the client knows they are taking the correct item.",
                    "Photo Optimization: RAYMI automatically reduces the weight of your photos upon upload, using less of your mobile data and loading faster."
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
                    Text(title, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    if (!expanded) {
                        Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
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
