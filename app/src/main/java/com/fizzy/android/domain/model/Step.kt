package com.fizzy.android.domain.model

import java.time.Instant

data class Step(
    val id: Long,
    val description: String,
    val completed: Boolean,
    val position: Int,
    val cardId: Long,
    val completedBy: User? = null,
    val completedAt: Instant? = null
)
