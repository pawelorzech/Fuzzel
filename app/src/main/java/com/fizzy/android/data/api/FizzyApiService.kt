package com.fizzy.android.data.api

import com.fizzy.android.data.api.dto.*
import retrofit2.Response
import retrofit2.http.*

interface FizzyApiService {

    // ==================== Auth ====================

    @POST("session")
    suspend fun requestMagicLink(@Body request: RequestMagicLinkRequest): Response<RequestMagicLinkResponse>

    @POST("session/magic_link")
    suspend fun verifyMagicLink(@Body request: VerifyMagicLinkRequest): Response<VerifyMagicLinkResponse>

    @GET("my/identity.json")
    suspend fun getCurrentIdentity(): Response<IdentityDto>

    // Legacy alias
    @GET("my/identity.json")
    suspend fun getCurrentUser(): Response<UserDto>

    // ==================== Users ====================

    @GET("users")
    suspend fun getUsers(): Response<List<UserDto>>

    @GET("users/{userId}")
    suspend fun getUser(@Path("userId") userId: String): Response<UserDto>

    // ==================== Boards ====================

    @GET("boards.json")
    suspend fun getBoards(): Response<BoardsResponse>

    @GET("boards/{boardId}.json")
    suspend fun getBoard(@Path("boardId") boardId: String): Response<BoardResponse>

    @POST("boards.json")
    suspend fun createBoard(@Body request: CreateBoardRequest): Response<Unit>

    @PUT("boards/{boardId}")
    suspend fun updateBoard(
        @Path("boardId") boardId: String,
        @Body request: UpdateBoardRequest
    ): Response<Unit>

    @DELETE("boards/{boardId}.json")
    suspend fun deleteBoard(@Path("boardId") boardId: String): Response<Unit>

    // ==================== Columns ====================

    @GET("boards/{boardId}/columns.json")
    suspend fun getColumns(@Path("boardId") boardId: String): Response<ColumnsResponse>

    @POST("boards/{boardId}/columns.json")
    suspend fun createColumn(
        @Path("boardId") boardId: String,
        @Body request: CreateColumnRequest
    ): Response<Unit>

    @PUT("boards/{boardId}/columns/{columnId}")
    suspend fun updateColumn(
        @Path("boardId") boardId: String,
        @Path("columnId") columnId: String,
        @Body request: UpdateColumnRequest
    ): Response<Unit>

    @DELETE("boards/{boardId}/columns/{columnId}.json")
    suspend fun deleteColumn(
        @Path("boardId") boardId: String,
        @Path("columnId") columnId: String
    ): Response<Unit>

    // ==================== Cards ====================

    @GET("cards.json")
    suspend fun getCards(@Query("board_ids[]") boardId: String? = null): Response<CardsResponse>

    @GET("cards/{cardNumber}.json")
    suspend fun getCard(@Path("cardNumber") cardNumber: Int): Response<CardResponse>

    @POST("boards/{boardId}/cards.json")
    suspend fun createCard(
        @Path("boardId") boardId: String,
        @Body request: CreateCardRequest
    ): Response<Unit>

    @PUT("cards/{cardNumber}")
    suspend fun updateCard(
        @Path("cardNumber") cardNumber: Int,
        @Body request: UpdateCardRequest
    ): Response<Unit>

    @DELETE("cards/{cardNumber}.json")
    suspend fun deleteCard(@Path("cardNumber") cardNumber: Int): Response<Unit>

    // ==================== Card Actions ====================

    // Close/Reopen
    @POST("cards/{cardNumber}/closure")
    suspend fun closeCard(@Path("cardNumber") cardNumber: Int): Response<Unit>

    @DELETE("cards/{cardNumber}/closure")
    suspend fun reopenCard(@Path("cardNumber") cardNumber: Int): Response<Unit>

    // Not Now (defer/put aside)
    @POST("cards/{cardNumber}/not_now")
    suspend fun markCardNotNow(@Path("cardNumber") cardNumber: Int): Response<Unit>

    // Triage (move to column)
    @POST("cards/{cardNumber}/triage")
    suspend fun triageCard(
        @Path("cardNumber") cardNumber: Int,
        @Body request: TriageCardRequest
    ): Response<Unit>

    @DELETE("cards/{cardNumber}/triage")
    suspend fun untriageCard(@Path("cardNumber") cardNumber: Int): Response<Unit>

    // Priority (golden)
    @POST("cards/{cardNumber}/goldness")
    suspend fun markCardGolden(@Path("cardNumber") cardNumber: Int): Response<Unit>

