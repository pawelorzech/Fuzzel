package com.fizzy.android.feature.kanban

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fizzy.android.core.network.ApiResult
import com.fizzy.android.domain.model.Board
import com.fizzy.android.domain.model.Card
import com.fizzy.android.domain.model.CardStatus
import com.fizzy.android.domain.model.Column
import com.fizzy.android.domain.repository.BoardRepository
import com.fizzy.android.domain.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import android.util.Log
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "KanbanViewModel"

data class KanbanUiState(
    val board: Board? = null,
    val columns: List<Column> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val showAddColumnDialog: Boolean = false,
    val editingColumn: Column? = null,
    val showAddCardDialog: String? = null, // columnId
    val dragState: DragState = DragState()
)

data class DragState(
    val isDragging: Boolean = false,
    val draggingCard: Card? = null,
    val sourceColumnId: String? = null,
    val targetColumnId: String? = null,
    val targetPosition: Int? = null
)

sealed class KanbanEvent {
    data class ShowError(val message: String) : KanbanEvent()
    data class NavigateToCard(val cardId: Long) : KanbanEvent()
    data object ColumnCreated : KanbanEvent()
    data object ColumnUpdated : KanbanEvent()
    data object ColumnDeleted : KanbanEvent()
    data object CardCreated : KanbanEvent()
    data object CardMoved : KanbanEvent()
}

