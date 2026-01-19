package com.fizzy.android.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Request magic link - Fizzy API uses email_address, not email
@JsonClass(generateAdapter = true)
data class RequestMagicLinkRequest(
    @Json(name = "email_address") val emailAddress: String
)

// Response contains pending_authentication_token for magic link flow
@JsonClass(generateAdapter = true)
data class RequestMagicLinkResponse(
    @Json(name = "pending_authentication_token") val pendingAuthenticationToken: String
)

// Verify magic link - send the code received via email
@JsonClass(generateAdapter = true)
data class VerifyMagicLinkRequest(
    @Json(name = "code") val code: String
)

// Verify response returns session_token
@JsonClass(generateAdapter = true)
data class VerifyMagicLinkResponse(
    @Json(name = "session_token") val sessionToken: String
)

// For Personal Access Token authentication (alternative to magic link)
@JsonClass(generateAdapter = true)
data class PersonalAccessTokenRequest(
    @Json(name = "token") val token: String
)
