package com.fizzy.android.data.repository

import com.fizzy.android.core.network.ApiResult
import com.fizzy.android.data.api.FizzyApiService
import com.fizzy.android.data.api.dto.*
import com.fizzy.android.domain.model.Board
import com.fizzy.android.domain.model.Column
import com.fizzy.android.domain.model.Tag
import com.fizzy.android.domain.model.User
import com.fizzy.android.domain.repository.BoardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BoardRepositoryImpl @Inject constructor(
    private val apiService: FizzyApiService
) : BoardRepository {

    private val _boardsFlow = MutableStateFlow<List<Board>>(emptyList())

    override fun observeBoards(): Flow<List<Board>> = _boardsFlow.asStateFlow()

    override suspend fun getBoards(): ApiResult<List<Board>> {
        return ApiResult.from {
            apiService.getBoards()
        }.map { response ->
            val boards = response.map { it.toDomain() }
            _boardsFlow.value = boards
            boards
        }
    }

    override suspend fun getBoard(boardId: String): ApiResult<Board> {
        return ApiResult.from {
            apiService.getBoard(boardId)
        }.map { response ->
            response.toDomain()
        }
    }

    override suspend fun createBoard(name: String, description: String?): ApiResult<Board> {
        val response = apiService.createBoard(createBoardRequest(name))
        return if (response.isSuccessful) {
            // Parse board ID from Location header: /0000001/boards/{id}.json
            val location = response.headers()["Location"]
            val boardId = location?.substringAfterLast("/boards/")?.removeSuffix(".json")

            if (boardId != null) {
                // Refresh the boards list and return the new board
                val boardsResult = getBoards()
                if (boardsResult is ApiResult.Success) {
                    val newBoard = boardsResult.data.find { it.id == boardId }
                    if (newBoard != null) {
                        ApiResult.Success(newBoard)
                    } else {
                        ApiResult.Error(0, "Board created but not found in list")
                    }
                } else {
                    ApiResult.Error(0, "Board created but failed to refresh list")
                }
            } else {
                ApiResult.Error(0, "Board created but no ID in response")
            }
        } else {
            ApiResult.Error(response.code(), response.message())
        }
    }

    override suspend fun updateBoard(boardId: String, name: String?, description: String?): ApiResult<Board> {
        val response = apiService.updateBoard(boardId, updateBoardRequest(name))
        return if (response.isSuccessful) {
            // API returns 204 No Content, so refetch the board
            val boardResult = getBoard(boardId)
            if (boardResult is ApiResult.Success) {
                val updatedBoard = boardResult.data
                _boardsFlow.value = _boardsFlow.value.map {
                    if (it.id == boardId) updatedBoard else it
                }
                ApiResult.Success(updatedBoard)
            } else {
                boardResult
            }
        } else {
            ApiResult.Error(response.code(), response.message())
        }
    }

    override suspend fun deleteBoard(boardId: String): ApiResult<Unit> {
        return ApiResult.from {
            apiService.deleteBoard(boardId)
        }.map {
            _boardsFlow.value = _boardsFlow.value.filter { it.id != boardId }
        }
    }

    override suspend fun getColumns(boardId: String): ApiResult<List<Column>> {
        return ApiResult.from {
            apiService.getColumns(boardId)
        }.map { response ->
            response.map { it.toDomain() }.sortedBy { it.position }
        }
    }

    override suspend fun createColumn(boardId: String, name: String, position: Int?): ApiResult<Column> {
        val response = apiService.createColumn(boardId, createColumnRequest(name, position = position))
        return if (response.isSuccessful) {
            // API returns 201 with empty body, parse column ID from Location header
            val location = response.headers()["Location"]
            val columnId = location?.substringAfterLast("/columns/")?.removeSuffix(".json")

            if (columnId != null) {
                // Refetch the columns and return the new one
                val columnsResult = getColumns(boardId)
                if (columnsResult is ApiResult.Success) {
                    val newColumn = columnsResult.data.find { it.id == columnId }
                    if (newColumn != null) {
                        ApiResult.Success(newColumn)
                    } else {
                        ApiResult.Error(0, "Column created but not found in list")
                    }
                } else {
                    ApiResult.Error(0, "Column created but failed to refresh list")
                }
            } else {
                ApiResult.Error(0, "Column created but no ID in response")
            }
        } else {
            ApiResult.Error(response.code(), response.message())
        }
    }

    override suspend fun updateColumn(
        boardId: String,
        columnId: String,
        name: String?,
        position: Int?
    ): ApiResult<Column> {
        val response = apiService.updateColumn(boardId, columnId, updateColumnRequest(name, position = position))
        return if (response.isSuccessful) {
            // API returns 204 No Content, so refetch the columns
            val columnsResult = getColumns(boardId)
            if (columnsResult is ApiResult.Success) {
                val updatedColumn = columnsResult.data.find { it.id == columnId }
                if (updatedColumn != null) {
                    ApiResult.Success(updatedColumn)
                } else {
                    ApiResult.Error(0, "Column updated but not found in list")
                }
            } else {
                ApiResult.Error(0, "Column updated but failed to refresh list")
            }
        } else {
            ApiResult.Error(response.code(), response.message())
        }
    }

    override suspend fun deleteColumn(boardId: String, columnId: String): ApiResult<Unit> {
        return ApiResult.from {
            apiService.deleteColumn(boardId, columnId)
        }
    }

    // Tags are now at account level, not board level
    override suspend fun getTags(boardId: String): ApiResult<List<Tag>> {
        return ApiResult.from {
            apiService.getTags()
        }.map { response ->
            response.map { it.toDomain() }
        }
    }

    override suspend fun createTag(boardId: String, name: String, color: String): ApiResult<Tag> {
        // Note: Tag creation at account level is not currently supported in the API service
        // This would need a new endpoint like POST /tags with wrapped request
        android.util.Log.w("BoardRepository", "createTag: Account-level tag creation not implemented")
        return ApiResult.Error(501, "Tag creation at account level not implemented")
    }

    override suspend fun deleteTag(boardId: String, tagId: Long): ApiResult<Unit> {
        // Note: Tag deletion at account level is not currently supported in the API service
        // This would need a new endpoint like DELETE /tags/{tagId}
        android.util.Log.w("BoardRepository", "deleteTag: Account-level tag deletion not implemented")
        return ApiResult.Error(501, "Tag deletion at account level not implemented")
    }

    override suspend fun getBoardUsers(boardId: String): ApiResult<List<User>> {
        return ApiResult.from {
            apiService.getUsers()
        }.map { users ->
            users.map { it.toDomain() }
        }
    }

    override suspend fun refreshBoards() {
        getBoards()
    }
}
