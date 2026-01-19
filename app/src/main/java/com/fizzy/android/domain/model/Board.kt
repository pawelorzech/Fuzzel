package com.fizzy.android.domain.model

import java.time.Instant

data class Board(
    val id: String,
    val name: String,
    val description: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant? = null,
    val cardsCount: Int = 0,
    val columnsCount: Int = 0,
    val creator: User? = null,
    val allAccess: Boolean = false,
    val url: String? = null
)
