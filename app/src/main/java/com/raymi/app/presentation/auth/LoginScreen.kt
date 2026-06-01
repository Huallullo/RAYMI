package com.raymi.app.presentation.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.raymi.app.core.lang.EnglishStrings
import com.raymi.app.core.lang.LocalRaymiStrings
import com.raymi.app.core.lang.SpanishStrings
import androidx.compose.ui.window.Dialog
import android.speech.tts.TextToSpeech
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onNavigateToDashboard: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToWorkspaceSelection: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    
    val strings = LocalRaymiStrings.current
    val isEnglish = strings is EnglishStrings

    // Motor de Texto a Voz (TTS) para el CAPTCHA
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = if (isEnglish) Locale.US else Locale("es", "PE")
            }
        }
    }
    
    // Liberar recursos de voz al salir
    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    fun speakChallenge() {
        val opLabel = when(uiState.botOp) {
            "+" -> strings.botOpPlus
            "-" -> strings.botOpMinus
            else -> strings.botOpMult
        }
        val num1Word = strings.numberWords.getOrElse(uiState.botNum1) { uiState.botNum1.toString() }
        val num2Word = strings.numberWords.getOrElse(uiState.botNum2) { uiState.botNum2.toString() }
        val text = strings.solveChallenge.format(num1Word, opLabel, num2Word)
        
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    // Gestión de navegación
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
            val lowerError = it.lowercase()
            val msg = when {
                lowerError.contains("contraseña") || lowerError.contains("password") || lowerError.contains("incorrect") -> strings.errorWrongPassword
                lowerError.contains("formato") || lowerError.contains("email") -> strings.errorInvalidEmail
                lowerError.contains("red") || lowerError.contains("network") || lowerError.contains("conexión") -> strings.errorNetwork
                lowerError.contains("permiso") || lowerError.contains("permission") || lowerError.contains("denegado") -> strings.errorUnauthorized
                else -> it
            }
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Long,
                actionLabel = "OK"
            )
            viewModel.clearError()
        }
        uiState.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearInfoMessage()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {},
                actions = {
                    TextButton(onClick = { viewModel.setLanguage(if (isEnglish) "es" else "en") }) {
                        Text(
                            text = if (isEnglish) "ESPAÑOL 🇵🇪" else "ENGLISH 🇺🇸",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
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
        val backgroundColor by animateColorAsState(
            if (uiState.isRegisterMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.background,
            label = "BgColorAnimation"
        )

        Box(modifier = Modifier.fillMaxSize().background(backgroundColor).padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(30.dp))

                val logoScale by animateFloatAsState(if (uiState.isLoading) 0.9f else 1f, label = "LogoScale")
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.scale(logoScale)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_raymi_logo),
                        contentDescription = "Logo RAYMI",
                        modifier = Modifier.size(if (uiState.isRegisterMode) 100.dp else 90.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = strings.appName,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 4.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

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
                            text = if (isRegister) strings.registerTitle else strings.loginTitle,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (isRegister) strings.registerSubtitle else strings.loginSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                AuthFormStrings(
                    uiState = uiState,
                    viewModel = viewModel,
                    focusManager = focusManager,
                    emailFocusRequester = emailFocusRequester,
                    passwordFocusRequester = passwordFocusRequester,
                    strings = strings
                )

                // CAPTCHA WIDGET
                if (uiState.isRegisterMode) {
                    Spacer(modifier = Modifier.height(28.dp))
                    CaptchaWidget(
                        isVerified = uiState.isBotVerified,
                        strings = strings,
                        onClick = { viewModel.onRobotCheckboxClick() }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

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
                    enabled = !uiState.isLoading && (!uiState.isRegisterMode || uiState.isBotVerified)
                ) {
                    Text(
                        if (uiState.isRegisterMode) strings.registerButton else strings.loginButton,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (uiState.isRegisterMode) strings.hasAccount else strings.noAccount,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = viewModel::toggleAuthMode) {
                        Text(
                            if (uiState.isRegisterMode) strings.goToLogin else strings.goToRegister,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (!uiState.isRegisterMode) {
                    TextButton(
                        onClick = viewModel::resetPassword,
                        modifier = Modifier.alpha(0.7f)
                    ) {
                        Text(strings.forgotPassword, style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }

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
                            strings.syncCloud,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    // Modal del Desafío Matemático (Estilo reCAPTCHA Image Challenge)
    if (uiState.showBotMath && !uiState.isBotVerified) {
        Dialog(onDismissRequest = { /* Forzar resolución */ }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(2.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column {
                    // Cabecera Azul reCAPTCHA
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF4A90E2))
                            .padding(24.dp)
                    ) {
                        Column {
                            Text(
                                text = if (isEnglish) "Select the correct" else "Resuelve para continuar",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = strings.botChallenge.uppercase(),
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Contenido del Desafío
                    Column(modifier = Modifier.padding(24.dp)) {
                        val opLabel = when(uiState.botOp) {
                            "+" -> strings.botOpPlus
                            "-" -> strings.botOpMinus
                            else -> strings.botOpMult
                        }
                        val num1Word = strings.numberWords.getOrElse(uiState.botNum1) { uiState.botNum1.toString() }
                        val num2Word = strings.numberWords.getOrElse(uiState.botNum2) { uiState.botNum2.toString() }

                        Text(
                            text = strings.solveChallenge.format(num1Word, opLabel, num2Word),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 28.sp,
                            color = Color(0xFF333333)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedTextField(
                            value = uiState.botAnswer,
                            onValueChange = viewModel::onBotAnswerChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(2.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4A90E2),
                                cursorColor = Color(0xFF4A90E2)
                            )
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Barra de Herramientas reCAPTCHA - FIJA Y ESPACIADA
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Grupo de íconos (Izquierda)
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { viewModel.refreshBotChallenge() }) {
                                    Icon(Icons.Default.Refresh, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                                }
                                IconButton(onClick = { speakChallenge() }) {
                                    Icon(Icons.Default.Headset, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                                }
                                IconButton(onClick = { /* Info */ }) {
                                    Icon(Icons.Default.Info, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                                }
                            }

                            // Botón de acción (Derecha) - Con ancho mínimo para evitar deformación
                            Button(
                                onClick = { viewModel.verifyBotAnswer() },
                                modifier = Modifier
                                    .height(48.dp)
                                    .widthIn(min = 120.dp),
                                shape = RoundedCornerShape(2.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2)),
                                enabled = uiState.botAnswer.isNotEmpty(),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = if (isEnglish) "VERIFY" else "VERIFICAR",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                        color = Color.White
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CaptchaWidget(
    isVerified: Boolean,
    strings: com.raymi.app.core.lang.RaymiStrings,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(304.dp)
            .height(78.dp)
            .clickable(enabled = !isVerified) { onClick() },
        shape = RoundedCornerShape(3.dp),
        color = Color(0xFFF9F9F9),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD1D1D1)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox interactivo
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .border(
                        width = 2.dp,
                        color = if (isVerified) Color(0xFF4CAF50) else Color(0xFFC1C1C1),
                        shape = RoundedCornerShape(2.dp)
                    )
                    .background(if (isVerified) Color.Transparent else Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (isVerified) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = Color(0xFF4CAF50)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = strings.iamNotARobot,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = Color(0xFF333333),
                modifier = Modifier.weight(1f)
            )

            // Logo reCAPTCHA Style
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(start = 8.dp)) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_raymi_logo),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp).alpha(0.5f),
                    tint = Color.Unspecified
                )
                Text(
                    text = "reCAPTCHA",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                    color = Color.Gray
                )
                Text(
                    text = "Privacy - Terms",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun AuthFormStrings(
    uiState: LoginUiState,
    viewModel: LoginViewModel,
    focusManager: androidx.compose.ui.focus.FocusManager,
    emailFocusRequester: FocusRequester,
    passwordFocusRequester: FocusRequester,
    strings: com.raymi.app.core.lang.RaymiStrings
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AnimatedVisibility(visible = uiState.isRegisterMode) {
            OutlinedTextField(
                value = uiState.businessName,
                onValueChange = viewModel::onBusinessNameChange,
                label = { Text(strings.businessNameLabel) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                leadingIcon = { Icon(Icons.Default.Storefront, null) },
                isError = uiState.businessNameError != null,
                supportingText = uiState.businessNameError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )
        }

        OutlinedTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text(strings.emailLabel) },
            modifier = Modifier.fillMaxWidth().focusRequester(emailFocusRequester),
            shape = MaterialTheme.shapes.large,
            leadingIcon = { Icon(Icons.Default.Mail, null) },
            isError = uiState.emailError != null,
            supportingText = uiState.emailError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )

        var passwordVisible by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text(strings.passwordLabel) },
            modifier = Modifier.fillMaxWidth().focusRequester(passwordFocusRequester),
            shape = MaterialTheme.shapes.large,
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
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
