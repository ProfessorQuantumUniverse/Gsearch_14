package com.olafsapp.gsearch14.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.olafsapp.gsearch14.R
import com.olafsapp.gsearch14.core.UrlLauncher
import com.olafsapp.gsearch14.core.pressable
import com.olafsapp.gsearch14.data.model.Bookmark
import com.olafsapp.gsearch14.data.model.HistoryEntry
import com.olafsapp.gsearch14.ui.components.BookmarkRow
import com.olafsapp.gsearch14.ui.components.EmptyState
import com.olafsapp.gsearch14.ui.components.HistoryRow
import com.olafsapp.gsearch14.ui.components.SectionHeader
import com.olafsapp.gsearch14.ui.components.SwipeToDelete
import com.olafsapp.gsearch14.ui.theme.Motion
import kotlinx.coroutines.launch

private enum class LibraryTab { HISTORY, BOOKMARKS }

/**
 * History and bookmarks.
 *
 * Deleting is a swipe plus an undo snackbar rather than a confirmation dialog, which is
 * both faster and more forgiving than version 3's "are you sure?" on every single entry.
 */
@Composable
fun LibraryScreen(
    history: List<HistoryEntry>,
    bookmarks: List<Bookmark>,
    onBack: () -> Unit,
    onReplay: (HistoryEntry) -> Unit,
    onTogglePin: (HistoryEntry) -> Unit,
    onDeleteHistoryEntry: (HistoryEntry) -> Unit,
    onRestoreHistoryEntry: (HistoryEntry) -> Unit,
    onClearHistory: () -> Unit,
    onOpenBookmark: (Bookmark) -> Unit,
    onDeleteBookmark: (Bookmark) -> Unit,
    onRestoreBookmark: (Bookmark) -> Unit,
    onClearBookmarks: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableStateOf(LibraryTab.HISTORY) }
    var filter by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val undoLabel = stringResource(R.string.action_undo)
    val historyClearedMessage = stringResource(R.string.history_cleared)
    val bookmarkRemovedMessage = stringResource(R.string.bookmark_removed)

    BackHandler(onBack = onBack)

    val visibleHistory = remember(history, filter) {
        val needle = filter.trim()
        val matching = if (needle.isEmpty()) {
            history
        } else {
            history.filter { it.query.contains(needle, ignoreCase = true) }
        }
        matching.filter { it.pinned } + matching.filterNot { it.pinned }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding()),
        ) {
            LibraryHeader(
                tab = tab,
                onTabChange = { tab = it },
                onBack = onBack,
                onClearAll = { confirmClear = true },
                clearEnabled = when (tab) {
                    LibraryTab.HISTORY -> history.any { !it.pinned }
                    LibraryTab.BOOKMARKS -> bookmarks.isNotEmpty()
                },
            )

            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    val width = if (forward) 1 else -1
                    (
                        slideInHorizontally(Motion.offsetSpring) { it / 6 * width } +
                            fadeIn(Motion.effectsFast())
                        ).togetherWith(
                        slideOutHorizontally(Motion.offsetSpring) { -it / 6 * width } +
                            fadeOut(Motion.effectsFast()),
                    )
                },
                label = "libraryTab",
            ) { current ->
                when (current) {
                    LibraryTab.HISTORY -> HistoryList(
                        entries = visibleHistory,
                        filter = filter,
                        onFilterChange = { filter = it },
                        onReplay = onReplay,
                        onTogglePin = onTogglePin,
                        onDelete = { entry ->
                            onDeleteHistoryEntry(entry)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = entry.query,
                                    actionLabel = undoLabel,
                                    duration = SnackbarDuration.Short,
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    onRestoreHistoryEntry(entry)
                                }
                            }
                        },
                    )

                    LibraryTab.BOOKMARKS -> BookmarkList(
                        bookmarks = bookmarks,
                        onOpen = onOpenBookmark,
                        onDelete = { bookmark ->
                            onDeleteBookmark(bookmark)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = bookmarkRemovedMessage,
                                    actionLabel = undoLabel,
                                    duration = SnackbarDuration.Short,
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    onRestoreBookmark(bookmark)
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = {
                Text(
                    stringResource(
                        if (tab == LibraryTab.HISTORY) {
                            R.string.history_clear_confirm_title
                        } else {
                            R.string.bookmarks_clear
                        },
                    ),
                )
            },
            text = {
                if (tab == LibraryTab.HISTORY) {
                    Text(stringResource(R.string.history_clear_confirm_body))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    if (tab == LibraryTab.HISTORY) {
                        onClearHistory()
                        scope.launch { snackbarHostState.showSnackbar(historyClearedMessage) }
                    } else {
                        onClearBookmarks()
                    }
                }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun LibraryHeader(
    tab: LibraryTab,
    onTabChange: (LibraryTab) -> Unit,
    onBack: () -> Unit,
    onClearAll: () -> Unit,
    clearEnabled: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .pressable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.results_back),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(21.dp),
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.library_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .pressable(onClick = onClearAll, enabled = clearEnabled),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteSweep,
                    contentDescription = stringResource(R.string.history_clear),
                    tint = if (clearEnabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    },
                    modifier = Modifier.size(21.dp),
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            LibraryTab.entries.forEach { entry ->
                val selected = entry == tab
                val container by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.surfaceContainerLowest
                    } else {
                        Color.Transparent
                    },
                    animationSpec = Motion.effectsDefault(),
                    label = "tabContainer",
                )
                val content by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = Motion.effectsDefault(),
                    label = "tabContent",
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(container)
                        .pressable(onClick = { onTabChange(entry) }),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (entry == LibraryTab.HISTORY) {
                            Icons.Rounded.History
                        } else {
                            Icons.Rounded.BookmarkBorder
                        },
                        contentDescription = null,
                        tint = content,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = stringResource(
                            if (entry == LibraryTab.HISTORY) {
                                R.string.tab_history
                            } else {
                                R.string.tab_bookmarks
                            },
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = content,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryList(
    entries: List<HistoryEntry>,
    filter: String,
    onFilterChange: (String) -> Unit,
    onReplay: (HistoryEntry) -> Unit,
    onTogglePin: (HistoryEntry) -> Unit,
    onDelete: (HistoryEntry) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "filter") {
            FilterField(value = filter, onValueChange = onFilterChange)
        }

        if (entries.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Rounded.History,
                    title = stringResource(R.string.history_empty_title),
                    body = stringResource(R.string.history_empty_body),
                )
            }
        }

        val pinned = entries.filter { it.pinned }
        val rest = entries.filterNot { it.pinned }

        if (pinned.isNotEmpty()) {
            item(key = "pinned-header") {
                SectionHeader(text = stringResource(R.string.history_pinned_section))
            }
            items(pinned, key = { it.id }) { entry ->
                HistoryListItem(entry, onReplay, onTogglePin, onDelete)
            }
            if (rest.isNotEmpty()) {
                item(key = "recent-header") {
                    SectionHeader(text = stringResource(R.string.history_title))
                }
            }
        }

        items(rest, key = { it.id }) { entry ->
            HistoryListItem(entry, onReplay, onTogglePin, onDelete)
        }
    }
}

