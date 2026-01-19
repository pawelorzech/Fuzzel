package com.fizzy.android.data.api.dto

import com.fizzy.android.domain.model.Board
import com.fizzy.android.domain.model.User
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

@JsonClass(generateAdapter = true)
data class BoardDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "cards_count") val cardsCount: Int = 0,
    @Json(name = "columns_count") val columnsCount: Int = 0,
    @Json(name = "creator") val creator: CreatorDto? = null,
    @Json(name = "all_access") val allAccess: Boolean = false,
    @Json(name = "url") val url: String? = null
)

// Creator in board response has different structure than User
@JsonClass(generateAdapter = true)
data class CreatorDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "role") val role: String? = null,
    @Json(name = "active") val active: Boolean? = null,
    @Json(name = "email_address") val emailAddress: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "url") val url: String? = null
)

// API returns direct array/object, not wrapped
typealias BoardsResponse = List<BoardDto>
typealias BoardResponse = BoardDto

// Wrapped request for creating boards (Fizzy API requires nested object)
@JsonClass(generateAdapter = true)
data class CreateBoardRequest(
    @Json(name = "board") val board: BoardData
)

@JsonClass(generateAdapter = true)
data class BoardData(
    @Json(name = "name") val name: String,
    @Json(name = "all_access") val allAccess: Boolean? = null
)

// Wrapped request for updating boards
@JsonClass(generateAdapter = true)
data class UpdateBoardRequest(
    @Json(name = "board") val board: UpdateBoardData
)

@JsonClass(generateAdapter = true)
data class UpdateBoardData(
    @Json(name = "name") val name: String? = null,
    @Json(name = "all_access") val allAccess: Boolean? = null
)

fun BoardDto.toDomain(): Board = Board(
    id = id,
    name = name,
    description = description,
    createdAt = Instant.parse(createdAt),
    updatedAt = updatedAt?.let { Instant.parse(it) },
    cardsCount = cardsCount,
    columnsCount = columnsCount,
    creator = creator?.toUser(),
    allAccess = allAccess,
    url = url
)

fun CreatorDto.toUser(): User = User(
    id = 0L, // Fizzy uses string IDs
    name = name,
    email = emailAddress ?: "",
    avatarUrl = avatarUrl,
    admin = role == "owner" || role == "admin"
)

// Helper function to create CreateBoardRequest with nested structure
fun createBoardRequest(name: String, allAccess: Boolean? = null): CreateBoardRequest {
    return CreateBoardRequest(
        board = BoardData(
            name = name,
            allAccess = allAccess
        )
    )
}

// Helper function to create UpdateBoardRequest with nested structure
fun updateBoardRequest(name: String? = null, allAccess: Boolean? = null): UpdateBoardRequest {
    return UpdateBoardRequest(
        board = UpdateBoardData(
            name = name,
            allAccess = allAccess
        )
    )
}
