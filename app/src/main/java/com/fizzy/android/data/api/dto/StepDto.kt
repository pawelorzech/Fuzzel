package com.fizzy.android.data.api.dto

import com.fizzy.android.domain.model.Step
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

@JsonClass(generateAdapter = true)
data class StepDto(
    @Json(name = "id") val id: String,
    @Json(name = "content") val content: String,
    @Json(name = "completed") val completed: Boolean,
    @Json(name = "position") val position: Int = 0,
    @Json(name = "card_id") val cardId: String? = null,
    @Json(name = "completed_by") val completedBy: UserDto? = null,
    @Json(name = "completed_at") val completedAt: String? = null
)

// API returns direct arrays, not wrapped
typealias StepsResponse = List<StepDto>
typealias StepResponse = StepDto

// Wrapped request for creating steps (Fizzy API requires nested object)
@JsonClass(generateAdapter = true)
data class CreateStepRequest(
    @Json(name = "step") val step: StepData
)

@JsonClass(generateAdapter = true)
data class StepData(
    @Json(name = "content") val content: String,
    @Json(name = "completed") val completed: Boolean? = null
)

// Wrapped request for updating steps
@JsonClass(generateAdapter = true)
data class UpdateStepRequest(
    @Json(name = "step") val step: UpdateStepData
)

@JsonClass(generateAdapter = true)
data class UpdateStepData(
    @Json(name = "content") val content: String? = null,
    @Json(name = "completed") val completed: Boolean? = null,
    @Json(name = "position") val position: Int? = null
)

fun StepDto.toDomain(): Step = Step(
    id = id.toLongOrNull() ?: 0L,
    description = content, // Map content to description in domain model
    completed = completed,
    position = position,
    cardId = cardId?.toLongOrNull() ?: 0L,
    completedBy = completedBy?.toDomain(),
    completedAt = completedAt?.let { Instant.parse(it) }
)

// Helper function to create CreateStepRequest with nested structure
fun createStepRequest(content: String): CreateStepRequest {
    return CreateStepRequest(
        step = StepData(content = content)
    )
}

// Helper function to create UpdateStepRequest with nested structure
fun updateStepRequest(
    content: String? = null,
    completed: Boolean? = null,
    position: Int? = null
): UpdateStepRequest {
    return UpdateStepRequest(
        step = UpdateStepData(
            content = content,
            completed = completed,
            position = position
        )
    )
}
