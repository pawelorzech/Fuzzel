package com.fizzy.android.domain.model

data class Column(
    val id: String,
    val name: String,
    val position: Int,
    val boardId: String,
    val cards: List<Card> = emptyList(),
    val cardsCount: Int = 0
)
