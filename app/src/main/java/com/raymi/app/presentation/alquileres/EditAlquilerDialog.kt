package com.raymi.app.presentation.alquileres

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.raymi.app.core.theme.CustomShapes
import com.raymi.app.core.utils.formatTo
import com.raymi.app.domain.model.Alquiler
import java.util.*

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
                Icon(Icons.Default.Edit, contentDescription = null)
                Text("Editar Alquiler")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Información no editable
                Text(
                    text = "Cliente: ${alquiler.clienteNombre}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Vestuario: ${alquiler.vestuarioNombre}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider()

                // Cantidad
                OutlinedTextField(
                    value = cantidad,
                    onValueChange = {
                        if (it.isEmpty() || it.toIntOrNull() != null) {
                            cantidad = it
                        }
                    },
                    label = { Text("Cantidad *") },
                    leadingIcon = {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                // Fecha de devolución prevista
                OutlinedTextField(
                    value = fechaFin.formatTo("dd/MM/yyyy"),
                    onValueChange = {},
                    label = { Text("Fecha de Devolución Prevista") },
                    leadingIcon = {
                        Icon(Icons.Default.Event, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            val calendar = Calendar.getInstance()
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
                        }) {
                            Icon(Icons.Default.EditCalendar, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    enabled = !isLoading
                )

                // Adelanto
                OutlinedTextField(
                    value = adelanto,
                    onValueChange = {
                        if (it.isEmpty() || it.toDoubleOrNull() != null) {
                            adelanto = it
                        }
                    },
                    label = { Text("Adelanto") },
                    leadingIcon = {
                        Icon(Icons.Default.Payments, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                // Mostrar cálculos
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Precio Total:")
                            Text(
                                "S/. ${String.format("%.2f", precioTotal)}",
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Saldo:")
                            Text(
                                "S/. ${String.format("%.2f", saldo)}",
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = if (saldo > 0) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Observaciones
                OutlinedTextField(
                    value = observaciones,
                    onValueChange = { observaciones = it },
                    label = { Text("Observaciones") },
                    leadingIcon = {
                        Icon(Icons.Default.Notes, contentDescription = null)
                    },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cantidadInt = cantidad.toIntOrNull() ?: 1
                    if (cantidadInt > 0) {
                        onConfirm(
                            alquiler.copy(
                                cantidad = cantidadInt,
                                fechaFinPrevista = com.google.firebase.Timestamp(fechaFin),
                                precioTotal = precioTotal,
                                adelanto = adelantoDouble,
                                saldo = saldo,
                                observaciones = observaciones
                            )
                        )
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Guardar Cambios")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancelar")
            }
        },
        shape = CustomShapes.DialogShape
    )
}