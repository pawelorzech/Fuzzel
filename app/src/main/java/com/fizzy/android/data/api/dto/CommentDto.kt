package com.fizzy.android.data.api.dto

import com.fizzy.android.domain.model.Comment
import com.fizzy.android.domain.model.Reaction
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

@JsonClass(generateAdapter = true)
data class CommentDto(
    @Json(name = "id") val id: String,
    @Json(name = "content") val content: String,
    @Json(name = "card_id") val cardId: String? = null,
    @Json(name = "author") val author: UserDto? = null,
    @Json(name = "creator") val creator: UserDto? = null,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "reactions") val reactions: List<ReactionDto>? = null
)

@JsonClass(generateAdapter = true)
data class ReactionDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "content") val content: String? = null,
    @Json(name = "emoji") val emoji: String? = null,
    @Json(name = "count") val count: Int = 1,
    @Json(name = "users") val users: List<UserDto>? = null,
    @Json(name = "reacted_by_me") val reactedByMe: Boolean = false,
    @Json(name = "creator") val creator: UserDto? = null
)

// API returns direct arrays, not wrapped
typealias CommentsResponse = List<CommentDto>
typealias CommentResponse = CommentDto

// Wrapped request for creating comments (Fizzy API requires nested object)
@JsonClass(generateAdapter = true)
data class CreateCommentRequest(
    @Json(name = "comment") val comment: CommentData
)

@JsonClass(generateAdapter = true)
data class CommentData(
    @Json(name = "body") val body: String
)

// Wrapped request for updating comments
@JsonClass(generateAdapter = true)
data class UpdateCommentRequest(
    @Json(name = "comment") val comment: CommentData
)

// Wrapped request for creating reactions
@JsonClass(generateAdapter = true)
data class CreateReactionRequest(
    @Json(name = "reaction") val reaction: ReactionData
)

@JsonClass(generateAdapter = true)
data class ReactionData(
    @Json(name = "content") val content: String
)

fun ReactionDto.toDomain(): Reaction = Reaction(
    emoji = content ?: emoji ?: "",
    count = count,
    users = users?.map { it.toDomain() } ?: emptyList(),
    reactedByMe = reactedByMe
)

fun CommentDto.toDomain(): Comment = Comment(
    id = id.toLongOrNull() ?: 0L,
    content = content,
    cardId = cardId?.toLongOrNull() ?: 0L,
    author = (author ?: creator)?.toDomain() ?: throw IllegalStateException("Comment must have author or creator"),
    createdAt = Instant.parse(createdAt),
    updatedAt = updatedAt?.let { Instant.parse(it) } ?: Instant.parse(createdAt),
    reactions = reactions?.map { it.toDomain() } ?: emptyList()
)

// Helper function to create CreateCommentRequest with nested structure
fun createCommentRequest(content: String): CreateCommentRequest {
    return CreateCommentRequest(
        comment = CommentData(body = content)
    )
}

// Helper function to create UpdateCommentRequest with nested structure
fun updateCommentRequest(content: String): UpdateCommentRequest {
    return UpdateCommentRequest(
        comment = CommentData(body = content)
    )
}

// Helper function to create CreateReactionRequest with nested structure
fun createReactionRequest(emoji: String): CreateReactionRequest {
    return CreateReactionRequest(
        reaction = ReactionData(content = emoji)
    )
}