    @DELETE("cards/{cardNumber}/goldness")
    suspend fun unmarkCardGolden(@Path("cardNumber") cardNumber: Int): Response<Unit>

    // Watch
    @POST("cards/{cardNumber}/watch")
    suspend fun watchCard(@Path("cardNumber") cardNumber: Int): Response<Unit>

    @DELETE("cards/{cardNumber}/watch")
    suspend fun unwatchCard(@Path("cardNumber") cardNumber: Int): Response<Unit>

    // ==================== Assignments ====================

    @POST("cards/{cardNumber}/assignments")
    suspend fun addAssignment(
        @Path("cardNumber") cardNumber: Int,
        @Body request: AssignmentRequest
    ): Response<Unit>

    // Note: Removing assignment may require PUT cards/{cardNumber} with updated assignee list
    // or a separate endpoint - documentation unclear

    // ==================== Tags (Account Level) ====================

    @GET("tags")
    suspend fun getTags(): Response<List<TagDto>>

    // ==================== Taggings (Card Tags) ====================

    @POST("cards/{cardNumber}/taggings")
    suspend fun addTagging(
        @Path("cardNumber") cardNumber: Int,
        @Body request: TaggingRequest
    ): Response<Unit>

    @DELETE("cards/{cardNumber}/taggings/{taggingId}")
    suspend fun removeTagging(
        @Path("cardNumber") cardNumber: Int,
        @Path("taggingId") taggingId: String
    ): Response<Unit>

    // ==================== Steps ====================

    @GET("cards/{cardNumber}/steps.json")
    suspend fun getSteps(@Path("cardNumber") cardNumber: Int): Response<StepsResponse>

    @POST("cards/{cardNumber}/steps.json")
    suspend fun createStep(
        @Path("cardNumber") cardNumber: Int,
        @Body request: CreateStepRequest
    ): Response<Unit>

    @PUT("cards/{cardNumber}/steps/{stepId}")
    suspend fun updateStep(
        @Path("cardNumber") cardNumber: Int,
        @Path("stepId") stepId: String,
        @Body request: UpdateStepRequest
    ): Response<Unit>

    @DELETE("cards/{cardNumber}/steps/{stepId}.json")
    suspend fun deleteStep(
        @Path("cardNumber") cardNumber: Int,
        @Path("stepId") stepId: String
    ): Response<Unit>

    // ==================== Comments ====================

    @GET("cards/{cardNumber}/comments.json")
    suspend fun getComments(@Path("cardNumber") cardNumber: Int): Response<CommentsResponse>

    @POST("cards/{cardNumber}/comments.json")
    suspend fun createComment(
        @Path("cardNumber") cardNumber: Int,
        @Body request: CreateCommentRequest
    ): Response<Unit>

    @PUT("cards/{cardNumber}/comments/{commentId}")
    suspend fun updateComment(
        @Path("cardNumber") cardNumber: Int,
        @Path("commentId") commentId: String,
        @Body request: UpdateCommentRequest
    ): Response<Unit>

    @DELETE("cards/{cardNumber}/comments/{commentId}.json")
    suspend fun deleteComment(
        @Path("cardNumber") cardNumber: Int,
        @Path("commentId") commentId: String
    ): Response<Unit>

    // ==================== Reactions ====================

    @POST("cards/{cardNumber}/comments/{commentId}/reactions")
    suspend fun addReaction(
        @Path("cardNumber") cardNumber: Int,
        @Path("commentId") commentId: String,
        @Body request: CreateReactionRequest
    ): Response<Unit>

    @DELETE("cards/{cardNumber}/comments/{commentId}/reactions/{reactionId}")
    suspend fun removeReaction(
        @Path("cardNumber") cardNumber: Int,
        @Path("commentId") commentId: String,
        @Path("reactionId") reactionId: String
    ): Response<Unit>

    // ==================== Notifications ====================

    @GET("notifications.json")
    suspend fun getNotifications(): Response<NotificationsResponse>

    @POST("notifications/{notificationId}/reading")
    suspend fun markNotificationRead(@Path("notificationId") notificationId: String): Response<Unit>

    @DELETE("notifications/{notificationId}/reading")
    suspend fun markNotificationUnread(@Path("notificationId") notificationId: String): Response<Unit>

    @POST("notifications/bulk_reading")
    suspend fun markAllNotificationsRead(): Response<Unit>
}
