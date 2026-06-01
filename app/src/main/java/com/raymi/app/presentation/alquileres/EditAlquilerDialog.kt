package com.raymi.app.presentation.alquileres

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.raymi.app.core.theme.CustomShapes
import com.raymi.app.core.utils.formatTo
import com.raymi.app.domain.model.Alquiler
import com.raymi.app.core.lang.LocalRaymiStrings
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAlquilerDialog(
    alquiler: Alquiler,
    onDismiss: () -> Unit,
    onConfirm: (Alquiler) -> Unit,
    isLoading: Boolean = false
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val strings = LocalRaymiStrings.current
    val isSpanish = strings is com.raymi.app.core.lang.SpanishStrings

    var cantidad by remember { mutableStateOf(alquiler.cantidad.toString()) }
    var fechaFin by remember { mutableStateOf(alquiler.fechaFinPrevista.toDate()) }
    var adelanto by remember { mutableStateOf(alquiler.adelanto.toString()) }
    var observaciones by remember { mutableStateOf(alquiler.observaciones) }

    // Calcular totales
    val precioTotal = alquiler.precioUnitario * (cantidad.toIntOrNull() ?: 1)
    val adelantoDouble = adelanto.toDoubleOrNull() ?: 0.0
    val saldo = precioTotal - adelantoDouble

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(strings.editItem, fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info del cliente y producto
                Column {
                    Text("${strings.selectClient}: ${alquiler.clienteNombre}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("${strings.selectItem}: ${alquiler.itemNombre}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Cantidad
                OutlinedTextField(
                    value = cantidad,
                    onValueChange = { if (it.isEmpty() || it.toIntOrNull() != null) cantidad = it },
                    label = { Text(strings.stock) },
                    leadingIcon = { Icon(Icons.Filled.ShoppingCart, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    enabled = !isLoading
                )

                // Fecha
                OutlinedTextField(
                    value = fechaFin.formatTo("dd/MM/yyyy"),
                    onValueChange = {},
                    label = { Text(strings.endDate) },
                    leadingIcon = { Icon(Icons.Filled.Event, null) },
                    trailingIcon = {
                        IconButton(onClick = {
                            val calendar = Calendar.getInstance(if (isSpanish) Locale("es", "PE") else Locale.US)
                            calendar.time = fechaFin
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    calendar.set(year, month, day)
                                    fechaFin = calendar.time
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }) { Icon(Icons.Filled.EditCalendar, null, tint = MaterialTheme.colorScheme.primary) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    shape = MaterialTheme.shapes.large,
                    enabled = !isLoading
                )

                // Adelanto
                OutlinedTextField(
                    value = adelanto,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) adelanto = it },
                    label = { Text(strings.advance) },
                    leadingIcon = { Icon(Icons.Filled.Payments, null) },
                    prefix = { Text("S/. ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    enabled = !isLoading
                )

                // Recuadro de Totales (Diseño Mejorado)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.large,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(strings.totalRental + ":", style = MaterialTheme.typography.bodyMedium)
                            Text("S/. ${String.format(Locale.US, "%.2f", precioTotal)}", fontWeight = FontWeight.Black)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(strings.balance + ":", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "S/. ${String.format(Locale.US, "%.2f", saldo)}",
                                fontWeight = FontWeight.Black,
                                color = if (saldo > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Observaciones
                OutlinedTextField(
                    value = observaciones,
                    onValueChange = { observaciones = it },
                    label = { Text(strings.notes) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, null) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = MaterialTheme.shapes.large,
                    enabled = !isLoading
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cantidadInt = cantidad.toIntOrNull() ?: 1
                    if (cantidadInt > 0) {
                        onConfirm(alquiler.copy(
                            cantidad = cantidadInt,
                            fechaFinPrevista = com.google.firebase.Timestamp(fechaFin),
                            precioTotal = precioTotal,
                            adelanto = adelantoDouble,
                            saldo = saldo,
                            observaciones = observaciones
                        ))
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = MaterialTheme.shapes.large,
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                else Text(strings.save, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text(strings.cancel) }
        },
        shape = CustomShapes.DialogShape
    )
}
