package com.fizzy.android.domain.repository

import com.fizzy.android.core.network.ApiResult
import com.fizzy.android.domain.model.Notification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    val unreadCount: Flow<Int>

    suspend fun getNotifications(): ApiResult<List<Notification>>
    suspend fun markAsRead(notificationId: Long): ApiResult<Unit>
    suspend fun markAllAsRead(): ApiResult<Unit>
}