@HiltViewModel
class KanbanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val boardRepository: BoardRepository,
    private val cardRepository: CardRepository
) : ViewModel() {

    private val boardId: String = checkNotNull(savedStateHandle["boardId"])

    private val _uiState = MutableStateFlow(KanbanUiState())
    val uiState: StateFlow<KanbanUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<KanbanEvent>()
    val events: SharedFlow<KanbanEvent> = _events.asSharedFlow()

    private var pollingJob: Job? = null

    init {
        loadBoard()
        startPolling()
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }

    private fun startPolling() {
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(10_000) // Poll every 10 seconds
                if (!_uiState.value.isDragging()) {
                    silentRefresh()
                }
            }
        }
    }

    private suspend fun silentRefresh() {
        val columnsResult = boardRepository.getColumns(boardId)
        val cardsResult = cardRepository.getBoardCards(boardId)

        if (columnsResult is ApiResult.Success) {
            val cards = if (cardsResult is ApiResult.Success) cardsResult.data else emptyList()
            val columnsWithCards = distributeCardsToColumns(columnsResult.data, cards)
            _uiState.update { state ->
                state.copy(columns = columnsWithCards)
            }
        }
    }

    private fun KanbanUiState.isDragging() = dragState.isDragging

    fun loadBoard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val boardResult = boardRepository.getBoard(boardId)
            val columnsResult = boardRepository.getColumns(boardId)
            val cardsResult = cardRepository.getBoardCards(boardId)
            Log.d(TAG, "loadBoard cardsResult: $cardsResult")

            when {
                boardResult is ApiResult.Success && columnsResult is ApiResult.Success -> {
                    // Get cards (may fail, that's ok - show empty)
                    val cards = if (cardsResult is ApiResult.Success) cardsResult.data else emptyList()
                    val columnsWithCards = distributeCardsToColumns(columnsResult.data, cards)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            board = boardResult.data,
                            columns = columnsWithCards
                        )
                    }
                }
                boardResult is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Failed to load board: ${boardResult.message}")
                    }
                }
                columnsResult is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Failed to load columns: ${(columnsResult as ApiResult.Error).message}")
                    }
                }
                else -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Network error")
                    }
                }
            }
        }
    }

    private fun distributeCardsToColumns(columns: List<Column>, cards: List<Card>): List<Column> {
        Log.d(TAG, "distributeCardsToColumns: ${cards.size} cards, ${columns.size} columns")
        Log.d(TAG, "Column IDs: ${columns.map { "${it.name}=${it.id}" }}")
        Log.d(TAG, "Card statuses: ${cards.map { "${it.title}→${it.status}" }}")

        // Group cards by status
        val deferredCards = cards.filter { it.status == CardStatus.DEFERRED }.sortedBy { it.position }
        val closedCards = cards.filter { it.status == CardStatus.CLOSED }.sortedBy { it.position }
        val activeCards = cards.filter { it.status == CardStatus.ACTIVE || it.status == CardStatus.TRIAGED }

        // Group active cards by column
        val triageCards = activeCards.filter { it.columnId.isEmpty() }.sortedBy { it.position }
        val cardsByColumn = activeCards.filter { it.columnId.isNotEmpty() }.groupBy { it.columnId }

        Log.d(TAG, "Cards by status - deferred: ${deferredCards.size}, triage: ${triageCards.size}, in columns: ${cardsByColumn.values.sumOf { it.size }}, closed: ${closedCards.size}")

        val result = mutableListOf<Column>()
        val defaultBoardId = columns.firstOrNull()?.boardId ?: boardId

        // NOT NOW swimlane (deferred cards) - always visible, position -2
        result += Column(
            id = "__not_now__",
            name = "Not Now",
            position = -2,
            boardId = defaultBoardId,
            cards = deferredCards
        )
        Log.d(TAG, "Added NOT NOW swimlane with ${deferredCards.size} cards")

        // Triage swimlane (active cards without column) - always visible, position -1
        result += Column(
            id = "",
            name = "Triage",
            position = -1,
            boardId = defaultBoardId,
            cards = triageCards
        )
        Log.d(TAG, "Added Triage swimlane with ${triageCards.size} cards")

        // User-created columns with active cards
        val columnsWithCards = columns.map { column ->
            val columnCards = cardsByColumn[column.id]?.sortedBy { it.position } ?: emptyList()
            Log.d(TAG, "Column '${column.name}' (${column.id}): ${columnCards.size} cards")
            column.copy(cards = columnCards)
        }
        result += columnsWithCards

        // DONE swimlane (closed cards) - always visible, position at end
        result += Column(
            id = "__done__",
            name = "Done",
            position = Int.MAX_VALUE,
            boardId = defaultBoardId,
            cards = closedCards
        )
        Log.d(TAG, "Added DONE swimlane with ${closedCards.size} cards")

        return result
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadBoardData()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private suspend fun loadBoardData() {
        val columnsResult = boardRepository.getColumns(boardId)
        val cardsResult = cardRepository.getBoardCards(boardId)

        if (columnsResult is ApiResult.Success) {
            val cards = if (cardsResult is ApiResult.Success) cardsResult.data else emptyList()
            val columnsWithCards = distributeCardsToColumns(columnsResult.data, cards)
            _uiState.update { it.copy(columns = columnsWithCards) }
        }
    }

    // Column operations
    fun showAddColumnDialog() {
        _uiState.update { it.copy(showAddColumnDialog = true) }
    }

    fun hideAddColumnDialog() {
        _uiState.update { it.copy(showAddColumnDialog = false) }
    }

    fun createColumn(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val position = _uiState.value.columns.maxOfOrNull { it.position }?.plus(1) ?: 0

            when (val result = boardRepository.createColumn(boardId, name, position)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            showAddColumnDialog = false,
                            columns = state.columns + result.data
                        )
                    }
                    _events.emit(KanbanEvent.ColumnCreated)
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(KanbanEvent.ShowError("Failed to create column"))
                }
                is ApiResult.Exception -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(KanbanEvent.ShowError("Network error"))
                }
            }
        }
    }

    fun showEditColumnDialog(column: Column) {
        _uiState.update { it.copy(editingColumn = column) }
    }

    fun hideEditColumnDialog() {
        _uiState.update { it.copy(editingColumn = null) }
    }

    fun updateColumn(columnId: String, name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = boardRepository.updateColumn(boardId, columnId, name, null)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            editingColumn = null,
                            columns = state.columns.map {
                                if (it.id == columnId) result.data else it
                            }
                        )
                    }
                    _events.emit(KanbanEvent.ColumnUpdated)
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(KanbanEvent.ShowError("Failed to update column"))
                }
                is ApiResult.Exception -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(KanbanEvent.ShowError("Network error"))
                }
            }
        }
    }

    fun deleteColumn(columnId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (boardRepository.deleteColumn(boardId, columnId)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            columns = state.columns.filter { it.id != columnId }
                        )
                    }
                    _events.emit(KanbanEvent.ColumnDeleted)
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(KanbanEvent.ShowError("Failed to delete column"))
                }
                is ApiResult.Exception -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(KanbanEvent.ShowError("Network error"))
                }
            }
        }
    }

    // Card operations
    fun showAddCardDialog(columnId: String) {
        _uiState.update { it.copy(showAddCardDialog = columnId) }
    }

    fun hideAddCardDialog() {
        _uiState.update { it.copy(showAddCardDialog = null) }
    }

    fun createCard(columnId: String, title: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = cardRepository.createCard(boardId, columnId, title, null)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            showAddCardDialog = null,
                            columns = state.columns.map { column ->
                                if (column.id == columnId) {
                                    column.copy(cards = column.cards + result.data)
                                } else column
                            }
                        )
                    }
                    _events.emit(KanbanEvent.CardCreated)
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(KanbanEvent.ShowError("Failed to create card"))
                }
                is ApiResult.Exception -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(KanbanEvent.ShowError("Network error"))
                }
            }
        }
    }

    // Drag and drop
    fun startDragging(card: Card) {
        _uiState.update {
            it.copy(
                dragState = DragState(
                    isDragging = true,
                    draggingCard = card,
                    sourceColumnId = card.columnId
                )
            )
        }
    }

    fun updateDragTarget(columnId: String, position: Int) {
        _uiState.update {
            it.copy(
                dragState = it.dragState.copy(
                    targetColumnId = columnId,
                    targetPosition = position
                )
            )
        }
    }

    fun endDragging() {
        val dragState = _uiState.value.dragState
        val card = dragState.draggingCard
        val targetColumnId = dragState.targetColumnId
        val targetPosition = dragState.targetPosition

        if (card != null && targetColumnId != null && targetPosition != null) {
            // Check if actually moved
            if (card.columnId != targetColumnId || card.position != targetPosition) {
                moveCard(card.id, targetColumnId, targetPosition)
            }
        }

        _uiState.update { it.copy(dragState = DragState()) }
    }

    fun cancelDragging() {
        _uiState.update { it.copy(dragState = DragState()) }
    }

    private fun moveCard(cardId: Long, columnId: String, position: Int) {
        viewModelScope.launch {
            // Optimistic update
            _uiState.update { state ->
                val card = state.columns.flatMap { it.cards }.find { it.id == cardId } ?: return@update state

                val updatedColumns = state.columns.map { column ->
                    when {
                        column.id == card.columnId && column.id != columnId -> {
                            // Remove from source column
                            column.copy(cards = column.cards.filter { it.id != cardId })
                        }
                        column.id == columnId -> {
                            // Add to target column
                            val cardsWithoutCard = column.cards.filter { it.id != cardId }
                            val updatedCard = card.copy(columnId = columnId, position = position)
                            val newCards = cardsWithoutCard.toMutableList().apply {
                                add(position.coerceIn(0, size), updatedCard)
                            }
                            column.copy(cards = newCards)
                        }
                        else -> column
                    }
                }

                state.copy(columns = updatedColumns)
            }

            // API call
            when (cardRepository.moveCard(cardId, columnId, position)) {
                is ApiResult.Success -> {
                    _events.emit(KanbanEvent.CardMoved)
                }
                is ApiResult.Error, is ApiResult.Exception -> {
                    // Rollback - reload data
                    loadBoardData()
                    _events.emit(KanbanEvent.ShowError("Failed to move card"))
                }
            }
        }
    }

    fun onCardClick(cardId: Long) {
        viewModelScope.launch {
            _events.emit(KanbanEvent.NavigateToCard(cardId))
        }
    }
}
