package com.fizzy.android.data.api.dto

import com.fizzy.android.domain.model.Notification
import com.fizzy.android.domain.model.NotificationType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

@JsonClass(generateAdapter = true)
data class NotificationDto(
    @Json(name = "id") val id: String,
    @Json(name = "read") val read: Boolean,
    @Json(name = "read_at") val readAt: String? = null,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "title") val title: String,
    @Json(name = "body") val body: String,
    @Json(name = "type") val type: String? = null,
    @Json(name = "creator") val creator: UserDto? = null,
    @Json(name = "card") val card: NotificationCardDto? = null,
    @Json(name = "url") val url: String? = null
)

@JsonClass(generateAdapter = true)
data class NotificationCardDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "status") val status: String? = null,
    @Json(name = "url") val url: String? = null
)

// API returns notifications list with unread count
@JsonClass(generateAdapter = true)
data class NotificationsResponse(
    @Json(name = "notifications") val notifications: List<NotificationDto>,
    @Json(name = "unread_count") val unreadCount: Int = 0
)

fun NotificationDto.toDomain(): Notification = Notification(
    id = id.toLongOrNull() ?: 0L,
    type = when (type?.lowercase()) {
        "card_assigned" -> NotificationType.CARD_ASSIGNED
        "card_mentioned" -> NotificationType.CARD_MENTIONED
        "card_commented" -> NotificationType.CARD_COMMENTED
        "card_moved" -> NotificationType.CARD_MOVED
        "card_updated" -> NotificationType.CARD_UPDATED
        "step_completed" -> NotificationType.STEP_COMPLETED
        "reaction_added" -> NotificationType.REACTION_ADDED
        "board_shared" -> NotificationType.BOARD_SHARED
        else -> NotificationType.OTHER
    },
    title = title,
    body = body,
    read = read,
    createdAt = Instant.parse(createdAt),
    cardId = card?.id?.toLongOrNull(),
    boardId = null, // Not directly in new API structure
    actor = creator?.toDomain()
)
