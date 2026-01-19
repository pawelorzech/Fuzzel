package com.fizzy.android.feature.boards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fizzy.android.core.network.ApiResult
import com.fizzy.android.domain.model.Board
import com.fizzy.android.domain.repository.BoardRepository
import com.fizzy.android.domain.repository.CardRepository
import com.fizzy.android.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BoardListUiState(
    val boards: List<Board> = emptyList(),
    val filteredBoards: List<Board> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val showCreateDialog: Boolean = false,
    val showEditDialog: Board? = null,
    val showDeleteConfirmation: Board? = null
)

sealed class BoardListEvent {
    data class NavigateToBoard(val boardId: String) : BoardListEvent()
    data class ShowError(val message: String) : BoardListEvent()
    data object BoardCreated : BoardListEvent()
    data object BoardUpdated : BoardListEvent()
    data object BoardDeleted : BoardListEvent()
}

@HiltViewModel
class BoardListViewModel @Inject constructor(
    private val boardRepository: BoardRepository,
    private val cardRepository: CardRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BoardListUiState())
    val uiState: StateFlow<BoardListUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<BoardListEvent>()
    val events: SharedFlow<BoardListEvent> = _events.asSharedFlow()

    val unreadNotificationsCount: StateFlow<Int> = notificationRepository.unreadCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        loadBoards()
        observeBoards()
    }

    private fun observeBoards() {
        viewModelScope.launch {
            boardRepository.observeBoards().collect { boards ->
                // Fetch stats for boards from the flow
                val boardsWithStats = fetchBoardStats(boards)
                _uiState.update { state ->
                    state.copy(
                        boards = boardsWithStats,
                        filteredBoards = filterBoards(boardsWithStats, state.searchQuery)
                    )
                }
            }
        }
    }

    fun loadBoards() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = boardRepository.getBoards()) {
                is ApiResult.Success -> {
                    val boards = result.data
                    // Fetch stats for each board in parallel
                    val boardsWithStats = fetchBoardStats(boards)
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            boards = boardsWithStats,
                            filteredBoards = filterBoards(boardsWithStats, state.searchQuery)
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to load boards: ${result.message}"
                        )
                    }
                }
                is ApiResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Network error. Please check your connection."
                        )
                    }
                }
            }
        }
    }

    private suspend fun fetchBoardStats(boards: List<Board>): List<Board> = coroutineScope {
        boards.map { board ->
            async {
                val columnsResult = boardRepository.getColumns(board.id)
                val cardsResult = cardRepository.getBoardCards(board.id)
                board.copy(
                    columnsCount = (columnsResult as? ApiResult.Success)?.data?.size ?: 0,
                    cardsCount = (cardsResult as? ApiResult.Success)?.data?.size ?: 0
                )
            }
        }.awaitAll()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            boardRepository.refreshBoards()
            notificationRepository.getNotifications()

            // Re-fetch stats for updated boards
            val currentBoards = _uiState.value.boards
            if (currentBoards.isNotEmpty()) {
                val boardsWithStats = fetchBoardStats(currentBoards)
                _uiState.update { state ->
                    state.copy(
                        boards = boardsWithStats,
                        filteredBoards = filterBoards(boardsWithStats, state.searchQuery)
                    )
                }
            }

            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredBoards = filterBoards(state.boards, query)
            )
        }
    }

    fun clearSearch() {
        _uiState.update { state ->
            state.copy(
                searchQuery = "",
                filteredBoards = state.boards
            )
        }
    }

    private fun filterBoards(boards: List<Board>, query: String): List<Board> {
        if (query.isBlank()) return boards
        return boards.filter { board ->
            board.name.contains(query, ignoreCase = true) ||
                    board.description?.contains(query, ignoreCase = true) == true
        }
    }

    fun showCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true) }
    }

    fun hideCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false) }
    }

    fun createBoard(name: String, description: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = boardRepository.createBoard(name, description?.takeIf { it.isNotBlank() })) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, showCreateDialog = false) }
                    _events.emit(BoardListEvent.BoardCreated)
                    _events.emit(BoardListEvent.NavigateToBoard(result.data.id))
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(BoardListEvent.ShowError("Failed to create board: ${result.message}"))
                }
                is ApiResult.Exception -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(BoardListEvent.ShowError("Network error"))
                }
            }
        }
    }

    fun showEditDialog(board: Board) {
        _uiState.update { it.copy(showEditDialog = board) }
    }

    fun hideEditDialog() {
        _uiState.update { it.copy(showEditDialog = null) }
    }

    fun updateBoard(boardId: String, name: String, description: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (boardRepository.updateBoard(boardId, name, description?.takeIf { it.isNotBlank() })) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, showEditDialog = null) }
                    _events.emit(BoardListEvent.BoardUpdated)
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(BoardListEvent.ShowError("Failed to update board"))
                }
                is ApiResult.Exception -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(BoardListEvent.ShowError("Network error"))
                }
            }
        }
    }

    fun showDeleteConfirmation(board: Board) {
        _uiState.update { it.copy(showDeleteConfirmation = board) }
    }

    fun hideDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = null) }
    }

    fun deleteBoard(boardId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (boardRepository.deleteBoard(boardId)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, showDeleteConfirmation = null) }
                    _events.emit(BoardListEvent.BoardDeleted)
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(BoardListEvent.ShowError("Failed to delete board"))
                }
                is ApiResult.Exception -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(BoardListEvent.ShowError("Network error"))
                }
            }
        }
    }
}
