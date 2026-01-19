package com.fizzy.android.domain.model

import java.time.Instant

data class Comment(
    val id: Long,
    val content: String,
    val cardId: Long,
    val author: User,
    val createdAt: Instant,
    val updatedAt: Instant,
    val reactions: List<Reaction> = emptyList()
)

data class Reaction(
    val emoji: String,
    val count: Int,
    val users: List<User> = emptyList(),
    val reactedByMe: Boolean = false
)
