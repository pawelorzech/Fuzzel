package com.fizzy.android.data.api.dto

import android.util.Log
import com.fizzy.android.domain.model.Card
import com.fizzy.android.domain.model.CardStatus
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

private const val TAG = "CardDto"

@JsonClass(generateAdapter = true)
data class CardDto(
    @Json(name = "id") val id: String,
    @Json(name = "number") val number: Int = 0,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "position") val position: Int = 0,
    @Json(name = "column") val column: CardColumnDto? = null,
    @Json(name = "board") val board: CardBoardDto? = null,
    @Json(name = "status") val status: String = "active",
    @Json(name = "golden") val golden: Boolean = false,
    @Json(name = "closed") val closed: Boolean = false,
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "last_active_at") val lastActiveAt: String? = null,
    @Json(name = "creator") val creator: UserDto? = null,
    @Json(name = "assignees") val assignees: List<UserDto>? = null,
    @Json(name = "tags") val tags: List<TagDto>? = null,
    @Json(name = "steps") val steps: List<StepDto>? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "comments_url") val commentsUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class CardColumnDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String? = null,
    @Json(name = "color") val color: ColumnColorDto? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CardBoardDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String? = null,
    @Json(name = "all_access") val allAccess: Boolean = false,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "creator") val creator: UserDto? = null
)

// API returns direct array, not wrapped
typealias CardsResponse = List<CardDto>
typealias CardResponse = CardDto

// Wrapped request for creating cards (Fizzy API requires nested object)
@JsonClass(generateAdapter = true)
data class CreateCardRequest(
    @Json(name = "card") val card: CardData
)

@JsonClass(generateAdapter = true)
data class CardData(
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "column_id") val columnId: String? = null,
    @Json(name = "tag_ids") val tagIds: List<String>? = null
)

// Wrapped request for updating cards
@JsonClass(generateAdapter = true)
data class UpdateCardRequest(
    @Json(name = "card") val card: UpdateCardData
)

@JsonClass(generateAdapter = true)
data class UpdateCardData(
    @Json(name = "title") val title: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "column_id") val columnId: String? = null,
    @Json(name = "position") val position: Int? = null
)

// Triage card to a specific column
@JsonClass(generateAdapter = true)
data class TriageCardRequest(
    @Json(name = "column_id") val columnId: String
)

// Add assignment to a card
@JsonClass(generateAdapter = true)
data class AssignmentRequest(
    @Json(name = "assignee_id") val assigneeId: String
)

fun CardDto.toDomain(): Card {
    Log.d(TAG, "toDomain: title='$title', column=$column, columnId=${column?.id}")
    return Card(
        id = number.toLong(), // Use number for card identification (used in URLs)
        title = title,
        description = description,
        position = position,
        columnId = column?.id ?: "",
        boardId = board?.id ?: "",
        status = when {
            closed -> CardStatus.CLOSED
            status.lowercase() == "triaged" -> CardStatus.TRIAGED
            status.lowercase() == "deferred" -> CardStatus.DEFERRED
            else -> CardStatus.ACTIVE
        },
        priority = golden,
        watching = false, // Not in API response
        triageAt = null, // Not directly in API response
        deferUntil = null, // Not directly in API response
        createdAt = if (createdAt.isNotEmpty()) Instant.parse(createdAt) else Instant.now(),
        updatedAt = lastActiveAt?.let { Instant.parse(it) } ?: Instant.now(),
        creator = creator?.toDomain(),
        assignees = assignees?.map { it.toDomain() } ?: emptyList(),
        tags = tags?.map { it.toDomain() } ?: emptyList(),
        stepsTotal = steps?.size ?: 0,
        stepsCompleted = steps?.count { it.completed } ?: 0,
        commentsCount = 0 // Not in list response
    )
}

// Helper function to create CreateCardRequest with nested structure
fun createCardRequest(title: String, description: String?, columnId: String): CreateCardRequest {
    return CreateCardRequest(
        card = CardData(
            title = title,
            description = description,
            columnId = columnId
        )
    )
}

// Helper function to create UpdateCardRequest with nested structure
fun updateCardRequest(
    title: String? = null,
    description: String? = null,
    columnId: String? = null,
    position: Int? = null
): UpdateCardRequest {
    return UpdateCardRequest(
        card = UpdateCardData(
            title = title,
            description = description,
            columnId = columnId,
            position = position
        )
    )
}
