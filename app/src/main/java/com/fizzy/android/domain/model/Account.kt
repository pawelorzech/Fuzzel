package com.fizzy.android.domain.model

data class Account(
    val id: String,
    val instanceUrl: String,
    val email: String,
    val token: String,
    val userName: String,
    val userId: Long,
    val avatarUrl: String? = null,
    val isActive: Boolean = false,
    val fizzyAccountId: String? = null,
    val fizzyAccountSlug: String? = null
) {
    val displayName: String
        get() = "$userName ($instanceUrl)"

    val instanceHost: String
        get() = instanceUrl
            .removePrefix("https://")
            .removePrefix("http://")
            .removeSuffix("/")
}
