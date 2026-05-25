package com.raymi.app.presentation.alquileres

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.raymi.app.core.theme.CustomShapes
import com.raymi.app.core.utils.formatTo
import com.raymi.app.domain.model.Alquiler
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
                Icon(Icons.Filled.Edit, contentDescription = null)
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
                    text = "Ítem: ${alquiler.itemNombre}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

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
                        Icon(Icons.Filled.ShoppingCart, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                // Fecha de devolución prevista (con calendario en español)
                OutlinedTextField(
                    value = fechaFin.formatTo("dd/MM/yyyy"),
                    onValueChange = {},
                    label = { Text("Fecha de Devolución Prevista") },
                    leadingIcon = {
                        Icon(Icons.Filled.Event, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            // Forzar locale español
                            val calendar = Calendar.getInstance(Locale("es", "PE"))
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
                            ).apply {
                                // Opcional: forzar textos de botones
                                setButton(DatePickerDialog.BUTTON_POSITIVE, "Aceptar", this)
                                setButton(DatePickerDialog.BUTTON_NEGATIVE, "Cancelar", this)
                            }.show()
                        }) {
                            Icon(Icons.Filled.EditCalendar, contentDescription = null)
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
                        Icon(Icons.Filled.Payments, contentDescription = null)
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
                                "S/. ${String.format(Locale.getDefault(), "%.2f", precioTotal)}",
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Saldo:")
                            Text(
                                "S/. ${String.format(Locale.getDefault(), "%.2f", saldo)}",
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
                        Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null)
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