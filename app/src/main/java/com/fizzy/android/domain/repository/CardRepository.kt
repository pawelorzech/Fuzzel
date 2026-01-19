package com.fizzy.android.domain.repository

import com.fizzy.android.core.network.ApiResult
import com.fizzy.android.domain.model.Card
import com.fizzy.android.domain.model.Comment
import com.fizzy.android.domain.model.Step
import java.time.LocalDate

interface CardRepository {
    suspend fun getBoardCards(boardId: String): ApiResult<List<Card>>
    suspend fun getCard(cardId: Long): ApiResult<Card>
    suspend fun createCard(boardId: String, columnId: String, title: String, description: String?): ApiResult<Card>
    suspend fun updateCard(cardId: Long, title: String?, description: String?): ApiResult<Card>
    suspend fun deleteCard(cardId: Long): ApiResult<Unit>
    suspend fun moveCard(cardId: Long, columnId: String, position: Int): ApiResult<Card>

    // Card actions
    suspend fun closeCard(cardId: Long): ApiResult<Card>
    suspend fun reopenCard(cardId: Long): ApiResult<Card>
    suspend fun triageCard(cardId: Long, date: LocalDate): ApiResult<Card>
    suspend fun deferCard(cardId: Long, date: LocalDate): ApiResult<Card>
    suspend fun togglePriority(cardId: Long, priority: Boolean): ApiResult<Card>
    suspend fun toggleWatch(cardId: Long, watching: Boolean): ApiResult<Card>

    // Assignees
    suspend fun addAssignee(cardId: Long, userId: Long): ApiResult<Card>
    suspend fun removeAssignee(cardId: Long, userId: Long): ApiResult<Card>

    // Tags
    suspend fun addTag(cardId: Long, tagId: Long): ApiResult<Card>
    suspend fun removeTag(cardId: Long, tagId: Long): ApiResult<Card>

    // Steps
    suspend fun getSteps(cardId: Long): ApiResult<List<Step>>
    suspend fun createStep(cardId: Long, description: String): ApiResult<Step>
    suspend fun updateStep(cardId: Long, stepId: Long, description: String?, completed: Boolean?, position: Int?): ApiResult<Step>
    suspend fun deleteStep(cardId: Long, stepId: Long): ApiResult<Unit>

    // Comments
    suspend fun getComments(cardId: Long): ApiResult<List<Comment>>
    suspend fun createComment(cardId: Long, content: String): ApiResult<Comment>
    suspend fun updateComment(cardId: Long, commentId: Long, content: String): ApiResult<Comment>
    suspend fun deleteComment(cardId: Long, commentId: Long): ApiResult<Unit>

    // Reactions
    suspend fun addReaction(cardId: Long, commentId: Long, emoji: String): ApiResult<Comment>
    suspend fun removeReaction(cardId: Long, commentId: Long, emoji: String): ApiResult<Comment>
}
