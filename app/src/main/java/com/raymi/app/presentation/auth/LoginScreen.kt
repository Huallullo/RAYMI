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
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    
    val strings = LocalRaymiStrings.current
    val isEnglish = strings is EnglishStrings

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
            val msg = when {
                it.contains("formato válido") || it.contains("invalid email") -> strings.errorInvalidEmail
                it.contains("incorrecta") || it.contains("wrong password") -> strings.errorWrongPassword
                it.contains("red") || it.contains("network") -> strings.errorNetwork
                it.contains("permiso") || it.contains("permission") -> strings.errorUnauthorized
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

                // Bot Protection (Solo en Registro)
                AnimatedVisibility(visible = uiState.isRegisterMode) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = strings.botChallenge,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val opLabel = when(uiState.botOp) {
                                "+" -> strings.botOpPlus
                                "-" -> strings.botOpMinus
                                else -> strings.botOpMult
                            }
                            val num1Word = strings.numberWords.getOrElse(uiState.botNum1) { uiState.botNum1.toString() }
                            val num2Word = strings.numberWords.getOrElse(uiState.botNum2) { uiState.botNum2.toString() }
                            
                            Text(
                                text = strings.solveChallenge.format(num1Word, opLabel, num2Word),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            OutlinedTextField(
                                value = uiState.botAnswer,
                                onValueChange = viewModel::onBotAnswerChange,
                                modifier = Modifier.width(85.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium,
                                isError = uiState.botAnswer.isNotEmpty() && !uiState.isBotVerified
                            )
                            IconButton(onClick = { viewModel.refreshBotChallenge() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(20.dp))
                            }
                            if (uiState.isBotVerified) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50))
                            }
                        }
                    }
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
