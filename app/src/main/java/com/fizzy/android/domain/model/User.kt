package com.fizzy.android.domain.model

data class User(
    val id: Long,
    val name: String,
    val email: String,
    val avatarUrl: String? = null,
    val admin: Boolean = false
)
