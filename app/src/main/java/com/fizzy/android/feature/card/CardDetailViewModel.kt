package com.fizzy.android.feature.card

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fizzy.android.core.network.ApiResult
import com.fizzy.android.domain.model.*
import com.fizzy.android.domain.repository.BoardRepository
import com.fizzy.android.domain.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class CardDetailUiState(
    val card: Card? = null,
    val steps: List<Step> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val boardTags: List<Tag> = emptyList(),
    val boardUsers: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedTab: CardDetailTab = CardDetailTab.STEPS,
    val isEditing: Boolean = false,
    val editTitle: String = "",
    val editDescription: String = "",
    val showDatePicker: DatePickerAction? = null,
    val showTagPicker: Boolean = false,
    val showAssigneePicker: Boolean = false,
    val showAddStepDialog: Boolean = false,
    val showAddCommentDialog: Boolean = false,
    val editingStep: Step? = null,
    val showEmojiPicker: Long? = null // commentId
)

enum class CardDetailTab {
    STEPS, COMMENTS, ACTIVITY
}

enum class DatePickerAction {
    TRIAGE, DEFER
}

sealed class CardDetailEvent {
    data class ShowError(val message: String) : CardDetailEvent()
    data object CardUpdated : CardDetailEvent()
    data object CardClosed : CardDetailEvent()
    data object CardReopened : CardDetailEvent()
    data object NavigateBack : CardDetailEvent()
}

