package com.fizzy.android.data.api.dto

import com.fizzy.android.domain.model.Column
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ColumnDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "position") val position: Int = 0,
    @Json(name = "board_id") val boardId: String = "",
    @Json(name = "cards") val cards: List<CardDto>? = null,
    @Json(name = "cards_count") val cardsCount: Int = 0,
    @Json(name = "color") val color: ColumnColorDto? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class ColumnColorDto(
    @Json(name = "name") val name: String,
    @Json(name = "value") val value: String
)

// API returns direct arrays, not wrapped
typealias ColumnsResponse = List<ColumnDto>
typealias ColumnResponse = ColumnDto

// Wrapped request for creating columns (Fizzy API requires nested object)
@JsonClass(generateAdapter = true)
data class CreateColumnRequest(
    @Json(name = "column") val column: ColumnData
)

@JsonClass(generateAdapter = true)
data class ColumnData(
    @Json(name = "name") val name: String,
    @Json(name = "color") val color: String? = null,
    @Json(name = "position") val position: Int? = null
)

// Wrapped request for updating columns
@JsonClass(generateAdapter = true)
data class UpdateColumnRequest(
    @Json(name = "column") val column: UpdateColumnData
)

@JsonClass(generateAdapter = true)
data class UpdateColumnData(
    @Json(name = "name") val name: String? = null,
    @Json(name = "color") val color: String? = null,
    @Json(name = "position") val position: Int? = null
)

fun ColumnDto.toDomain(): Column = Column(
    id = id,
    name = name,
    position = position,
    boardId = boardId,
    cards = cards?.map { it.toDomain() } ?: emptyList(),
    cardsCount = cardsCount
)

// Helper function to create CreateColumnRequest with nested structure
fun createColumnRequest(name: String, color: String? = null, position: Int? = null): CreateColumnRequest {
    return CreateColumnRequest(
        column = ColumnData(
            name = name,
            color = color,
            position = position
        )
    )
}

// Helper function to create UpdateColumnRequest with nested structure
fun updateColumnRequest(name: String? = null, color: String? = null, position: Int? = null): UpdateColumnRequest {
    return UpdateColumnRequest(
        column = UpdateColumnData(
            name = name,
            color = color,
            position = position
        )
    )
}
