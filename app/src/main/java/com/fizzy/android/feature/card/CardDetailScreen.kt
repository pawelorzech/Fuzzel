package com.fizzy.android.feature.card

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fizzy.android.core.ui.components.ErrorMessage
import com.fizzy.android.core.ui.components.LoadingIndicator
import com.fizzy.android.core.ui.theme.FizzyGold
import com.fizzy.android.domain.model.*
import kotlinx.coroutines.flow.collectLatest
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    cardId: Long,
    onBackClick: () -> Unit,
    viewModel: CardDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is CardDetailEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                CardDetailEvent.CardUpdated -> snackbarHostState.showSnackbar("Card updated")
                CardDetailEvent.CardClosed -> snackbarHostState.showSnackbar("Card closed")
                CardDetailEvent.CardReopened -> snackbarHostState.showSnackbar("Card reopened")
                CardDetailEvent.NavigateBack -> onBackClick()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!uiState.isEditing) {
                        IconButton(onClick = viewModel::startEditing) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when {
            uiState.isLoading && uiState.card == null -> {
                LoadingIndicator(modifier = Modifier.padding(paddingValues))
            }
            uiState.error != null && uiState.card == null -> {
                ErrorMessage(
                    message = uiState.error ?: "Unknown error",
                    onRetry = viewModel::loadCard,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            uiState.card != null -> {
                if (uiState.isEditing) {
                    CardEditContent(
                        title = uiState.editTitle,
                        description = uiState.editDescription,
                        onTitleChange = viewModel::onTitleChange,
                        onDescriptionChange = viewModel::onDescriptionChange,
                        onSave = viewModel::saveChanges,
                        onCancel = viewModel::cancelEditing,
                        isLoading = uiState.isLoading,
                        modifier = Modifier.padding(paddingValues)
                    )
                } else {
                    CardDetailContent(
                        card = uiState.card!!,
                        steps = uiState.steps,
                        comments = uiState.comments,
                        boardTags = uiState.boardTags,
                        boardUsers = uiState.boardUsers,
                        selectedTab = uiState.selectedTab,
                        onTabSelect = viewModel::selectTab,
                        onTogglePriority = viewModel::togglePriority,
                        onToggleWatch = viewModel::toggleWatch,
                        onClose = viewModel::closeCard,
                        onReopen = viewModel::reopenCard,
                        onTriage = viewModel::showTriageDatePicker,
                        onDefer = viewModel::showDeferDatePicker,
                        onDelete = viewModel::deleteCard,
                        onAddTag = viewModel::showTagPicker,
                        onRemoveTag = viewModel::removeTag,
                        onAddAssignee = viewModel::showAssigneePicker,
                        onRemoveAssignee = viewModel::removeAssignee,
                        onAddStep = viewModel::showAddStepDialog,
                        onToggleStep = viewModel::toggleStepCompleted,
                        onDeleteStep = viewModel::deleteStep,
                        onAddComment = viewModel::showAddCommentDialog,
                        onDeleteComment = viewModel::deleteComment,
                        onAddReaction = viewModel::showEmojiPicker,
                        onRemoveReaction = viewModel::removeReaction,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }

    // Dialogs
    if (uiState.showAddStepDialog) {
        TextInputDialog(
            title = "Add Step",
            label = "Step description",
            onDismiss = viewModel::hideAddStepDialog,
            onConfirm = viewModel::createStep
        )
    }

    if (uiState.showAddCommentDialog) {
        TextInputDialog(
            title = "Add Comment",
            label = "Comment",
            multiline = true,
            onDismiss = viewModel::hideAddCommentDialog,
            onConfirm = viewModel::createComment
        )
    }

    if (uiState.showTagPicker) {
        TagPickerDialog(
            availableTags = uiState.boardTags,
            selectedTags = uiState.card?.tags ?: emptyList(),
            onDismiss = viewModel::hideTagPicker,
            onTagToggle = { tag ->
                if (uiState.card?.tags?.any { it.id == tag.id } == true) {
                    viewModel.removeTag(tag.id)
                } else {
                    viewModel.addTag(tag.id)
                }
            }
        )
    }

    if (uiState.showAssigneePicker) {
        AssigneePickerDialog(
            availableUsers = uiState.boardUsers,
            selectedUsers = uiState.card?.assignees ?: emptyList(),
            onDismiss = viewModel::hideAssigneePicker,
            onUserToggle = { user ->
                if (uiState.card?.assignees?.any { it.id == user.id } == true) {
                    viewModel.removeAssignee(user.id)
                } else {
                    viewModel.addAssignee(user.id)
                }
            }
        )
    }

    uiState.showEmojiPicker?.let { commentId ->
        EmojiPickerDialog(
            onDismiss = viewModel::hideEmojiPicker,
            onEmojiSelect = { emoji -> viewModel.addReaction(commentId, emoji) }
        )
    }

    uiState.showDatePicker?.let { action ->
        DatePickerDialog(
            title = if (action == DatePickerAction.TRIAGE) "Triage until" else "Defer until",
            onDismiss = viewModel::hideDatePicker,
            onDateSelect = { date ->
                if (action == DatePickerAction.TRIAGE) {
                    viewModel.triageCard(date)
                } else {
                    viewModel.deferCard(date)
                }
            }
        )
    }
}

@Composable
private fun CardDetailContent(
    card: Card,
    steps: List<Step>,
    comments: List<Comment>,
    boardTags: List<Tag>,
    boardUsers: List<User>,
    selectedTab: CardDetailTab,
    onTabSelect: (CardDetailTab) -> Unit,
    onTogglePriority: () -> Unit,
    onToggleWatch: () -> Unit,
    onClose: () -> Unit,
    onReopen: () -> Unit,
    onTriage: () -> Unit,
    onDefer: () -> Unit,
    onDelete: () -> Unit,
    onAddTag: () -> Unit,
    onRemoveTag: (Long) -> Unit,
    onAddAssignee: () -> Unit,
    onRemoveAssignee: (Long) -> Unit,
    onAddStep: () -> Unit,
    onToggleStep: (Step) -> Unit,
    onDeleteStep: (Long) -> Unit,
    onAddComment: () -> Unit,
    onDeleteComment: (Long) -> Unit,
    onAddReaction: (Long) -> Unit,
    onRemoveReaction: (Long, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Title and status
        Column(modifier = Modifier.padding(16.dp)) {
            if (card.status != CardStatus.ACTIVE) {
                StatusChip(status = card.status)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = card.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (!card.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = card.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider()

        // Action buttons
        CardActions(
            card = card,
            onTogglePriority = onTogglePriority,
            onToggleWatch = onToggleWatch,
            onClose = onClose,
            onReopen = onReopen,
            onTriage = onTriage,
            onDefer = onDefer,
            onDelete = onDelete
        )

        HorizontalDivider()

        // Tags section
        TagsSection(
            tags = card.tags,
            onAddTag = onAddTag,
            onRemoveTag = onRemoveTag
        )

        HorizontalDivider()

        // Assignees section
        AssigneesSection(
            assignees = card.assignees,
            onAddAssignee = onAddAssignee,
            onRemoveAssignee = onRemoveAssignee
        )

        HorizontalDivider()

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == CardDetailTab.STEPS,
                onClick = { onTabSelect(CardDetailTab.STEPS) },
                text = { Text("Steps (${steps.size})") }
            )
            Tab(
                selected = selectedTab == CardDetailTab.COMMENTS,
                onClick = { onTabSelect(CardDetailTab.COMMENTS) },
                text = { Text("Comments (${comments.size})") }
            )
        }

        // Tab content
        when (selectedTab) {
            CardDetailTab.STEPS -> StepsContent(
                steps = steps,
                onAddStep = onAddStep,
                onToggleStep = onToggleStep,
                onDeleteStep = onDeleteStep
            )
            CardDetailTab.COMMENTS -> CommentsContent(
                comments = comments,
                onAddComment = onAddComment,
                onDeleteComment = onDeleteComment,
                onAddReaction = onAddReaction,
                onRemoveReaction = onRemoveReaction
            )
            CardDetailTab.ACTIVITY -> {
                // Placeholder for activity log
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Activity log coming soon",
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: CardStatus) {
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
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun CardActions(
    card: Card,
    onTogglePriority: () -> Unit,
    onToggleWatch: () -> Unit,
    onClose: () -> Unit,
    onReopen: () -> Unit,
    onTriage: () -> Unit,
    onDefer: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ActionButton(
            icon = if (card.priority) Icons.Default.Star else Icons.Default.StarOutline,
            label = "Priority",
            tint = if (card.priority) FizzyGold else MaterialTheme.colorScheme.outline,
            onClick = onTogglePriority
        )

        ActionButton(
            icon = if (card.watching) Icons.Default.Visibility else Icons.Default.VisibilityOff,
            label = "Watch",
            tint = if (card.watching) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            onClick = onToggleWatch
        )

        if (card.status == CardStatus.ACTIVE) {
            ActionButton(
                icon = Icons.Default.Close,
                label = "Close",
                onClick = onClose
            )
        } else if (card.status == CardStatus.CLOSED) {
            ActionButton(
                icon = Icons.Default.Refresh,
                label = "Reopen",
                onClick = onReopen
            )
        }

        ActionButton(
            icon = Icons.Default.Schedule,
            label = "Triage",
            onClick = onTriage
        )

        ActionButton(
            icon = Icons.Default.EventBusy,
            label = "Defer",
            onClick = onDefer
        )

        ActionButton(
            icon = Icons.Default.Delete,
            label = "Delete",
            tint = MaterialTheme.colorScheme.error,
            onClick = onDelete
        )
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.outline,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }
}

@Composable
private fun TagsSection(
    tags: List<Tag>,
    onAddTag: () -> Unit,
    onRemoveTag: (Long) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tags",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            IconButton(onClick = onAddTag) {
                Icon(Icons.Default.Add, contentDescription = "Add tag", modifier = Modifier.size(20.dp))
            }
        }

        if (tags.isEmpty()) {
            Text(
                text = "No tags",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                tags.forEach { tag ->
                    InputChip(
                        selected = false,
                        onClick = { onRemoveTag(tag.id) },
                        label = { Text(tag.name) },
                        colors = InputChipDefaults.inputChipColors(
                            containerColor = tag.backgroundColor
                        ),
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                modifier = Modifier.size(16.dp),
                                tint = tag.textColor
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AssigneesSection(
    assignees: List<User>,
    onAddAssignee: () -> Unit,
    onRemoveAssignee: (Long) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Assignees",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            IconButton(onClick = onAddAssignee) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add assignee", modifier = Modifier.size(20.dp))
            }
        }

        if (assignees.isEmpty()) {
            Text(
                text = "No assignees",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                assignees.forEach { user ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = user.name.first().uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onRemoveAssignee(user.id) }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepsContent(
    steps: List<Step>,
    onAddStep: () -> Unit,
    onToggleStep: (Step) -> Unit,
    onDeleteStep: (Long) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        steps.forEach { step ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleStep(step) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = step.completed,
                    onCheckedChange = { onToggleStep(step) }
                )
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    color = if (step.completed)
                        MaterialTheme.colorScheme.outline
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = { onDeleteStep(step.id) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        TextButton(onClick = onAddStep, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Step")
        }
    }
}

@Composable
private fun CommentsContent(
    comments: List<Comment>,
    onAddComment: () -> Unit,
    onDeleteComment: (Long) -> Unit,
    onAddReaction: (Long) -> Unit,
    onRemoveReaction: (Long, String) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Button(
            onClick = onAddComment,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Comment")
        }

        Spacer(modifier = Modifier.height(16.dp))

        comments.forEach { comment ->
            CommentItem(
                comment = comment,
                onDelete = { onDeleteComment(comment.id) },
                onAddReaction = { onAddReaction(comment.id) },
                onRemoveReaction = { emoji -> onRemoveReaction(comment.id, emoji) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun CommentItem(
    comment: Comment,
    onDelete: () -> Unit,
    onAddReaction: () -> Unit,
    onRemoveReaction: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(28.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = comment.author.name.first().uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = comment.author.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = comment.createdAt.atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium
            )

            if (comment.reactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    comment.reactions.forEach { reaction ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (reaction.reactedByMe)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface,
                            modifier = Modifier.clickable {
                                if (reaction.reactedByMe) {
                                    onRemoveReaction(reaction.emoji)
                                } else {
                                    // Re-add same reaction - handled by API
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = reaction.emoji)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = reaction.count.toString(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            Row(modifier = Modifier.padding(top = 8.dp)) {
                TextButton(onClick = onAddReaction) {
                    Icon(Icons.Default.AddReaction, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("React")
                }
            }
        }
    }
}

@Composable
private fun CardEditContent(
    title: String,
    description: String,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Description") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                Text("Cancel")
            }

            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                enabled = title.isNotBlank() && !isLoading
            ) {
                Text("Save")
            }
        }
    }
}

// Dialogs
@Composable
private fun TextInputDialog(
    title: String,
    label: String,
    multiline: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = !multiline,
                minLines = if (multiline) 3 else 1
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
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

@Composable
private fun TagPickerDialog(
    availableTags: List<Tag>,
    selectedTags: List<Tag>,
    onDismiss: () -> Unit,
    onTagToggle: (Tag) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Tags") },
        text = {
            LazyColumn {
                items(availableTags) { tag ->
                    val isSelected = selectedTags.any { it.id == tag.id }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTagToggle(tag) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onTagToggle(tag) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = tag.backgroundColor
                        ) {
                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = tag.textColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun AssigneePickerDialog(
    availableUsers: List<User>,
    selectedUsers: List<User>,
    onDismiss: () -> Unit,
    onUserToggle: (User) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Assignees") },
        text = {
            LazyColumn {
                items(availableUsers) { user ->
                    val isSelected = selectedUsers.any { it.id == user.id }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUserToggle(user) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onUserToggle(user) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = user.name.first().uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun EmojiPickerDialog(
    onDismiss: () -> Unit,
    onEmojiSelect: (String) -> Unit
) {
    val commonEmojis = listOf(
        "\uD83D\uDC4D", "\uD83D\uDC4E", "\u2764\uFE0F", "\uD83D\uDE00",
        "\uD83E\uDD14", "\uD83D\uDE4C", "\uD83D\uDE80", "\uD83C\uDF89",
        "\uD83D\uDD25", "\uD83D\uDC40", "\uD83D\uDC4F", "\uD83D\uDE4F"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Reaction") },
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                commonEmojis.chunked(4).forEach { row ->
                    Column {
                        row.forEach { emoji ->
                            TextButton(onClick = { onEmojiSelect(emoji) }) {
                                Text(emoji, style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    title: String,
    onDismiss: () -> Unit,
    onDateSelect: (java.time.LocalDate) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis() + 86400000 // Tomorrow
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onDateSelect(date)
                    }
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            title = { Text(title, modifier = Modifier.padding(16.dp)) }
        )
    }
}
