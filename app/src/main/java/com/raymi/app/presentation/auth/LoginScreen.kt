package com.raymi.app.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.raymi.app.R

/**
 * Pantalla de inicio de sesión
 * Permite al usuario autenticarse en la aplicación
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    // Navegar al dashboard cuando el login sea exitoso
    LaunchedEffect(uiState.isLoginSuccessful) {
        if (uiState.isLoginSuccessful) {
            onLoginSuccess()
        }
    }

    // Mostrar snackbar de error o informacion
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }
    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearInfoMessage()
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Logo y título
                Icon(
                    painter = painterResource(id = R.drawable.ic_raymi_logo),
                    contentDescription = "Logo RAYMI",
                    modifier = Modifier.size(110.dp),
                    tint = Color.Unspecified
                )

                Text(
                    text = "RAYMI",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = if (uiState.isRegisterMode) {
                        "Crea tu negocio de alquiler"
                    } else {
                        "Sistema de Gestión de Alquiler"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))
                if (uiState.isRegisterMode) {
                    OutlinedTextField(
                        value = uiState.businessName,
                        onValueChange = viewModel::onBusinessNameChange,
                        label = { Text("Nombre del negocio") },
                        isError = uiState.businessNameError != null,
                        supportingText = {
                            uiState.businessNameError?.let { error ->
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading
                    )
                }
                // Campo de email
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = viewModel::onEmailChange,
                    label = { Text("Email") },
                    leadingIcon = {
                        Icon(Icons.Filled.Email, contentDescription = null)
                    },
                    isError = uiState.emailError != null,
                    supportingText = {
                        uiState.emailError?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )

                // Campo de contraseña
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = { Text("Contraseña") },
                    leadingIcon = {
                        Icon(Icons.Filled.Lock, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = viewModel::togglePasswordVisibility) {
                            Icon(
                                imageVector = if (uiState.isPasswordVisible) {
                                    Icons.Filled.VisibilityOff
                                } else {
                                    Icons.Filled.Visibility
                                },
                                contentDescription = if (uiState.isPasswordVisible) {
                                    "Ocultar contraseña"
                                } else {
                                    "Mostrar contraseña"
                                }
                            )
                        }
                    },
                    visualTransformation = if (uiState.isPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    isError = uiState.passwordError != null,
                    supportingText = {
                        uiState.passwordError?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (uiState.isRegisterMode) {
                                viewModel.register()
                            } else {
                                viewModel.login()
                            }
                        }
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Botón principal de autentificacion
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (uiState.isRegisterMode) {
                            viewModel.register()
                        } else {
                            viewModel.login()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (uiState.isRegisterMode) "Crear cuenta" else "Iniciar Sesión",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = viewModel::toggleAuthMode,
                    enabled = !uiState.isLoading
                ) {
                    Text(
                        text = if (uiState.isRegisterMode) {
                            "Ya tengo cuenta"
                        } else {
                            "Crear cuenta para mi negocio"
                        }
                    )
                }

                if (!uiState.isRegisterMode) {
                    TextButton(
                        onClick = viewModel::resetPassword,
                        enabled = !uiState.isLoading
                    ) {
                        Text("Olvidé mi contraseña")
                    }
                }
                // Texto informativo
                Text(
                    text = if (uiState.isRegisterMode) {
                        "Cada cuenta crea un negocio aislado con su propio historial"
                    } else {
                        "Ingresa o crea una cuenta para administrar tu negocio"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
