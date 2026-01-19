package com.fizzy.android.domain.model

import java.time.Instant
import java.time.LocalDate

data class Card(
    val id: Long,
    val title: String,
    val description: String? = null,
    val position: Int,
    val columnId: String,
    val boardId: String,
    val status: CardStatus = CardStatus.ACTIVE,
    val priority: Boolean = false,
    val watching: Boolean = false,
    val triageAt: LocalDate? = null,
    val deferUntil: LocalDate? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val creator: User? = null,
    val assignees: List<User> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val stepsTotal: Int = 0,
    val stepsCompleted: Int = 0,
    val commentsCount: Int = 0
) {
    val hasSteps: Boolean
        get() = stepsTotal > 0

    val stepsProgress: Float
        get() = if (stepsTotal > 0) stepsCompleted.toFloat() / stepsTotal else 0f

    val stepsDisplay: String
        get() = "$stepsCompleted/$stepsTotal"
}

enum class CardStatus {
    ACTIVE,
    CLOSED,
    TRIAGED,
    DEFERRED
}
