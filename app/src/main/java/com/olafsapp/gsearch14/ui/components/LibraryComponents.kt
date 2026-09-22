package com.olafsapp.gsearch14.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.olafsapp.gsearch14.R
import com.olafsapp.gsearch14.core.pressable
import com.olafsapp.gsearch14.data.SearchEngine
import com.olafsapp.gsearch14.data.SearchVertical
import com.olafsapp.gsearch14.data.model.Bookmark
import com.olafsapp.gsearch14.data.model.HistoryEntry
import com.olafsapp.gsearch14.ui.theme.Motion
import java.text.DateFormat
import java.util.Date

/**
 * Wraps a row in swipe-to-delete.
 *
 * The delete affordance grows with the swipe distance instead of appearing at full strength
 * straight away, so a half-committed gesture reads as "not yet". Deletion is undoable via a
 * snackbar, which is why there is no confirmation dialog here — version 3 asked for
 * confirmation on every single swipe, which made the gesture pointless.
 */
@Composable
fun SwipeToDelete(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState(
        positionalThreshold = { distance -> distance * 0.4f },
    )

    LaunchedEffect(state.currentValue) {
        if (state.currentValue != SwipeToDismissBoxValue.Settled) {
            onDelete()
            state.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        backgroundContent = {
            val progress = state.progress.coerceIn(0f, 1f)
            val active = state.targetValue != SwipeToDismissBoxValue.Settled
            val background by animateColorAsState(
                targetValue = if (active) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                animationSpec = Motion.effectsFast(),
                label = "swipeBackground",
            )
            val iconScale by animateFloatAsState(
                targetValue = if (active) 1f else 0.7f,
                animationSpec = Motion.spatialFast(),
                label = "swipeIconScale",
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.large)
                    .background(background)
                    .padding(horizontal = 22.dp),
                contentAlignment = when (state.dismissDirection) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    else -> Alignment.CenterEnd
                },
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = MaterialTheme.colorScheme.onErrorContainer.copy(
                        alpha = 0.4f + 0.6f * progress,
                    ),
                    modifier = Modifier.scale(iconScale),
                )
            }
        },
        content = { content() },
    )
}

private fun verticalIcon(vertical: SearchVertical) = when (vertical) {
    SearchVertical.IMAGES -> Icons.Rounded.Image
    SearchVertical.VIDEOS -> Icons.Rounded.Videocam
    SearchVertical.NEWS -> Icons.Rounded.Newspaper
    SearchVertical.WEB -> Icons.Rounded.Search
}

/** One past search. */
@Composable
fun HistoryRow(
    entry: HistoryEntry,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val engine = remember(entry.engineId) { SearchEngine.byId(entry.engineId) }
    val timestamp = remember(entry.timestamp) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(entry.timestamp))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .pressable(onClick = onClick, onLongClick = onTogglePin)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EngineBadge(engine = engine, size = 36.dp)
        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = entry.query,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = verticalIcon(entry.vertical),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = stringResource(entry.vertical.labelRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "  ·  $timestamp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // A pin is a state, not an action, so it reads as a quiet marker rather than a
        // button; long-pressing the row toggles it.
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(MaterialTheme.shapes.small)
                .pressable(onClick = onTogglePin),
            contentAlignment = Alignment.Center,
        ) {
            val pinTint by animateColorAsState(
                targetValue = if (entry.pinned) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                },
                animationSpec = Motion.effectsDefault(),
                label = "pinTint",
            )
            val pinScale by animateFloatAsState(
                targetValue = if (entry.pinned) 1f else 0.85f,
                animationSpec = Motion.spatialBouncy(),
                label = "pinScale",
            )
            Icon(
                imageVector = Icons.Rounded.PushPin,
                contentDescription = stringResource(
                    if (entry.pinned) R.string.history_unpin else R.string.history_pin,
                ),
                tint = pinTint,
                modifier = Modifier
                    .size(17.dp)
                    .scale(pinScale),
            )
        }
    }
}

/** One saved page. */
@Composable
fun BookmarkRow(
    bookmark: Bookmark,
    host: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val engine = remember(bookmark.engineId) { SearchEngine.byId(bookmark.engineId) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .pressable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EngineBadge(engine = engine, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = bookmark.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (host.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = host,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Small pill used for the incognito notice and similar inline hints. */
@Composable
fun InlineNotice(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}
