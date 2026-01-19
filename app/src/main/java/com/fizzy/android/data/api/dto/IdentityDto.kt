package com.fizzy.android.data.api.dto

import com.fizzy.android.domain.model.User
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response from GET /my/identity.json
 * Example:
 * {
 *   "accounts": [{
 *     "id": "03ff9o0317vymjugkux4h9wkj",
 *     "name": "Paweł's Fizzy",
 *     "slug": "/0000001",
 *     "created_at": "2026-01-18T02:57:58.339Z",
 *     "user": {
 *       "id": "03ff9o042qaxblmwrxdgcs0fk",
 *       "name": "Paweł",
 *       "role": "owner",
 *       "active": true,
 *       "email_address": "pawel@orzech.me",
 *       "created_at": "2026-01-18T02:57:58.597Z",
 *       "url": "https://kanban.orzech.me/users/03ff9o042qaxblmwrxdgcs0fk"
 *     }
 *   }]
 * }
 */
@JsonClass(generateAdapter = true)
data class IdentityDto(
    @Json(name = "accounts") val accounts: List<IdentityAccountDto>? = null
)

@JsonClass(generateAdapter = true)
data class IdentityAccountDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "user") val user: IdentityUserDto? = null
)

@JsonClass(generateAdapter = true)
data class IdentityUserDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "role") val role: String? = null,
    @Json(name = "active") val active: Boolean? = null,
    @Json(name = "email_address") val emailAddress: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "url") val url: String? = null
)

fun IdentityDto.toUser(): User {
    val firstAccount = accounts?.firstOrNull()
    val user = firstAccount?.user
    return User(
        id = 0L, // Fizzy uses string IDs, we'll use 0 as placeholder
        name = user?.name ?: "Unknown",
        email = user?.emailAddress ?: "",
        avatarUrl = user?.avatarUrl,
        admin = user?.role == "owner" || user?.role == "admin"
    )
}

fun IdentityDto.getUserId(): String? = accounts?.firstOrNull()?.user?.id

fun IdentityDto.getAccountId(): String? = accounts?.firstOrNull()?.id

fun IdentityDto.getAccountSlug(): String? = accounts?.firstOrNull()?.slug