@HiltViewModel
class CardDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cardRepository: CardRepository,
    private val boardRepository: BoardRepository
) : ViewModel() {

    private val cardId: Long = checkNotNull(savedStateHandle["cardId"])

    private val _uiState = MutableStateFlow(CardDetailUiState())
    val uiState: StateFlow<CardDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CardDetailEvent>()
    val events: SharedFlow<CardDetailEvent> = _events.asSharedFlow()

    init {
        loadCard()
    }

    fun loadCard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = cardRepository.getCard(cardId)) {
                is ApiResult.Success -> {
                    val card = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            card = card,
                            editTitle = card.title,
                            editDescription = card.description ?: ""
                        )
                    }
                    loadCardDetails(card.boardId)
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Failed to load card")
                    }
                }
                is ApiResult.Exception -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Network error")
                    }
                }
            }
        }
    }

    private fun loadCardDetails(boardId: String) {
        viewModelScope.launch {
            // Load steps
            when (val result = cardRepository.getSteps(cardId)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(steps = result.data) }
                }
                else -> { /* Ignore */ }
            }

            // Load comments
            when (val result = cardRepository.getComments(cardId)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(comments = result.data) }
                }
                else -> { /* Ignore */ }
            }

            // Load board tags
            when (val result = boardRepository.getTags(boardId)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(boardTags = result.data) }
                }
                else -> { /* Ignore */ }
            }

            // Load board users
            when (val result = boardRepository.getBoardUsers(boardId)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(boardUsers = result.data) }
                }
                else -> { /* Ignore */ }
            }
        }
    }

    fun selectTab(tab: CardDetailTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    // Edit mode
    fun startEditing() {
        val card = _uiState.value.card ?: return
        _uiState.update {
            it.copy(
                isEditing = true,
                editTitle = card.title,
                editDescription = card.description ?: ""
            )
        }
    }

    fun cancelEditing() {
        _uiState.update { it.copy(isEditing = false) }
    }

    fun onTitleChange(title: String) {
        _uiState.update { it.copy(editTitle = title) }
    }

    fun onDescriptionChange(description: String) {
        _uiState.update { it.copy(editDescription = description) }
    }

    fun saveChanges() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val title = _uiState.value.editTitle.trim()
            val description = _uiState.value.editDescription.trim().takeIf { it.isNotEmpty() }

            when (val result = cardRepository.updateCard(cardId, title, description)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEditing = false,
                            card = result.data
                        )
                    }
                    _events.emit(CardDetailEvent.CardUpdated)
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(CardDetailEvent.ShowError("Failed to update card"))
                }
                is ApiResult.Exception -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(CardDetailEvent.ShowError("Network error"))
                }
            }
        }
    }

    // Card actions
    fun togglePriority() {
        viewModelScope.launch {
            val card = _uiState.value.card ?: return@launch
            when (val result = cardRepository.togglePriority(cardId, !card.priority)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(card = result.data) }
                }
                else -> _events.emit(CardDetailEvent.ShowError("Failed to update priority"))
            }
        }
    }

    fun toggleWatch() {
        viewModelScope.launch {
            val card = _uiState.value.card ?: return@launch
            when (val result = cardRepository.toggleWatch(cardId, !card.watching)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(card = result.data) }
                }
                else -> _events.emit(CardDetailEvent.ShowError("Failed to update watch status"))
            }
        }
    }

    fun closeCard() {
        viewModelScope.launch {
            when (val result = cardRepository.closeCard(cardId)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(card = result.data) }
                    _events.emit(CardDetailEvent.CardClosed)
                }
                else -> _events.emit(CardDetailEvent.ShowError("Failed to close card"))
            }
        }
    }

    fun reopenCard() {
        viewModelScope.launch {
            when (val result = cardRepository.reopenCard(cardId)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(card = result.data) }
                    _events.emit(CardDetailEvent.CardReopened)
                }
                else -> _events.emit(CardDetailEvent.ShowError("Failed to reopen card"))
            }
        }
    }

    fun showTriageDatePicker() {
        _uiState.update { it.copy(showDatePicker = DatePickerAction.TRIAGE) }
    }

    fun showDeferDatePicker() {
        _uiState.update { it.copy(showDatePicker = DatePickerAction.DEFER) }
    }

    fun hideDatePicker() {
        _uiState.update { it.copy(showDatePicker = null) }
    }

    fun triageCard(date: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(showDatePicker = null) }
            when (val result = cardRepository.triageCard(cardId, date)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(card = result.data) }
                }
                else -> _events.emit(CardDetailEvent.ShowError("Failed to triage card"))
            }
        }
    }

    fun deferCard(date: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(showDatePicker = null) }
            when (val result = cardRepository.deferCard(cardId, date)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(card = result.data) }
                }
                else -> _events.emit(CardDetailEvent.ShowError("Failed to defer card"))
            }
        }
    }

    fun deleteCard() {
        viewModelScope.launch {
            when (cardRepository.deleteCard(cardId)) {
                is ApiResult.Success -> {
                    _events.emit(CardDetailEvent.NavigateBack)
                }
                else -> _events.emit(CardDetailEvent.ShowError("Failed to delete card"))
            }
        }
    }

    // Tags
    fun showTagPicker() {
        _uiState.update { it.copy(showTagPicker = true) }
    }

    fun hideTagPicker() {
        _uiState.update { it.copy(showTagPicker = false) }
    }

    fun addTag(tagId: Long) {
        viewModelScope.launch {
            when (val result = cardRepository.addTag(cardId, tagId)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(card = result.data) }
                }
                else -> _events.emit(CardDetailEvent.ShowError("Failed to add tag"))
            }
        }
    }

    fun removeTag(tagId: Long) {
        viewModelScope.launch {
            when (val result = cardRepository.removeTag(cardId, tagId)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(card = result.data) }
                }
                else -> _events.emit(CardDetailEvent.ShowError("Failed to remove tag"))
            }
        }
    }

    // Assignees
    fun showAssigneePicker() {
        _uiState.update { it.copy(showAssigneePicker = true) }
    }

    fun hideAssigneePicker() {
        _uiState.update { it.copy(showAssigneePicker = false) }
    }

    fun addAssignee(userId: Long) {
        viewModelScope.launch {
            when (val result = cardRepository.addAssignee(cardId, userId)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(card = result.data) }
                }
                else -> _events.emit(CardDetailEvent.ShowError("Failed to add assignee"))
            }
        }
    }

    fun removeAssignee(userId: Long) {
        viewModelScope.launch {
            when (val result = cardRepository.removeAssignee(cardId, userId)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(card = result.data) }
                }
                else -> _events.emit(CardDetailEvent.ShowError("Failed to remove assignee"))
            }
        }
    }

    // Steps
    fun showAddStepDialog() {
        _uiState.update { it.copy(showAddStepDialog = true) }
    }

    fun hideAddStepDialog() {
        _uiState.update { it.copy(showAddStepDialog = false) }
    }

    fun createStep(description: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(showAddStepDialog = false) }
            when (val result = cardRepository.createStep(cardId, description)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(steps = state.steps + result.data)
                    }
                    refreshCard()
                }
                else -> _events.emit(CardDetailEvent.ShowError("Failed to create step"))
            }
        }
    }

    fun toggleStepCompleted(step: Step) {
        viewModelScope.launch {
            when (val result = cardRepository.updateStep(cardId, step.id, null, !step.completed, null)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(steps = state.steps.map {
                            if (it.id == step.id) result.data else it
                        })
                    }
                    refreshCard()
                }
                else -> _events.emit(CardDetailEvent.ShowError("Failed to update step"))
            }
        }
    }

    fun deleteStep(stepId: Long) {
        viewModelScope.launch {
            when (cardRepository.deleteStep(cardId, stepId)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(steps = state.steps.filter { it.id != stepId })
                    }
                    refreshCard()
                }
                else -> _events.emit(CardDetailEvent.ShowError("Failed to delete step"))
            }
        }
    }

    // Comments
    fun showAddCommentDialog() {
        _uiState.update { it.copy(showAddCommentDialog = true) }
    }

    fun hideAddCommentDialog() {
        _uiState.update { it.copy(showAddCommentDialog = false) }
    }

    fun createComment(content: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(showAddCommentDialog = false) }
            when (val result = cardRepository.createComment(cardId, content)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(comments = listOf(result.data) + state.comments)
                    }
                }
                else -> _events.emit(CardDetailEvent.ShowError("Failed to create comment"))
            }
        }
    }

    fun deleteComment(commentId: Long) {
        viewModelScope.launch {
            when (cardRepository.deleteComment(cardId, commentId)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(comments = state.comments.filter { it.id != commentId })
                    }
                }
                else -> _events.emit(CardDetailEvent.ShowError("Failed to delete comment"))
            }
        }
    }

    // Reactions
    fun showEmojiPicker(commentId: Long) {
        _uiState.update { it.copy(showEmojiPicker = commentId) }
    }

    fun hideEmojiPicker() {
        _uiState.update { it.copy(showEmojiPicker = null) }
    }

    fun addReaction(commentId: Long, emoji: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(showEmojiPicker = null) }
            when (val result = cardRepository.addReaction(cardId, commentId, emoji)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(comments = state.comments.map {
                            if (it.id == commentId) result.data else it
                        })
                    }
                }
                else -> _events.emit(CardDetailEvent.ShowError("Failed to add reaction"))
            }
        }
    }

    fun removeReaction(commentId: Long, emoji: String) {
        viewModelScope.launch {
            when (val result = cardRepository.removeReaction(cardId, commentId, emoji)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(comments = state.comments.map {
                            if (it.id == commentId) result.data else it
                        })
                    }
                }
                else -> _events.emit(CardDetailEvent.ShowError("Failed to remove reaction"))
            }
        }
    }

    private fun refreshCard() {
        viewModelScope.launch {
            when (val result = cardRepository.getCard(cardId)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(card = result.data) }
                }
                else -> { /* Ignore */ }
            }
        }
    }
}
