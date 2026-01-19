package com.fizzy.android.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fizzy.android.core.ui.components.EmptyState
import com.fizzy.android.core.ui.components.ErrorMessage
import com.fizzy.android.core.ui.components.LoadingIndicator
import com.fizzy.android.domain.model.Notification
import com.fizzy.android.domain.model.NotificationType
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBackClick: () -> Unit,
    onNotificationClick: (Notification) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is NotificationsEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                is NotificationsEvent.NavigateToCard -> {
                    val notification = uiState.notifications.find { it.cardId == event.cardId }
                    notification?.let { onNotificationClick(it) }
                }
            }
        }
    }

    val unreadCount = uiState.notifications.count { !it.read }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    if (unreadCount > 0) {
                        TextButton(onClick = viewModel::markAllAsRead) {
                            Text("Mark all read")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.notifications.isEmpty() -> {
                    LoadingIndicator()
                }
                uiState.error != null && uiState.notifications.isEmpty() -> {
                    ErrorMessage(
                        message = uiState.error ?: "Unknown error",
                        onRetry = viewModel::loadNotifications
                    )
                }
                uiState.notifications.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.Notifications,
                        title = "No notifications",
                        description = "You're all caught up!"
                    )
                }
                else -> {
                    NotificationsList(
                        groupedNotifications = uiState.groupedNotifications,
                        onNotificationClick = { notification ->
                            viewModel.onNotificationClick(notification)
                        },
                        onMarkAsRead = viewModel::markAsRead
                    )
                }
            }

            // Show loading indicator when refreshing
            if (uiState.isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun NotificationsList(
    groupedNotifications: Map<LocalDate, List<Notification>>,
    onNotificationClick: (Notification) -> Unit,
    onMarkAsRead: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        groupedNotifications.forEach { (date, notifications) ->
            item(key = "header_$date") {
                DateHeader(date = date)
            }

            items(
                items = notifications,
                key = { it.id }
            ) { notification ->
                NotificationItem(
                    notification = notification,
                    onClick = { onNotificationClick(notification) },
                    onMarkAsRead = { onMarkAsRead(notification.id) }
                )
            }
        }
    }
}

@Composable
private fun DateHeader(date: LocalDate) {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    val dateText = when (date) {
        today -> "Today"
        yesterday -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }

    Text(
        text = dateText,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit,
    onMarkAsRead: () -> Unit
) {
    val icon = when (notification.type) {
        NotificationType.CARD_ASSIGNED -> Icons.Default.PersonAdd
        NotificationType.CARD_MENTIONED -> Icons.Default.AlternateEmail
        NotificationType.CARD_COMMENTED -> Icons.Default.Comment
        NotificationType.CARD_MOVED -> Icons.Default.MoveDown
        NotificationType.CARD_UPDATED -> Icons.Default.Edit
        NotificationType.STEP_COMPLETED -> Icons.Default.CheckCircle
        NotificationType.REACTION_ADDED -> Icons.Default.ThumbUp
        NotificationType.BOARD_SHARED -> Icons.Default.Share
        NotificationType.OTHER -> Icons.Default.Notifications
    }

    val iconColor = when (notification.type) {
        NotificationType.CARD_ASSIGNED -> MaterialTheme.colorScheme.primary
        NotificationType.CARD_MENTIONED -> MaterialTheme.colorScheme.secondary
        NotificationType.CARD_COMMENTED -> MaterialTheme.colorScheme.tertiary
        NotificationType.STEP_COMPLETED -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (!notification.read)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                else
                    MaterialTheme.colorScheme.surface
            )
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Unread indicator
        if (!notification.read) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Icon
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = iconColor.copy(alpha = 0.15f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = iconColor
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notification.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (!notification.read) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = notification.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (notification.actor != null) {
                    Surface(
                        modifier = Modifier.size(16.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = notification.actor.name.first().uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = notification.actor.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Text(
                    text = notification.createdAt
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Mark as read button (if unread)
        if (!notification.read) {
            IconButton(
                onClick = onMarkAsRead,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.MarkEmailRead,
                    contentDescription = "Mark as read",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }

    HorizontalDivider()
}