/** One history row: swipe to delete, tap to replay, long-press or tap the pin to pin. */
@Composable
private fun androidx.compose.foundation.lazy.LazyItemScope.HistoryListItem(
    entry: HistoryEntry,
    onReplay: (HistoryEntry) -> Unit,
    onTogglePin: (HistoryEntry) -> Unit,
    onDelete: (HistoryEntry) -> Unit,
) {
    // animateItem keeps neighbours sliding smoothly into the gap a deletion leaves.
    Box(Modifier.animateItem()) {
        SwipeToDelete(onDelete = { onDelete(entry) }) {
            HistoryRow(
                entry = entry,
                onClick = { onReplay(entry) },
                onTogglePin = { onTogglePin(entry) },
            )
        }
    }
}

@Composable
private fun BookmarkList(
    bookmarks: List<Bookmark>,
    onOpen: (Bookmark) -> Unit,
    onDelete: (Bookmark) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (bookmarks.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Rounded.BookmarkBorder,
                    title = stringResource(R.string.bookmarks_empty_title),
                    body = stringResource(R.string.bookmarks_empty_body),
                )
            }
        }

        items(bookmarks, key = { it.id }) { bookmark ->
            Box(Modifier.animateItem()) {
                SwipeToDelete(onDelete = { onDelete(bookmark) }) {
                    BookmarkRow(
                        bookmark = bookmark,
                        host = UrlLauncher.hostLabel(bookmark.url),
                        onClick = { onOpen(bookmark) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterField(value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = stringResource(R.string.history_filter_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
