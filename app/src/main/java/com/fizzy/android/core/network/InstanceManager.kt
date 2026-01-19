package com.fizzy.android.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstanceManager @Inject constructor() {

    private val _currentInstance = MutableStateFlow<String?>(null)
    val currentInstance: StateFlow<String?> = _currentInstance.asStateFlow()

    private val _currentToken = MutableStateFlow<String?>(null)
    val currentToken: StateFlow<String?> = _currentToken.asStateFlow()

    private val _accountSlug = MutableStateFlow<String?>(null)
    val accountSlug: StateFlow<String?> = _accountSlug.asStateFlow()

    fun setInstance(baseUrl: String, token: String, slug: String? = null) {
        val normalizedUrl = normalizeUrl(baseUrl)
        _currentInstance.value = normalizedUrl
        _currentToken.value = token
        _accountSlug.value = slug?.removePrefix("/")
    }

    fun clearInstance() {
        _currentInstance.value = null
        _currentToken.value = null
        _accountSlug.value = null
    }

    fun getBaseUrl(): String? = _currentInstance.value

    fun getToken(): String? = _currentToken.value

    fun getAccountSlug(): String? = _accountSlug.value

    private fun normalizeUrl(url: String): String {
        var normalized = url.trim()
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://$normalized"
        }
        if (!normalized.endsWith("/")) {
            normalized = "$normalized/"
        }
        return normalized
    }

    companion object {
        const val OFFICIAL_INSTANCE = "https://fizzy.com/"
    }
}
