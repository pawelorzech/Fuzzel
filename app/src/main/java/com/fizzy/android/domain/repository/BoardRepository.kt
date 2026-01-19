package com.fizzy.android.domain.repository

import com.fizzy.android.core.network.ApiResult
import com.fizzy.android.domain.model.Board
import com.fizzy.android.domain.model.Column
import com.fizzy.android.domain.model.Tag
import com.fizzy.android.domain.model.User
import kotlinx.coroutines.flow.Flow

interface BoardRepository {
    fun observeBoards(): Flow<List<Board>>

    suspend fun getBoards(): ApiResult<List<Board>>
    suspend fun getBoard(boardId: String): ApiResult<Board>
    suspend fun createBoard(name: String, description: String?): ApiResult<Board>
    suspend fun updateBoard(boardId: String, name: String?, description: String?): ApiResult<Board>
    suspend fun deleteBoard(boardId: String): ApiResult<Unit>

    suspend fun getColumns(boardId: String): ApiResult<List<Column>>
    suspend fun createColumn(boardId: String, name: String, position: Int?): ApiResult<Column>
    suspend fun updateColumn(boardId: String, columnId: String, name: String?, position: Int?): ApiResult<Column>
    suspend fun deleteColumn(boardId: String, columnId: String): ApiResult<Unit>

    suspend fun getTags(boardId: String): ApiResult<List<Tag>>
    suspend fun createTag(boardId: String, name: String, color: String): ApiResult<Tag>
    suspend fun deleteTag(boardId: String, tagId: Long): ApiResult<Unit>

    suspend fun getBoardUsers(boardId: String): ApiResult<List<User>>

    suspend fun refreshBoards()
}
