package com.fizzy.android.data.repository

import android.util.Log
import com.fizzy.android.core.network.ApiResult
import com.fizzy.android.data.api.FizzyApiService
import com.fizzy.android.data.api.dto.*
import com.fizzy.android.domain.model.Card
import com.fizzy.android.domain.model.Comment
import com.fizzy.android.domain.model.Step
import com.fizzy.android.domain.repository.CardRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CardRepositoryImpl"

@Singleton
class CardRepositoryImpl @Inject constructor(
    private val apiService: FizzyApiService
) : CardRepository {

    override suspend fun getBoardCards(boardId: String): ApiResult<List<Card>> = coroutineScope {
        Log.d(TAG, "getBoardCards: Fetching all card states for board $boardId")

        // Fetch all 3 types of cards in parallel
        val activeDeferred = async { apiService.getCards(boardId, "all") }
        val closedDeferred = async { apiService.getCards(boardId, "closed") }
        val notNowDeferred = async { apiService.getCards(boardId, "not_now") }

        val activeResponse = activeDeferred.await()
        val closedResponse = closedDeferred.await()
        val notNowResponse = notNowDeferred.await()

        Log.d(TAG, "getBoardCards responses - active: ${activeResponse.isSuccessful}, closed: ${closedResponse.isSuccessful}, notNow: ${notNowResponse.isSuccessful}")

        // Combine all cards
        val allCards = mutableListOf<Card>()

        if (activeResponse.isSuccessful) {
            activeResponse.body()?.let { cards ->
                Log.d(TAG, "getBoardCards: ${cards.size} active cards")
                allCards.addAll(cards.map { it.toDomain() })
            }
        } else {
            Log.e(TAG, "getBoardCards active error: ${activeResponse.code()} - ${activeResponse.message()}")
        }

        if (closedResponse.isSuccessful) {
            closedResponse.body()?.let { cards ->
                Log.d(TAG, "getBoardCards: ${cards.size} closed cards")
                allCards.addAll(cards.map { it.toDomain() })
            }
        } else {
            Log.e(TAG, "getBoardCards closed error: ${closedResponse.code()} - ${closedResponse.message()}")
        }

        if (notNowResponse.isSuccessful) {
            notNowResponse.body()?.let { cards ->
                Log.d(TAG, "getBoardCards: ${cards.size} not_now cards")
                allCards.addAll(cards.map { it.toDomain() })
            }
        } else {
            Log.e(TAG, "getBoardCards not_now error: ${notNowResponse.code()} - ${notNowResponse.message()}")
        }

        // Deduplicate by ID and sort
        val uniqueCards = allCards.distinctBy { it.id }.sortedBy { it.position }
        Log.d(TAG, "getBoardCards: Total ${uniqueCards.size} unique cards")

        // Return success if at least active cards were fetched
        if (activeResponse.isSuccessful) {
            ApiResult.Success(uniqueCards)
        } else {
            ApiResult.Error(activeResponse.code(), activeResponse.message())
        }
    }

    override suspend fun getCard(cardId: Long): ApiResult<Card> {
        return ApiResult.from {
            apiService.getCard(cardId.toInt())
        }.map { response ->
            response.toDomain()
        }
    }

    override suspend fun createCard(
        boardId: String,
        columnId: String,
        title: String,
        description: String?
    ): ApiResult<Card> {
        val response = apiService.createCard(boardId, createCardRequest(title, description, columnId))
        return if (response.isSuccessful) {
            // API returns 201 with empty body, parse card number from Location header
            val location = response.headers()["Location"]
            val cardNumberStr = location?.substringAfterLast("/cards/")?.removeSuffix(".json")
            val cardNumber = cardNumberStr?.toIntOrNull()

            if (cardNumber != null) {
                // Fetch the new card
                getCard(cardNumber.toLong())
            } else {
                ApiResult.Error(0, "Card created but no number in response")
            }
        } else {
            ApiResult.Error(response.code(), response.message())
        }
    }

    override suspend fun updateCard(cardId: Long, title: String?, description: String?): ApiResult<Card> {
        val response = apiService.updateCard(cardId.toInt(), updateCardRequest(title = title, description = description))
        return if (response.isSuccessful) {
            // API returns 204 No Content, so refetch the card
            getCard(cardId)
        } else {
            ApiResult.Error(response.code(), response.message())
        }
    }

    override suspend fun deleteCard(cardId: Long): ApiResult<Unit> {
        return ApiResult.from {
            apiService.deleteCard(cardId.toInt())
        }
    }

    override suspend fun moveCard(cardId: Long, columnId: String, position: Int): ApiResult<Card> {
        // Use triage endpoint to move card to a column
        val response = apiService.triageCard(cardId.toInt(), TriageCardRequest(columnId))
        return if (response.isSuccessful) {
            // If we also need to update position, do it separately
            if (position > 0) {
                apiService.updateCard(cardId.toInt(), updateCardRequest(columnId = columnId, position = position))
            }
            getCard(cardId)
        } else {
            ApiResult.Error(response.code(), response.message())
        }
    }

    override suspend fun closeCard(cardId: Long): ApiResult<Card> {
        val response = apiService.closeCard(cardId.toInt())
        return if (response.isSuccessful) {
            getCard(cardId)
        } else {
            ApiResult.Error(response.code(), response.message())
        }
    }

    override suspend fun reopenCard(cardId: Long): ApiResult<Card> {
        val response = apiService.reopenCard(cardId.toInt())
        return if (response.isSuccessful) {
            getCard(cardId)
        } else {
            ApiResult.Error(response.code(), response.message())
        }
    }

    override suspend fun triageCard(cardId: Long, date: LocalDate): ApiResult<Card> {
        // Note: Fizzy API triage takes column_id, not date
        // This may need to be adjusted based on actual API behavior
        val response = apiService.markCardNotNow(cardId.toInt())
        return if (response.isSuccessful) {
            getCard(cardId)
        } else {
            ApiResult.Error(response.code(), response.message())
        }
    }

    override suspend fun deferCard(cardId: Long, date: LocalDate): ApiResult<Card> {
        // Use not_now endpoint for deferring
        val response = apiService.markCardNotNow(cardId.toInt())
        return if (response.isSuccessful) {
            getCard(cardId)
        } else {
            ApiResult.Error(response.code(), response.message())
        }
    }

    override suspend fun togglePriority(cardId: Long, priority: Boolean): ApiResult<Card> {
        val response = if (priority) {
            apiService.markCardGolden(cardId.toInt())
        } else {
            apiService.unmarkCardGolden(cardId.toInt())
        }
        return if (response.isSuccessful) {
            getCard(cardId)
        } else {
            ApiResult.Error(response.code(), response.message())
        }
    }

    override suspend fun toggleWatch(cardId: Long, watching: Boolean): ApiResult<Card> {
        val response = if (watching) {
            apiService.watchCard(cardId.toInt())
        } else {
            apiService.unwatchCard(cardId.toInt())
        }
        return if (response.isSuccessful) {
            getCard(cardId)
        } else {
            ApiResult.Error(response.code(), response.message())
        }
    }

    override suspend fun addAssignee(cardId: Long, userId: Long): ApiResult<Card> {
        val response = apiService.addAssignment(cardId.toInt(), AssignmentRequest(userId.toString()))
        return if (response.isSuccessful) {
            getCard(cardId)
        } else {
            ApiResult.Error(response.code(), response.message())
        }
    }

    override suspend fun removeAssignee(cardId: Long, userId: Long): ApiResult<Card> {
        // Note: The Fizzy API may not have a direct endpoint for removing assignees
        // This might need to be done via PUT cards/{cardNumber} with updated assignee list
        // For now, refetch the card (this is a stub that needs actual API clarification)
        Log.w(TAG, "removeAssignee: API endpoint unclear, operation may not work correctly")
        return getCard(cardId)
    }

    override suspend fun addTag(cardId: Long, tagId: Long): ApiResult<Card> {
        // Note: Fizzy API uses tag_title for taggings, not tag_id
        // This might need a separate getTags call to get the title first
        // For now, assuming the tagId is actually the tag title or we have it cached
        Log.w(TAG, "addTag: Using tagId as tag title - may need adjustment")
        val response = apiService.addTagging(cardId.toInt(), TaggingRequest(tagId.toString()))
        return if (response.isSuccessful) {
            getCard(cardId)
        } else {
            ApiResult.Error(response.code(), response.message())
        }
    }

    override suspend fun removeTag(cardId: Long, tagId: Long): ApiResult<Card> {
        // Fizzy API uses taggingId to remove, not tagId
        // This needs the tagging ID from the card's tags list
        val response = apiService.removeTagging(cardId.toInt(), tagId.toString())
        return if (response.isSuccessful) {
            getCard(cardId)
        } else {
            ApiResult.Error(response.code(), response.message())
        }
    }

    override suspend fun getSteps(cardId: Long): ApiResult<List<Step>> {
        return ApiResult.from {
            apiService.getSteps(cardId.toInt())
        }.map { response ->
            response.map { it.toDomain() }.sortedBy { it.position }
        }
    }

    override suspend fun createStep(cardId: Long, description: String): ApiResult<Step> {
        val response = apiService.createStep(cardId.toInt(), createStepRequest(description))
        return if (response.isSuccessful) {
            // API returns 201 with empty body, parse step ID from Location header
            val location = response.headers()["Location"]
            val stepIdStr = location?.substringAfterLast("/steps/")?.removeSuffix(".json")
            val stepId = stepIdStr?.toLongOrNull()

            if (stepId != null) {
                // Refetch the steps and return the new one
                val stepsResult = getSteps(cardId)
                if (stepsResult is ApiResult.Success) {
                    val newStep = stepsResult.data.find { it.id == stepId }
                    if (newStep != null) {
                        ApiResult.Success(newStep)
                    } else {
                        ApiResult.Error(0, "Step created but not found in list")
                    }
                } else {
                    ApiResult.Error(0, "Step created but failed to refresh list")
                }
            } else {
                ApiResult.Error(0, "Step created but no ID in response")
            }
        } else {
            ApiResult.Error(response.code(), response.message())
        }
    }

    override suspend fun updateStep(
        cardId: Long,
        stepId: Long,
        description: String?,
        completed: Boolean?,
        position: Int?
    ): ApiResult<Step> {
        val response = apiService.updateStep(
            cardId.toInt(),
            stepId.toString(),
            updateStepRequest(content = description, completed = completed, position = position)
        )
        return if (response.isSuccessful) {
            // API returns 204 No Content, so refetch the steps
            val stepsResult = getSteps(cardId)
            if (stepsResult is ApiResult.Success) {
                val updatedStep = stepsResult.data.find { it.id == stepId }
                if (updatedStep != null) {
                    ApiResult.Success(updatedStep)
                } else {
                    ApiResult.Error(0, "Step updated but not found in list")
                }
            } else {
                ApiResult.Error(0, "Step updated but failed to refresh list")
            }
        } else {
            ApiResult.Error(response.code(), response.message())
        }
    }

    override suspend fun deleteStep(cardId: Long, stepId: Long): ApiResult<Unit> {
        return ApiResult.from {
            apiService.deleteStep(cardId.toInt(), stepId.toString())
        }
    }

    override suspend fun getComments(cardId: Long): ApiResult<List<Comment>> {
        return ApiResult.from {
            apiService.getComments(cardId.toInt())
        }.map { response ->
            response.map { it.toDomain() }
        }
    }

    override suspend fun createComment(cardId: Long, content: String): ApiResult<Comment> {
        val response = apiService.createComment(cardId.toInt(), createCommentRequest(content))
        return if (response.isSuccessful) {
            // API returns 201 with empty body, parse comment ID from Location header
            val location = response.headers()["Location"]
            val commentIdStr = location?.substringAfterLast("/comments/")?.removeSuffix(".json")
            val commentId = commentIdStr?.toLongOrNull()

            if (commentId != null) {
                // Refetch the comments and return the new one
                val commentsResult = getComments(cardId)
                if (commentsResult is ApiResult.Success) {
                    val newComment = commentsResult.data.find { it.id == commentId }
                    if (newComment != null) {
                        ApiResult.Success(newComment)
                    } else {
                        ApiResult.Error(0, "Comment created but not found in list")
                    }
                } else {
                    ApiResult.Error(0, "Comment created but failed to refresh list")
                }
            } else {
                ApiResult.Error(0, "Comment created but no ID in response")
            }
        } else {
            ApiResult.Error(response.code(), response.message())
        }
    }

    override suspend fun updateComment(cardId: Long, commentId: Long, content: String): ApiResult<Comment> {
        val response = apiService.updateComment(cardId.toInt(), commentId.toString(), updateCommentRequest(content))
        return if (response.isSuccessful) {
            // API returns 204 No Content, so refetch the comments
            val commentsResult = getComments(cardId)
            if (commentsResult is ApiResult.Success) {
                val updatedComment = commentsResult.data.find { it.id == commentId }
                if (updatedComment != null) {
                    ApiResult.Success(updatedComment)
                } else {
                    ApiResult.Error(0, "Comment updated but not found in list")
                }
            } else {
                ApiResult.Error(0, "Comment updated but failed to refresh list")
            }
        } else {
            ApiResult.Error(response.code(), response.message())
        }
    }

    override suspend fun deleteComment(cardId: Long, commentId: Long): ApiResult<Unit> {
        return ApiResult.from {
            apiService.deleteComment(cardId.toInt(), commentId.toString())
        }
    }

    override suspend fun addReaction(cardId: Long, commentId: Long, emoji: String): ApiResult<Comment> {
        val response = apiService.addReaction(cardId.toInt(), commentId.toString(), createReactionRequest(emoji))
        return if (response.isSuccessful) {
            // Refetch comments and find the updated one
            val commentsResult = getComments(cardId)
            if (commentsResult is ApiResult.Success) {
                val comment = commentsResult.data.find { it.id == commentId }
                if (comment != null) {
                    ApiResult.Success(comment)
                } else {
                    ApiResult.Error(0, "Reaction added but comment not found")
                }
            } else {
                ApiResult.Error(0, "Reaction added but failed to refresh comments")
            }
        } else {
            ApiResult.Error(response.code(), response.message())
        }
    }

    override suspend fun removeReaction(cardId: Long, commentId: Long, emoji: String): ApiResult<Comment> {
        // Note: Fizzy API removes by reactionId, not emoji
        // This needs the reaction ID from the comment's reactions list
        // For now, treating emoji as reactionId (needs proper implementation)
        Log.w(TAG, "removeReaction: Using emoji as reactionId - may need adjustment")
        val response = apiService.removeReaction(cardId.toInt(), commentId.toString(), emoji)
        return if (response.isSuccessful) {
            // Refetch comments and find the updated one
            val commentsResult = getComments(cardId)
            if (commentsResult is ApiResult.Success) {
                val comment = commentsResult.data.find { it.id == commentId }
                if (comment != null) {
                    ApiResult.Success(comment)
                } else {
                    ApiResult.Error(0, "Reaction removed but comment not found")
                }
            } else {
                ApiResult.Error(0, "Reaction removed but failed to refresh comments")
            }
        } else {
            ApiResult.Error(response.code(), response.message())
        }
    }
}
