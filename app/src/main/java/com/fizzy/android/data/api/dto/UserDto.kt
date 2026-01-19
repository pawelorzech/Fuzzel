package com.fizzy.android.data.api.dto

import com.fizzy.android.domain.model.User
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "email_address") val emailAddress: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "role") val role: String? = null,
    @Json(name = "active") val active: Boolean = true,
    @Json(name = "admin") val admin: Boolean = false,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "url") val url: String? = null
)

fun UserDto.toDomain(): User = User(
    id = id.hashCode().toLong(), // Convert string ID to long for domain model
    name = name,
    email = emailAddress ?: "",
    avatarUrl = avatarUrl,
    admin = admin || role == "owner"
)
