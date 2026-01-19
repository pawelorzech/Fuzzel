package com.fizzy.android.feature.kanban

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fizzy.android.core.ui.components.ErrorMessage
import com.fizzy.android.core.ui.components.LoadingIndicator
import com.fizzy.android.core.ui.theme.FizzyGold
import com.fizzy.android.domain.model.Card
import com.fizzy.android.domain.model.CardStatus
import com.fizzy.android.domain.model.Column
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanbanScreen(
    boardId: String,
    onBackClick: () -> Unit,
    onCardClick: (Long) -> Unit,
    viewModel: KanbanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is KanbanEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                is KanbanEvent.NavigateToCard -> onCardClick(event.cardId)
                KanbanEvent.ColumnCreated -> snackbarHostState.showSnackbar("Column created")
                KanbanEvent.ColumnUpdated -> snackbarHostState.showSnackbar("Column updated")
                KanbanEvent.ColumnDeleted -> snackbarHostState.showSnackbar("Column deleted")
                KanbanEvent.CardCreated -> snackbarHostState.showSnackbar("Card created")
                KanbanEvent.CardMoved -> { /* Silent */ }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.board?.name ?: "Board") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = viewModel::showAddColumnDialog) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Add column")
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
                uiState.isLoading && uiState.columns.isEmpty() -> {
                    LoadingIndicator()
                }
                uiState.error != null && uiState.columns.isEmpty() -> {
                    ErrorMessage(
                        message = uiState.error ?: "Unknown error",
                        onRetry = viewModel::loadBoard
                    )
                }
                else -> {
                    KanbanBoard(
                        columns = uiState.columns,
                        dragState = uiState.dragState,
                        onCardClick = viewModel::onCardClick,
                        onCardLongPress = viewModel::startDragging,
                        onDragEnd = viewModel::endDragging,
                        onDragCancel = viewModel::cancelDragging,
                        onDragTargetUpdate = viewModel::updateDragTarget,
                        onAddCard = viewModel::showAddCardDialog,
                        onEditColumn = viewModel::showEditColumnDialog,
                        onDeleteColumn = viewModel::deleteColumn
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

    // Add Column Dialog
    if (uiState.showAddColumnDialog) {
        ColumnDialog(
            title = "Add Column",
            initialName = "",
            onDismiss = viewModel::hideAddColumnDialog,
            onConfirm = viewModel::createColumn
        )
    }

    // Edit Column Dialog
    uiState.editingColumn?.let { column ->
        ColumnDialog(
            title = "Edit Column",
            initialName = column.name,
            onDismiss = viewModel::hideEditColumnDialog,
            onConfirm = { name -> viewModel.updateColumn(column.id, name) }
        )
    }

    // Add Card Dialog
    uiState.showAddCardDialog?.let { columnId ->
        CardQuickAddDialog(
            onDismiss = viewModel::hideAddCardDialog,
            onConfirm = { title -> viewModel.createCard(columnId, title) }
        )
    }
}

@Composable
private fun KanbanBoard(
    columns: List<Column>,
    dragState: DragState,
    onCardClick: (Long) -> Unit,
    onCardLongPress: (Card) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onDragTargetUpdate: (String, Int) -> Unit,
    onAddCard: (String) -> Unit,
    onEditColumn: (Column) -> Unit,
    onDeleteColumn: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(scrollState)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        columns.forEach { column ->
            KanbanColumn(
                column = column,
                isDragTarget = dragState.targetColumnId == column.id,
                onCardClick = onCardClick,
                onCardLongPress = onCardLongPress,
                onDragEnd = onDragEnd,
                onDragCancel = onDragCancel,
                onDragTargetUpdate = { position -> onDragTargetUpdate(column.id, position) },
                onAddCard = { onAddCard(column.id) },
                onEditColumn = { onEditColumn(column) },
                onDeleteColumn = { onDeleteColumn(column.id) },
                draggingCard = if (dragState.sourceColumnId == column.id) dragState.draggingCard else null
            )
        }

        // Add column button at the end
        AddColumnButton(onClick = { /* Handled by FAB */ })
    }
}

@Composable
private fun KanbanColumn(
    column: Column,
    isDragTarget: Boolean,
    onCardClick: (Long) -> Unit,
    onCardLongPress: (Card) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onDragTargetUpdate: (Int) -> Unit,
    onAddCard: () -> Unit,
    onEditColumn: () -> Unit,
    onDeleteColumn: () -> Unit,
    draggingCard: Card?
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragTarget)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Column Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = column.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = column.cards.size.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Column options",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onEditColumn()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDeleteColumn()
                            }
                        )
                    }
                }
            }

            // Cards List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(column.cards, key = { _, card -> card.id }) { index, card ->
                    KanbanCard(
                        card = card,
                        isDragging = draggingCard?.id == card.id,
                        onClick = { onCardClick(card.id) },
                        onLongPress = { onCardLongPress(card) }
                    )
                }
            }

            // Add Card Button
            TextButton(
                onClick = onAddCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Card")
            }
        }
    }
}

@Composable
private fun KanbanCard(
    card: Card,
    isDragging: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                if (isDragging) {
                    alpha = 0.5f
                }
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onLongPress() },
                    onDragEnd = { },
                    onDragCancel = { },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                )
            }
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Priority indicator
            if (card.priority) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Priority",
                        modifier = Modifier.size(14.dp),
                        tint = FizzyGold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Priority",
                        style = MaterialTheme.typography.labelSmall,
                        color = FizzyGold
                    )
                }
            }

            // Title
            Text(
                text = card.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Tags
            if (card.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    card.tags.take(3).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = tag.backgroundColor
                        ) {
                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = tag.textColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (card.tags.size > 3) {
                        Text(
                            text = "+${card.tags.size - 3}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Bottom indicators
            if (card.hasSteps || card.commentsCount > 0 || card.assignees.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Steps progress
                    if (card.hasSteps) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckBox,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (card.stepsCompleted == card.stepsTotal)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = card.stepsDisplay,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // Comments count
                    if (card.commentsCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = card.commentsCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Assignees avatars
                    if (card.assignees.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy((-8).dp)
                        ) {
                            card.assignees.take(3).forEach { user ->
                                Surface(
                                    modifier = Modifier.size(24.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = user.name.first().uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Status badge
            if (card.status != CardStatus.ACTIVE) {
                Spacer(modifier = Modifier.height(8.dp))
                StatusBadge(status = card.status)
            }
        }
    }
}

@Composable
private fun StatusBadge(status: CardStatus) {
    val (text, color) = when (status) {
        CardStatus.CLOSED -> "Closed" to MaterialTheme.colorScheme.error
        CardStatus.TRIAGED -> "Triaged" to Color(0xFFF97316)
        CardStatus.DEFERRED -> "Deferred" to Color(0xFF8B5CF6)
        CardStatus.ACTIVE -> return
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun AddColumnButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(100.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add Column",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun ColumnDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Column name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text(if (initialName.isEmpty()) "Create" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CardQuickAddDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Card") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Card title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title) },
                enabled = title.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
