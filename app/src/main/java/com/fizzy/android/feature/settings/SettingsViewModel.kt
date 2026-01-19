package com.fizzy.android.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fizzy.android.data.local.SettingsStorage
import com.fizzy.android.data.local.ThemeMode
import com.fizzy.android.domain.model.Account
import com.fizzy.android.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val currentAccount: Account? = null,
    val allAccounts: List<Account> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showAccountSwitcher: Boolean = false,
    val showAddAccount: Boolean = false,
    val showLogoutConfirmation: Account? = null,
    val showLogoutAllConfirmation: Boolean = false
)

sealed class SettingsEvent {
    data object Logout : SettingsEvent()
    data object AccountSwitched : SettingsEvent()
    data object NavigateToAddAccount : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsStorage: SettingsStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        observeAccounts()
        observeTheme()
    }

    private fun observeAccounts() {
        viewModelScope.launch {
            combine(
                authRepository.currentAccount,
                authRepository.allAccounts
            ) { current, all ->
                Pair(current, all)
            }.collect { (current, all) ->
                _uiState.update {
                    it.copy(
                        currentAccount = current,
                        allAccounts = all
                    )
                }
            }
        }
    }

    private fun observeTheme() {
        viewModelScope.launch {
            settingsStorage.themeMode.collect { theme ->
                _uiState.update { it.copy(themeMode = theme) }
            }
        }
    }

    fun showAccountSwitcher() {
        _uiState.update { it.copy(showAccountSwitcher = true) }
    }

    fun hideAccountSwitcher() {
        _uiState.update { it.copy(showAccountSwitcher = false) }
    }

    fun switchAccount(accountId: String) {
        viewModelScope.launch {
            authRepository.switchAccount(accountId)
            _uiState.update { it.copy(showAccountSwitcher = false) }
            _events.emit(SettingsEvent.AccountSwitched)
        }
    }

    fun showAddAccount() {
        _uiState.update { it.copy(showAccountSwitcher = false, showAddAccount = true) }
    }

    fun hideAddAccount() {
        _uiState.update { it.copy(showAddAccount = false) }
    }

    fun navigateToAddAccount() {
        viewModelScope.launch {
            _events.emit(SettingsEvent.NavigateToAddAccount)
        }
    }

    fun showLogoutConfirmation(account: Account) {
        _uiState.update { it.copy(showLogoutConfirmation = account) }
    }

    fun hideLogoutConfirmation() {
        _uiState.update { it.copy(showLogoutConfirmation = null) }
    }

    fun logout(accountId: String) {
        viewModelScope.launch {
            authRepository.logout(accountId)
            _uiState.update { it.copy(showLogoutConfirmation = null) }

            // Check if there are any accounts left
            val remaining = _uiState.value.allAccounts.filter { it.id != accountId }
            if (remaining.isEmpty()) {
                _events.emit(SettingsEvent.Logout)
            }
        }
    }

    fun showLogoutAllConfirmation() {
        _uiState.update { it.copy(showLogoutAllConfirmation = true) }
    }

    fun hideLogoutAllConfirmation() {
        _uiState.update { it.copy(showLogoutAllConfirmation = false) }
    }

    fun logoutAll() {
        viewModelScope.launch {
            authRepository.logoutAll()
            _uiState.update { it.copy(showLogoutAllConfirmation = false) }
            _events.emit(SettingsEvent.Logout)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsStorage.setThemeMode(mode)
        }
    }
}
