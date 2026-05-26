package com.raymi.app.presentation.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.painterResource
import com.raymi.app.R
import androidx.compose.ui.platform.testTag

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onNavigateToDashboard: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToWorkspaceSelection: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }

    // Gestión de navegación (Senior)
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                NavigationEvent.GoToDashboard -> onNavigateToDashboard()
                NavigationEvent.GoToOnboarding -> onNavigateToOnboarding()
                NavigationEvent.GoToWorkspaceSelection -> onNavigateToWorkspaceSelection()
            }
        }
    }

    // Gestión de foco en errores
    LaunchedEffect(uiState.emailError) {
        if (uiState.emailError != null) emailFocusRequester.requestFocus()
    }
    LaunchedEffect(uiState.passwordError) {
        if (uiState.passwordError != null && uiState.emailError == null) {
            passwordFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(uiState.error, uiState.infoMessage) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long,
                actionLabel = "Entendido"
            )
            viewModel.clearError()
        }
        uiState.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearInfoMessage()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier.padding(12.dp),
                    containerColor = if (uiState.error != null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (uiState.error != null) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    actionColor = MaterialTheme.colorScheme.primary,
                    snackbarData = data
                )
            }
        }
    ) { paddingValues ->
        // Diferenciación de fondo por modo (Senior UX)
        val backgroundColor by animateColorAsState(
            if (uiState.isRegisterMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.background,
            label = "BgColorAnimation"
        )

        Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
            // Fondo Decorativo Premium que cambia según el modo (Senior Differentiation)
            val gradientColors = if (uiState.isRegisterMode) {
                listOf(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f), Color.Transparent)
            } else {
                listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), Color.Transparent)
            }

            if (uiState.isRegisterMode) {
                // Decoración única para Registro (Círculos flotantes o formas)
                Box(
                    modifier = Modifier
                        .offset(x = (-50).dp, y = (-20).dp)
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f))
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (uiState.isRegisterMode) 400.dp else 300.dp)
                    .background(Brush.verticalGradient(gradientColors))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(50.dp))

                // Logo con animación sutil de escala
                val logoScale by animateFloatAsState(if (uiState.isLoading) 0.9f else 1f, label = "LogoScale")
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.scale(logoScale)
                ) {
                    // Cambiamos el icono sutilmente en registro para dar variedad visual
                    Icon(
                        painter = painterResource(id = R.drawable.ic_raymi_logo),
                        contentDescription = "Logo RAYMI",
                        modifier = Modifier.size(if (uiState.isRegisterMode) 100.dp else 90.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "RAYMI",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = if (uiState.isRegisterMode) 6.sp else 4.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Encabezado Dinámico
                AnimatedContent(
                    targetState = uiState.isRegisterMode,
                    transitionSpec = {
                        (fadeIn(tween(400)) + slideInVertically(tween(400), initialOffsetY = { it / 2 }))
                            .togetherWith(fadeOut(tween(400)))
                    },
                    label = "HeaderAnimation"
                ) { isRegister ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isRegister) "Crea tu Cuenta" else "Bienvenido de nuevo",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (isRegister) "Únete a la mejor gestión de alquileres" else "Accede a tu panel de control central",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Formulario
                AuthForm(
                    uiState = uiState,
                    viewModel = viewModel,
                    focusManager = focusManager,
                    emailFocusRequester = emailFocusRequester,
                    passwordFocusRequester = passwordFocusRequester
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Botón de Acción Principal (Estilo SaaS Premium)
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (uiState.isRegisterMode) viewModel.register() else viewModel.login()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("login_button"),
                    shape = MaterialTheme.shapes.extraLarge,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    enabled = !uiState.isLoading
                ) {
                    Text(
                        if (uiState.isRegisterMode) "COMENZAR AHORA" else "ENTRAR AL SISTEMA",
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Switch de modo con animación
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (uiState.isRegisterMode) "¿Ya tienes cuenta?" else "¿No tienes un negocio?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = viewModel::toggleAuthMode) {
                        Text(
                            if (uiState.isRegisterMode) "Inicia Sesión" else "Regístrate aquí",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (!uiState.isRegisterMode) {
                    TextButton(
                        onClick = viewModel::resetPassword,
                        modifier = Modifier.alpha(0.7f)
                    ) {
                        Text("Recuperar mi contraseña", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }

            // Pantalla de Carga (Overlay)
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f))
                        .clickable(enabled = false) { },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(strokeWidth = 3.dp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Sincronizando con la nube...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AuthForm(
    uiState: LoginUiState,
    viewModel: LoginViewModel,
    focusManager: androidx.compose.ui.focus.FocusManager,
    emailFocusRequester: FocusRequester,
    passwordFocusRequester: FocusRequester
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Campo Nombre del Negocio (Solo en Registro)
        AnimatedVisibility(visible = uiState.isRegisterMode) {
            OutlinedTextField(
                value = uiState.businessName,
                onValueChange = viewModel::onBusinessNameChange,
                label = { Text("Nombre de tu Negocio") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("business_name_input"),
                shape = MaterialTheme.shapes.large,
                leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                isError = uiState.businessNameError != null,
                supportingText = uiState.businessNameError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )
        }

        OutlinedTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text("Correo Electrónico") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(emailFocusRequester)
                .testTag("email_input"),
            shape = MaterialTheme.shapes.large,
            leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) },
            isError = uiState.emailError != null,
            supportingText = uiState.emailError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )

        var passwordVisible by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text("Contraseña") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(passwordFocusRequester)
                .testTag("password_input"),
            shape = MaterialTheme.shapes.large,
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                }
            },
            isError = uiState.passwordError != null,
            supportingText = uiState.passwordError?.let { { Text(it) } },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); if (uiState.isRegisterMode) viewModel.register() else viewModel.login() })
        )
    }
}