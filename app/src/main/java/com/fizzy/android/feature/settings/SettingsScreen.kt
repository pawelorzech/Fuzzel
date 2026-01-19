package com.fizzy.android.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fizzy.android.data.local.ThemeMode
import com.fizzy.android.domain.model.Account
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                SettingsEvent.Logout -> onLogout()
                SettingsEvent.AccountSwitched -> { /* Stay on screen */ }
                SettingsEvent.NavigateToAddAccount -> {
                    // Would navigate to auth screen for adding new account
                    // For simplicity, just log out and let user re-auth
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Current Account Section
            SettingsSectionHeader("Account")

            uiState.currentAccount?.let { account ->
                AccountItem(
                    account = account,
                    isActive = true,
                    showSwitcher = uiState.allAccounts.size > 1,
                    onSwitcherClick = viewModel::showAccountSwitcher,
                    onLogoutClick = { viewModel.showLogoutConfirmation(account) }
                )
            }

            // Multiple Accounts Management
            if (uiState.allAccounts.size > 1) {
                ListItem(
                    headlineContent = { Text("Switch Account") },
                    leadingContent = {
                        Icon(Icons.Default.SwitchAccount, contentDescription = null)
                    },
                    trailingContent = {
                        Text(
                            "${uiState.allAccounts.size} accounts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    },
                    modifier = Modifier.clickable { viewModel.showAccountSwitcher() }
                )
            }

            ListItem(
                headlineContent = { Text("Add Account") },
                leadingContent = {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                },
                modifier = Modifier.clickable { viewModel.navigateToAddAccount() }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Appearance Section
            SettingsSectionHeader("Appearance")

            ThemeSelector(
                currentTheme = uiState.themeMode,
                onThemeSelect = viewModel::setThemeMode
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // About Section
            SettingsSectionHeader("About")

            ListItem(
                headlineContent = { Text("Version") },
                supportingContent = { Text("1.0.0") },
                leadingContent = {
                    Icon(Icons.Default.Info, contentDescription = null)
                }
            )

            ListItem(
                headlineContent = { Text("Open Source Licenses") },
                leadingContent = {
                    Icon(Icons.Default.Description, contentDescription = null)
                },
                modifier = Modifier.clickable { /* Open licenses */ }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Danger Zone
            SettingsSectionHeader("Danger Zone", isDestructive = true)

            ListItem(
                headlineContent = {
                    Text(
                        "Log out from all accounts",
                        color = MaterialTheme.colorScheme.error
                    )
                },
                leadingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                modifier = Modifier.clickable { viewModel.showLogoutAllConfirmation() }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Account Switcher Dialog
    if (uiState.showAccountSwitcher) {
        AccountSwitcherDialog(
            accounts = uiState.allAccounts,
            currentAccountId = uiState.currentAccount?.id,
            onAccountSelect = viewModel::switchAccount,
            onAddAccount = viewModel::showAddAccount,
            onDismiss = viewModel::hideAccountSwitcher
        )
    }

    // Logout Confirmation Dialog
    uiState.showLogoutConfirmation?.let { account ->
        AlertDialog(
            onDismissRequest = viewModel::hideLogoutConfirmation,
            title = { Text("Log out?") },
            text = {
                Text("Are you sure you want to log out from ${account.email} on ${account.instanceHost}?")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.logout(account.id) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Log out")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideLogoutConfirmation) {
                    Text("Cancel")
                }
            }
        )
    }

    // Logout All Confirmation Dialog
    if (uiState.showLogoutAllConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::hideLogoutAllConfirmation,
            title = { Text("Log out from all accounts?") },
            text = {
                Text("You will be logged out from all ${uiState.allAccounts.size} accounts. You will need to sign in again.")
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::logoutAll,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Log out all")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideLogoutAllConfirmation) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    isDestructive: Boolean = false
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = if (isDestructive)
            MaterialTheme.colorScheme.error
        else
            MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun AccountItem(
    account: Account,
    isActive: Boolean,
    showSwitcher: Boolean,
    onSwitcherClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = account.userName,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        supportingContent = {
            Column {
                Text(account.email)
                Text(
                    account.instanceHost,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        leadingContent = {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = account.userName.first().uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        },
        trailingContent = {
            Row {
                if (showSwitcher) {
                    IconButton(onClick = onSwitcherClick) {
                        Icon(
                            Icons.Default.SwitchAccount,
                            contentDescription = "Switch account"
                        )
                    }
                }
                IconButton(onClick = onLogoutClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Log out",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    )
}

@Composable
private fun ThemeSelector(
    currentTheme: ThemeMode,
    onThemeSelect: (ThemeMode) -> Unit
) {
    Column {
        ThemeMode.entries.forEach { theme ->
            ListItem(
                headlineContent = {
                    Text(
                        when (theme) {
                            ThemeMode.SYSTEM -> "System default"
                            ThemeMode.LIGHT -> "Light"
                            ThemeMode.DARK -> "Dark"
                        }
                    )
                },
                leadingContent = {
                    RadioButton(
                        selected = currentTheme == theme,
                        onClick = { onThemeSelect(theme) }
                    )
                },
                modifier = Modifier.clickable { onThemeSelect(theme) }
            )
        }
    }
}

@Composable
private fun AccountSwitcherDialog(
    accounts: List<Account>,
    currentAccountId: String?,
    onAccountSelect: (String) -> Unit,
    onAddAccount: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Switch Account") },
        text = {
            LazyColumn {
                items(accounts) { account ->
                    ListItem(
                        headlineContent = { Text(account.userName) },
                        supportingContent = {
                            Column {
                                Text(account.email)
                                Text(
                                    account.instanceHost,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        },
                        leadingContent = {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = if (account.id == currentAccountId)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = account.userName.first().uppercase(),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (account.id == currentAccountId)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        trailingContent = {
                            if (account.id == currentAccountId) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Active",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier.clickable {
                            if (account.id != currentAccountId) {
                                onAccountSelect(account.id)
                            }
                        }
                    )
                    HorizontalDivider()
                }

                item {
                    ListItem(
                        headlineContent = { Text("Add account") },
                        leadingContent = {
                            Icon(Icons.Default.PersonAdd, contentDescription = null)
                        },
                        modifier = Modifier.clickable { onAddAccount() }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
