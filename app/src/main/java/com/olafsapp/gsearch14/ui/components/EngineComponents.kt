package com.olafsapp.gsearch14.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.olafsapp.gsearch14.R
import com.olafsapp.gsearch14.core.pressable
import com.olafsapp.gsearch14.data.SearchEngine
import com.olafsapp.gsearch14.ui.theme.Motion

/**
 * The engine's monogram on its brand colour, cut to the app's signature heptagon.
 *
 * Using a drawn monogram rather than bundled brand logos keeps the app free of third-party
 * marks while still making each engine instantly recognisable by colour.
 */
@Composable
fun EngineBadge(
    engine: SearchEngine,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    selected: Boolean = true,
) {
    val shape = remember { PolygonShape(Polygons.badge) }
    val container by animateColorAsState(
        targetValue = if (selected) {
            engine.brandColor
        } else {
            // Enough brand colour left to tell the engines apart at a glance, but clearly
            // subordinate to the selected one.
            engine.brandColor.copy(alpha = 0.32f)
        },
        animationSpec = Motion.effectsDefault(),
        label = "badgeContainer",
    )
    val contentColor = if (selected) {
        if (engine.brandColor.luminance() > 0.55f) Color.Black else Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = engine.monogram,
            color = contentColor,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * The horizontal engine rail.
 *
 * The selected engine grows a label beside its badge; the others stay as bare badges. The
 * expansion and the colour shift are what make switching feel like a choice rather than a
 * radio button.
 */
@Composable
fun EngineRail(
    engines: List<SearchEngine>,
    selected: SearchEngine,
    onSelect: (SearchEngine) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
) {
    val listState = rememberLazyListState()
    val selectedIndex = engines.indexOfFirst { it.id == selected.id }.coerceAtLeast(0)

    // Keep the active engine in view when it changes from somewhere else, e.g. by
    // replaying a history entry that used a different engine.
    LaunchedEffect(selected.id) {
        listState.animateScrollToItem(selectedIndex)
    }

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(engines, key = { _, engine -> engine.id.name }) { _, engine ->
            EngineChip(
                engine = engine,
                selected = engine.id == selected.id,
                onClick = { onSelect(engine) },
            )
        }
    }
}

@Composable
private fun EngineChip(
    engine: SearchEngine,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            Color.Transparent
        },
        animationSpec = Motion.effectsDefault(),
        label = "chipContainer",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            engine.brandColor.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = Motion.effectsDefault(),
        label = "chipBorder",
    )
    val selectedDescription = stringResource(R.string.engine_selected, engine.displayName)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .pressable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .semantics { if (selected) contentDescription = selectedDescription },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EngineBadge(engine = engine, size = 34.dp, selected = selected)
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(Motion.effectsFast()) +
                expandHorizontally(Motion.sizeSpring, expandFrom = Alignment.Start),
            exit = fadeOut(Motion.effectsFast()) +
                shrinkHorizontally(Motion.sizeSpring, shrinkTowards = Alignment.Start),
        ) {
            Text(
                text = engine.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 10.dp, end = 8.dp),
            )
        }
    }
}

/**
 * A full engine entry with its AI note, used in the engine sheet.
 */
@Composable
fun EngineListItem(
    engine: SearchEngine,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = Motion.effectsDefault(),
        label = "engineItemContainer",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.985f,
        animationSpec = Motion.spatialDefault(),
        label = "engineItemScale",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(MaterialTheme.shapes.large)
            .background(containerColor)
            .pressable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EngineBadge(engine = engine, size = 44.dp, selected = true)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = engine.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(engine.noteRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
