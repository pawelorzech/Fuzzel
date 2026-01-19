package com.fizzy.android.data.repository

import com.fizzy.android.core.network.ApiResult
import com.fizzy.android.data.api.FizzyApiService
import com.fizzy.android.data.api.dto.toDomain
import com.fizzy.android.domain.model.Notification
import com.fizzy.android.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val apiService: FizzyApiService
) : NotificationRepository {

    private val _unreadCount = MutableStateFlow(0)
    override val unreadCount: Flow<Int> = _unreadCount.asStateFlow()

    override suspend fun getNotifications(): ApiResult<List<Notification>> {
        return ApiResult.from {
            apiService.getNotifications()
        }.map { response ->
            _unreadCount.value = response.unreadCount
            response.notifications.map { it.toDomain() }
        }
    }

    override suspend fun markAsRead(notificationId: Long): ApiResult<Unit> {
        return ApiResult.from {
            apiService.markNotificationRead(notificationId.toString())
        }.map {
            _unreadCount.value = (_unreadCount.value - 1).coerceAtLeast(0)
        }
    }

    override suspend fun markAllAsRead(): ApiResult<Unit> {
        return ApiResult.from {
            apiService.markAllNotificationsRead()
        }.map {
            _unreadCount.value = 0
        }
    }
}
