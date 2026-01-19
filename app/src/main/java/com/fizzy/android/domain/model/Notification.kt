package com.fizzy.android.domain.model

import java.time.Instant

data class Notification(
    val id: Long,
    val type: NotificationType,
    val title: String,
    val body: String,
    val read: Boolean,
    val createdAt: Instant,
    val cardId: Long? = null,
    val boardId: Long? = null,
    val actor: User? = null
)

enum class NotificationType {
    CARD_ASSIGNED,
    CARD_MENTIONED,
    CARD_COMMENTED,
    CARD_MOVED,
    CARD_UPDATED,
    STEP_COMPLETED,
    REACTION_ADDED,
    BOARD_SHARED,
    OTHER
}
