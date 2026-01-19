package com.fizzy.android.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fizzy.android.core.network.ApiResult
import com.fizzy.android.core.network.InstanceManager
import com.fizzy.android.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val step: AuthStep = AuthStep.INSTANCE_SELECTION,
    val instanceUrl: String = InstanceManager.OFFICIAL_INSTANCE,
    val email: String = "",
    val code: String = "",
    val token: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val usePersonalToken: Boolean = false
)

enum class AuthStep {
    INSTANCE_SELECTION,
    EMAIL_INPUT,
    CODE_VERIFICATION,
    PERSONAL_TOKEN
}

sealed class AuthEvent {
    data object AuthSuccess : AuthEvent()
    data class ShowError(val message: String) : AuthEvent()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AuthEvent>()
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    val isLoggedIn: StateFlow<Boolean> = authRepository.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun initializeAuth() {
        viewModelScope.launch {
            authRepository.initializeActiveAccount()
        }
    }

    fun onInstanceUrlChange(url: String) {
        _uiState.update { it.copy(instanceUrl = url, error = null) }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, error = null) }
    }

    fun onCodeChange(code: String) {
        _uiState.update { it.copy(code = code.uppercase().take(6), error = null) }
    }

    fun onTokenChange(token: String) {
        _uiState.update { it.copy(token = token, error = null) }
    }

    fun useOfficialInstance() {
        _uiState.update {
            it.copy(
                instanceUrl = InstanceManager.OFFICIAL_INSTANCE,
                step = AuthStep.EMAIL_INPUT,
                error = null
            )
        }
    }

    fun useSelfHosted() {
        _uiState.update {
            it.copy(
                instanceUrl = "",
                error = null
            )
        }
    }

    fun onContinueWithInstance() {
        val url = _uiState.value.instanceUrl.trim()
        if (url.isBlank()) {
            _uiState.update { it.copy(error = "Please enter a valid instance URL") }
            return
        }
        _uiState.update { it.copy(step = AuthStep.EMAIL_INPUT, error = null) }
    }

    fun toggleAuthMethod() {
        _uiState.update {
            it.copy(
                usePersonalToken = !it.usePersonalToken,
                step = if (!it.usePersonalToken) AuthStep.PERSONAL_TOKEN else AuthStep.EMAIL_INPUT,
                error = null
            )
        }
    }

    fun requestMagicLink() {
        val email = _uiState.value.email.trim()
        if (email.isBlank() || !email.contains("@")) {
            _uiState.update { it.copy(error = "Please enter a valid email address") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = authRepository.requestMagicLink(_uiState.value.instanceUrl, email)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            step = AuthStep.CODE_VERIFICATION
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to send magic link: ${result.message}"
                        )
                    }
                }
                is ApiResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Network error. Please check your connection."
                        )
                    }
                }
            }
        }
    }

    fun verifyCode() {
        val code = _uiState.value.code.trim()
        if (code.length != 6) {
            _uiState.update { it.copy(error = "Please enter the 6-character code") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = authRepository.verifyMagicLink(
                _uiState.value.instanceUrl,
                _uiState.value.email,
                code
            )) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(AuthEvent.AuthSuccess)
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = if (result.code == 401) "Invalid code. Please try again." else "Verification failed: ${result.message}"
                        )
                    }
                }
                is ApiResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Network error. Please check your connection."
                        )
                    }
                }
            }
        }
    }

    fun loginWithToken() {
        val token = _uiState.value.token.trim()
        if (token.isBlank()) {
            _uiState.update { it.copy(error = "Please enter your personal access token") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = authRepository.loginWithToken(_uiState.value.instanceUrl, token)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(AuthEvent.AuthSuccess)
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message ?: "Invalid token or authentication failed"
                        )
                    }
                }
                is ApiResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Network error. Please check your connection."
                        )
                    }
                }
            }
        }
    }

    fun goBack() {
        _uiState.update { state ->
            when (state.step) {
                AuthStep.EMAIL_INPUT, AuthStep.PERSONAL_TOKEN -> state.copy(
                    step = AuthStep.INSTANCE_SELECTION,
                    error = null
                )
                AuthStep.CODE_VERIFICATION -> state.copy(
                    step = AuthStep.EMAIL_INPUT,
                    code = "",
                    error = null
                )
                else -> state
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
