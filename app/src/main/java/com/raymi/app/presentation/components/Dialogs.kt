package com.raymi.app.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.raymi.app.core.theme.CustomShapes

/**
 * Diálogo de confirmación genérico
 * Usado para confirmar acciones importantes
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = com.raymi.app.core.lang.LocalRaymiStrings.current.ok,
    dismissText: String = com.raymi.app.core.lang.LocalRaymiStrings.current.cancel,
    icon: ImageVector? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = icon?.let {
            { Icon(it, contentDescription = null) }
        },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        },
        shape = CustomShapes.DialogShape
    )
}

/**
 * Diálogo de confirmación de eliminación
 * Específico para acciones de eliminar
 */
@Composable
fun DeleteConfirmDialog(
    title: String = com.raymi.app.core.lang.LocalRaymiStrings.current.delete,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = com.raymi.app.core.lang.LocalRaymiStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(strings.delete)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        },
        shape = CustomShapes.DialogShape
    )
}

/**
 * Diálogo de advertencia
 * Para mostrar advertencias al usuario
 */
@Composable
fun WarningDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    val strings = com.raymi.app.core.lang.LocalRaymiStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(strings.understood)
            }
        },
        shape = CustomShapes.DialogShape
    )
}

/**
 * Diálogo de información
 * Para mostrar información al usuario
 */
@Composable
fun InfoDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    val strings = com.raymi.app.core.lang.LocalRaymiStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(strings.ok)
            }
        },
        shape = CustomShapes.DialogShape
    )
}
