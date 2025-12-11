package com.raymi.app.presentation.alquileres

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.raymi.app.core.theme.CustomShapes
import com.raymi.app.core.theme.RaymiColors
import com.raymi.app.domain.model.Alquiler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarPagoDialog(
    alquiler: Alquiler,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
    isLoading: Boolean = false
) {
    var montoPago by remember { mutableStateOf(alquiler.saldo.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    val nuevoAdelanto = alquiler.adelanto + (montoPago.toDoubleOrNull() ?: 0.0)
    val nuevoSaldo = alquiler.precioTotal - nuevoAdelanto

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Payments, contentDescription = null)
                Text("Registrar Pago")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Información del alquiler
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Cliente:")
                            Text(
                                alquiler.clienteNombre,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Precio Total:")
                            Text(
                                alquiler.precioFormateado,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pagado:")
                            Text(
                                alquiler.adelantoFormateado,
                                color = RaymiColors.Success
                            )
                        }
                        Divider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Saldo Actual:",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                alquiler.saldoFormateado,
                                color = RaymiColors.Error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Campo de monto a pagar
                OutlinedTextField(
                    value = montoPago,
                    onValueChange = {
                        montoPago = it
                        error = null

                        // Validar
                        val monto = it.toDoubleOrNull()
                        if (monto != null) {
                            when {
                                monto <= 0 -> error = "El monto debe ser mayor a 0"
                                monto > alquiler.saldo -> error = "El monto no puede ser mayor al saldo"
                            }
                        }
                    },
                    label = { Text("Monto a Pagar *") },
                    leadingIcon = {
                        Icon(Icons.Default.AttachMoney, contentDescription = null)
                    },
                    trailingIcon = {
                        // Botón para pagar todo
                        TextButton(onClick = {
                            montoPago = alquiler.saldo.toString()
                            error = null
                        }) {
                            Text("TODO", fontSize = MaterialTheme.typography.labelSmall.fontSize)
                        }
                    },
                    isError = error != null,
                    supportingText = {
                        error?.let { Text(it) }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                // Mostrar cálculo del nuevo saldo
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (nuevoSaldo <= 0) {
                            RaymiColors.Success.copy(alpha = 0.1f)
                        } else {
                            RaymiColors.Warning.copy(alpha = 0.1f)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Después del pago:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Nuevo adelanto:")
                            Text(
                                "S/. ${String.format("%.2f", nuevoAdelanto)}",
                                color = RaymiColors.Success,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Nuevo saldo:")
                            Text(
                                "S/. ${String.format("%.2f", nuevoSaldo)}",
                                color = if (nuevoSaldo <= 0) RaymiColors.Success else RaymiColors.Warning,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (nuevoSaldo <= 0) {
                            Divider()
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = RaymiColors.Success,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "¡Pago completo! Podrás devolver el vestuario.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RaymiColors.Success
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val monto = montoPago.toDoubleOrNull()
                    if (monto != null && monto > 0 && monto <= alquiler.saldo) {
                        onConfirm(monto)
                    } else {
                        error = "Monto inválido"
                    }
                },
                enabled = !isLoading && error == null
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Registrar Pago")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancelar")
            }
        },
        shape = CustomShapes.DialogShape
    )
}