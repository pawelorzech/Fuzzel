package com.fizzy.android.feature.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fizzy.android.core.ui.components.SmallLoadingIndicator
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is AuthEvent.AuthSuccess -> onAuthSuccess()
                is AuthEvent.ShowError -> { /* Error shown in UI state */ }
            }
        }
    }

    BackHandler(enabled = uiState.step != AuthStep.INSTANCE_SELECTION) {
        viewModel.goBack()
    }

    Scaffold(
        topBar = {
            if (uiState.step != AuthStep.INSTANCE_SELECTION) {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.goBack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Logo
            Icon(
                imageVector = Icons.Default.ViewKanban,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Fizzy",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Kanban boards, simplified",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            AnimatedContent(
                targetState = uiState.step,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "auth_step"
            ) { step ->
                when (step) {
                    AuthStep.INSTANCE_SELECTION -> InstanceSelectionContent(
                        instanceUrl = uiState.instanceUrl,
                        onInstanceUrlChange = viewModel::onInstanceUrlChange,
                        onUseOfficial = viewModel::useOfficialInstance,
                        onContinue = viewModel::onContinueWithInstance,
                        error = uiState.error
                    )
                    AuthStep.EMAIL_INPUT -> EmailInputContent(
                        email = uiState.email,
                        onEmailChange = viewModel::onEmailChange,
                        onContinue = viewModel::requestMagicLink,
                        onToggleMethod = viewModel::toggleAuthMethod,
                        isLoading = uiState.isLoading,
                        error = uiState.error
                    )
                    AuthStep.CODE_VERIFICATION -> CodeVerificationContent(
                        email = uiState.email,
                        code = uiState.code,
                        onCodeChange = viewModel::onCodeChange,
                        onVerify = viewModel::verifyCode,
                        onResend = viewModel::requestMagicLink,
                        isLoading = uiState.isLoading,
                        error = uiState.error
                    )
                    AuthStep.PERSONAL_TOKEN -> PersonalTokenContent(
                        token = uiState.token,
                        onTokenChange = viewModel::onTokenChange,
                        onLogin = viewModel::loginWithToken,
                        onToggleMethod = viewModel::toggleAuthMethod,
                        isLoading = uiState.isLoading,
                        error = uiState.error
                    )
                }
            }
        }
    }
}

@Composable
private fun InstanceSelectionContent(
    instanceUrl: String,
    onInstanceUrlChange: (String) -> Unit,
    onUseOfficial: () -> Unit,
    onContinue: () -> Unit,
    error: String?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Choose your instance",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Connect to the official Fizzy service or your self-hosted instance",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onUseOfficial,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Cloud, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Use fizzy.com")
        }

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "Or connect to self-hosted",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = instanceUrl,
            onValueChange = onInstanceUrlChange,
            label = { Text("Instance URL") },
            placeholder = { Text("https://fizzy.example.com") },
            leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(onGo = { onContinue() }),
            isError = error != null
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            enabled = instanceUrl.isNotBlank()
        ) {
            Text("Continue")
        }
    }
}

@Composable
private fun EmailInputContent(
    email: String,
    onEmailChange: (String) -> Unit,
    onContinue: () -> Unit,
    onToggleMethod: () -> Unit,
    isLoading: Boolean,
    error: String?
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sign in with email",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "We'll send you a magic link to sign in",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email address") },
            placeholder = { Text("you@example.com") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(
                onGo = {
                    focusManager.clearFocus()
                    onContinue()
                }
            ),
            isError = error != null,
            enabled = !isLoading
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                focusManager.clearFocus()
                onContinue()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = email.isNotBlank() && !isLoading
        ) {
            if (isLoading) {
                SmallLoadingIndicator()
            } else {
                Text("Send Magic Link")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = onToggleMethod) {
            Text("Use Personal Access Token instead")
        }
    }
}

@Composable
private fun CodeVerificationContent(
    email: String,
    code: String,
    onCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    isLoading: Boolean,
    error: String?
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Enter your code",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "We sent a 6-character code to\n$email",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = code,
            onValueChange = onCodeChange,
            label = { Text("Verification code") },
            placeholder = { Text("XXXXXX") },
            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                textAlign = TextAlign.Center,
                letterSpacing = 8.sp
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(
                onGo = {
                    focusManager.clearFocus()
                    onVerify()
                }
            ),
            isError = error != null,
            enabled = !isLoading
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                focusManager.clearFocus()
                onVerify()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = code.length == 6 && !isLoading
        ) {
            if (isLoading) {
                SmallLoadingIndicator()
            } else {
                Text("Verify")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onResend, enabled = !isLoading) {
            Text("Resend code")
        }
    }
}

@Composable
private fun PersonalTokenContent(
    token: String,
    onTokenChange: (String) -> Unit,
    onLogin: () -> Unit,
    onToggleMethod: () -> Unit,
    isLoading: Boolean,
    error: String?
) {
    var showToken by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Personal Access Token",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter your Personal Access Token to sign in",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = token,
            onValueChange = onTokenChange,
            label = { Text("Access Token") },
            leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { showToken = !showToken }) {
                    Icon(
                        imageVector = if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showToken) "Hide token" else "Show token"
                    )
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(
                onGo = {
                    focusManager.clearFocus()
                    onLogin()
                }
            ),
            isError = error != null,
            enabled = !isLoading
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                focusManager.clearFocus()
                onLogin()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = token.isNotBlank() && !isLoading
        ) {
            if (isLoading) {
                SmallLoadingIndicator()
            } else {
                Text("Sign In")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = onToggleMethod) {
            Text("Use Magic Link instead")
        }
    }
}
