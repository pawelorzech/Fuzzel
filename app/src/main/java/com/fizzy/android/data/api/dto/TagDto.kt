package com.fizzy.android.data.api.dto

import com.fizzy.android.domain.model.Tag
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TagDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "color") val color: String? = null
)

// API returns direct array for account-level tags
typealias TagsResponse = List<TagDto>

// Tagging request - add tag to card by tag title
@JsonClass(generateAdapter = true)
data class TaggingRequest(
    @Json(name = "tag_title") val tagTitle: String
)

// Tagging DTO - represents a tag association on a card
@JsonClass(generateAdapter = true)
data class TaggingDto(
    @Json(name = "id") val id: String,
    @Json(name = "tag") val tag: TagDto
)

fun TagDto.toDomain(): Tag = Tag(
    id = id.toLongOrNull() ?: 0L,
    name = title ?: name ?: "",
    color = color ?: "#808080"
)
