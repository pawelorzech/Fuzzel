package com.fizzy.android.feature.boards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fizzy.android.core.ui.components.EmptyState
import com.fizzy.android.core.ui.components.ErrorMessage
import com.fizzy.android.core.ui.components.LoadingIndicator
import com.fizzy.android.domain.model.Board
import kotlinx.coroutines.flow.collectLatest
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardListScreen(
    onBoardClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: BoardListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val unreadCount by viewModel.unreadNotificationsCount.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSearch by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is BoardListEvent.NavigateToBoard -> onBoardClick(event.boardId)
                is BoardListEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                BoardListEvent.BoardCreated -> snackbarHostState.showSnackbar("Board created")
                BoardListEvent.BoardUpdated -> snackbarHostState.showSnackbar("Board updated")
                BoardListEvent.BoardDeleted -> snackbarHostState.showSnackbar("Board deleted")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            placeholder = { Text("Search boards...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    } else {
                        Text("Boards")
                    }
                },
                actions = {
                    if (showSearch) {
                        IconButton(onClick = {
                            showSearch = false
                            viewModel.clearSearch()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close search")
                        }
                    } else {
                        IconButton(onClick = { showSearch = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }

                        IconButton(onClick = viewModel::refresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }

                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge { Text(unreadCount.toString()) }
                                }
                            }
                        ) {
                            IconButton(onClick = onNotificationsClick) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                            }
                        }

                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create board")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.boards.isEmpty() -> {
                    LoadingIndicator()
                }
                uiState.error != null && uiState.boards.isEmpty() -> {
                    ErrorMessage(
                        message = uiState.error ?: "Unknown error",
                        onRetry = viewModel::loadBoards
                    )
                }
                uiState.filteredBoards.isEmpty() && uiState.searchQuery.isNotEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.SearchOff,
                        title = "No boards found",
                        description = "Try a different search term"
                    )
                }
                uiState.boards.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.ViewKanban,
                        title = "No boards yet",
                        description = "Create your first board to get started",
                        action = {
                            Button(onClick = { viewModel.showCreateDialog() }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create Board")
                            }
                        }
                    )
                }
                else -> {
                    BoardList(
                        boards = uiState.filteredBoards,
                        onBoardClick = onBoardClick,
                        onEditClick = viewModel::showEditDialog,
                        onDeleteClick = viewModel::showDeleteConfirmation
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

    // Create Board Dialog
    if (uiState.showCreateDialog) {
        BoardDialog(
            title = "Create Board",
            initialName = "",
            initialDescription = "",
            onDismiss = viewModel::hideCreateDialog,
            onConfirm = { name, description ->
                viewModel.createBoard(name, description)
            },
            isLoading = uiState.isLoading
        )
    }

    // Edit Board Dialog
    uiState.showEditDialog?.let { board ->
        BoardDialog(
            title = "Edit Board",
            initialName = board.name,
            initialDescription = board.description ?: "",
            onDismiss = viewModel::hideEditDialog,
            onConfirm = { name, description ->
                viewModel.updateBoard(board.id, name, description)
            },
            isLoading = uiState.isLoading
        )
    }

    // Delete Confirmation Dialog
    uiState.showDeleteConfirmation?.let { board ->
        AlertDialog(
            onDismissRequest = viewModel::hideDeleteConfirmation,
            title = { Text("Delete Board") },
            text = { Text("Are you sure you want to delete \"${board.name}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteBoard(board.id) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideDeleteConfirmation) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BoardList(
    boards: List<Board>,
    onBoardClick: (String) -> Unit,
    onEditClick: (Board) -> Unit,
    onDeleteClick: (Board) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(boards, key = { it.id }) { board ->
            BoardCard(
                board = board,
                onClick = { onBoardClick(board.id) },
                onEditClick = { onEditClick(board) },
                onDeleteClick = { onDeleteClick(board) }
            )
        }
    }
}

@Composable
private fun BoardCard(
    board: Board,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = board.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More options"
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
                                onEditClick()
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
                                onDeleteClick()
                            }
                        )
                    }
                }
            }

            if (!board.description.isNullOrBlank()) {
                Text(
                    text = board.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ViewColumn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${board.columnsCount} columns",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CreditCard,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${board.cardsCount} cards",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = (board.updatedAt ?: board.createdAt).atZone(java.time.ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun BoardDialog(
    title: String,
    initialName: String,
    initialDescription: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String?) -> Unit,
    isLoading: Boolean
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    enabled = !isLoading
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, description) },
                enabled = name.isNotBlank() && !isLoading
            ) {
                Text(if (initialName.isEmpty()) "Create" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}
