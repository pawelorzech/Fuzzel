package com.fizzy.android.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fizzy.android.core.network.ApiResult
import com.fizzy.android.domain.model.Notification
import com.fizzy.android.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class NotificationsUiState(
    val notifications: List<Notification> = emptyList(),
    val groupedNotifications: Map<LocalDate, List<Notification>> = emptyMap(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

sealed class NotificationsEvent {
    data class ShowError(val message: String) : NotificationsEvent()
    data class NavigateToCard(val cardId: Long) : NotificationsEvent()
}

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<NotificationsEvent>()
    val events: SharedFlow<NotificationsEvent> = _events.asSharedFlow()

    private var pollingJob: Job? = null

    init {
        loadNotifications()
        startPolling()
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }

    private fun startPolling() {
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(30_000) // Poll every 30 seconds
                silentRefresh()
            }
        }
    }

    private suspend fun silentRefresh() {
        when (val result = notificationRepository.getNotifications()) {
            is ApiResult.Success -> {
                updateNotifications(result.data)
            }
            else -> { /* Ignore silent refresh failures */ }
        }
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = notificationRepository.getNotifications()) {
                is ApiResult.Success -> {
                    updateNotifications(result.data)
                    _uiState.update { it.copy(isLoading = false) }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Failed to load notifications")
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

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            when (val result = notificationRepository.getNotifications()) {
                is ApiResult.Success -> {
                    updateNotifications(result.data)
                }
                else -> { /* Ignore */ }
            }

            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun updateNotifications(notifications: List<Notification>) {
        val grouped = notifications.groupBy { notification ->
            notification.createdAt
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }.toSortedMap(compareByDescending { it })

        _uiState.update {
            it.copy(
                notifications = notifications,
                groupedNotifications = grouped
            )
        }
    }

    fun markAsRead(notificationId: Long) {
        viewModelScope.launch {
            when (notificationRepository.markAsRead(notificationId)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        val updated = state.notifications.map { notification ->
                            if (notification.id == notificationId) {
                                notification.copy(read = true)
                            } else notification
                        }
                        val grouped = updated.groupBy { notification ->
                            notification.createdAt
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        }.toSortedMap(compareByDescending { it })

                        state.copy(
                            notifications = updated,
                            groupedNotifications = grouped
                        )
                    }
                }
                else -> _events.emit(NotificationsEvent.ShowError("Failed to mark as read"))
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            when (notificationRepository.markAllAsRead()) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        val updated = state.notifications.map { it.copy(read = true) }
                        val grouped = updated.groupBy { notification ->
                            notification.createdAt
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        }.toSortedMap(compareByDescending { it })

                        state.copy(
                            notifications = updated,
                            groupedNotifications = grouped
                        )
                    }
                }
                else -> _events.emit(NotificationsEvent.ShowError("Failed to mark all as read"))
            }
        }
    }

    fun onNotificationClick(notification: Notification) {
        viewModelScope.launch {
            // Mark as read
            if (!notification.read) {
                markAsRead(notification.id)
            }

            // Navigate to card if applicable
            notification.cardId?.let { cardId ->
                _events.emit(NotificationsEvent.NavigateToCard(cardId))
            }
        }
    }
}
