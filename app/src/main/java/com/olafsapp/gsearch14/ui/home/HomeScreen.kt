package com.olafsapp.gsearch14.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NorthWest
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.olafsapp.gsearch14.R
import com.olafsapp.gsearch14.core.pressable
import com.olafsapp.gsearch14.core.rememberHaptics
import com.olafsapp.gsearch14.core.staggeredEntrance
import com.olafsapp.gsearch14.data.SearchEngine
import com.olafsapp.gsearch14.data.SearchVertical
import com.olafsapp.gsearch14.data.model.HistoryEntry
import com.olafsapp.gsearch14.data.repo.AppSettings
import com.olafsapp.gsearch14.ui.SearchRequest
import com.olafsapp.gsearch14.ui.Suggestion
import com.olafsapp.gsearch14.ui.components.AiFreeControl
import com.olafsapp.gsearch14.ui.components.EngineRail
import com.olafsapp.gsearch14.ui.components.ExpressiveSearchField
import com.olafsapp.gsearch14.ui.components.HistoryRow
import com.olafsapp.gsearch14.ui.components.InlineNotice
import com.olafsapp.gsearch14.ui.components.SectionHeader
import com.olafsapp.gsearch14.ui.components.VerticalSelector
import com.olafsapp.gsearch14.ui.theme.Motion
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val RECENT_PREVIEW_COUNT = 4

/**
 * The search screen.
 *
 * Everything above the fold is one stack: wordmark, input, controls, engine rail, recent
 * searches. When suggestions appear they take over the lower half rather than pushing it
 * down, so the layout never jumps under the user's thumb.
 *
 * Opened from the widget, the screen starts in a slim quick-search layout instead: just the
 * focused field with the keyboard up, the result-type picker and the engine rail. As soon
 * as the field loses focus (keyboard dismissed, back, a tap outside, a search) the rest of
 * the screen animates in and it is the normal home screen again.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    settings: AppSettings,
    query: String,
    vertical: SearchVertical,
    suggestions: List<Suggestion>,
    history: List<HistoryEntry>,
    focusInputOnStart: Boolean,
    onQueryChange: (String) -> Unit,
    onVerticalChange: (SearchVertical) -> Unit,
    onEngineChange: (SearchEngine) -> Unit,
    onAiFreeChange: (Boolean) -> Unit,
    onSubmit: (String) -> SearchRequest?,
    onSearch: (SearchRequest) -> Unit,
    onReplay: (HistoryEntry) -> Unit,
    onTogglePin: (HistoryEntry) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
    onWidgetLaunchHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val haptics = rememberHaptics()
    val focusManager = LocalFocusManager.current
    var fieldFocused by remember { mutableStateOf(false) }
    // Seeded from the launch flag so a widget start never flashes the full layout first.
    var quickMode by rememberSaveable { mutableStateOf(focusInputOnStart) }
    // Visibility rather than height: a floating keyboard is visible but reports no inset.
    val imeVisible by rememberUpdatedState(WindowInsets.isImeVisible)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val emptyQueryMessage = stringResource(R.string.search_empty_query)

    // Drives the one-off entrance of the whole screen. Skipped for a widget start, where
    // the point is to be typing as early as possible.
    val entrance = remember { Animatable(if (focusInputOnStart) 1f else 0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, Motion.effectsSlow())
    }

    // Launched from the widget: switch to the quick layout with an empty, focused field
    // and the keyboard up. The flag is cleared only after the focus request, so a
    // recomposition cannot cancel it first.
    LaunchedEffect(focusInputOnStart) {
        if (focusInputOnStart) {
            quickMode = true
            onQueryChange("")
            focusRequester.requestFocus()
            keyboard?.show()
            onWidgetLaunchHandled()
        }
    }

    // Quick mode ends the moment the field loses focus, after having had it.
    LaunchedEffect(quickMode) {
        if (!quickMode) return@LaunchedEffect
        snapshotFlow { fieldFocused }.dropWhile { !it }.first { !it }
        quickMode = false
    }

    // Hiding the keyboard (back, or its own dismiss key) leaves a Compose field focused,
    // so treat that as leaving the field too.
    LaunchedEffect(quickMode) {
        if (!quickMode) return@LaunchedEffect
        snapshotFlow { imeVisible }.dropWhile { !it }.first { !it }
        focusManager.clearFocus()
    }

    // With a hardware keyboard there is no IME to dismiss; back still leaves quick mode.
    BackHandler(enabled = quickMode) {
        focusManager.clearFocus()
        quickMode = false
    }

    // Tied to focus, not just to the text. Returning from a result page leaves the query in
    // place so it can be edited, but the overlay stays closed until the field is tapped
    // again — previously it was still hanging open over the screen.
    val suggestionsVisible = fieldFocused && suggestions.isNotEmpty() && query.trim().length >= 2

    fun runSearch(text: String) {
        val request = onSubmit(text)
        if (request == null) {
            scope.launch { snackbarHostState.showSnackbar(emptyQueryMessage) }
        } else {
            haptics.confirm()
            keyboard?.hide()
            focusManager.clearFocus()
            onSearch(request)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                // A tap on empty space in the quick layout means "done with the field".
                if (quickMode) {
                    Modifier.clickable(interactionSource = null, indication = null) {
                        focusManager.clearFocus()
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .imePadding()
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        ) {
            AnimatedVisibility(
                visible = !quickMode,
                enter = fadeIn(Motion.effectsDefault()) + expandVertically(Motion.sizeSpring),
                exit = fadeOut(Motion.effectsFast()) + shrinkVertically(Motion.sizeSpring),
            ) {
                Column {
                    HomeHeader(
                        scrollOffset = { scrollState.value.toFloat() },
                        onOpenLibrary = onOpenLibrary,
                        onOpenSettings = onOpenSettings,
                        modifier = Modifier.graphicsLayer { alpha = entrance.value },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // --- Input card ---
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .graphicsLayer {
                        alpha = entrance.value
                        translationY = (1f - entrance.value) * 40.dp.toPx()
                    },
            ) {
                ExpressiveSearchField(
                    value = query,
                    onValueChange = onQueryChange,
                    onSubmit = { runSearch(query) },
                    engine = settings.engine,
                    focusRequester = focusRequester,
                    onFocusChanged = { fieldFocused = it },
                )

                // Quick mode: the result-type picker sits right under the field.
                AnimatedVisibility(
                    visible = quickMode,
                    enter = fadeIn(Motion.effectsDefault()) + expandVertically(Motion.sizeSpring),
                    exit = fadeOut(Motion.effectsFast()) + shrinkVertically(Motion.sizeSpring),
                ) {
                    Column {
                        Spacer(Modifier.height(12.dp))
                        VerticalSelector(
                            verticals = SearchVertical.entries
                                .filter { settings.engine.supports(it) },
                            selected = vertical,
                            onSelect = onVerticalChange,
                        )
                    }
                }
            }

            // Quick mode: the engine rail, full width as on the normal screen.
            AnimatedVisibility(
                visible = quickMode,
                enter = fadeIn(Motion.effectsDefault()) + expandVertically(Motion.sizeSpring),
                exit = fadeOut(Motion.effectsFast()) + shrinkVertically(Motion.sizeSpring),
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    EngineRail(
                        engines = SearchEngine.ALL,
                        selected = settings.engine,
                        onSelect = onEngineChange,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .graphicsLayer {
                        alpha = entrance.value
                        translationY = (1f - entrance.value) * 40.dp.toPx()
                    },
            ) {

                AnimatedVisibility(
                    visible = suggestionsVisible,
                    enter = fadeIn(Motion.effectsFast()) + expandVertically(Motion.sizeSpring),
                    exit = fadeOut(Motion.effectsFast()) + shrinkVertically(Motion.sizeSpring),
                ) {
                    SuggestionList(
                        suggestions = suggestions,
                        onPick = { runSearch(it) },
                        onFill = onQueryChange,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }

                AnimatedVisibility(
                    visible = !suggestionsVisible && !quickMode,
                    enter = fadeIn(Motion.effectsDefault()) + expandVertically(Motion.sizeSpring),
                    exit = fadeOut(Motion.effectsFast()) + shrinkVertically(Motion.sizeSpring),
                ) {
                    Column {
                        Spacer(Modifier.height(14.dp))
                        VerticalSelector(
                            verticals = SearchVertical.entries
                                .filter { settings.engine.supports(it) },
                            selected = vertical,
                            onSelect = onVerticalChange,
                        )
                        Spacer(Modifier.height(12.dp))
                        AiFreeControl(
                            aiFree = settings.aiFree,
                            engine = settings.engine,
                            onToggle = onAiFreeChange,
                        )
                        Spacer(Modifier.height(14.dp))
                        SearchButton(
                            engine = settings.engine,
                            onClick = { runSearch(query) },
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !suggestionsVisible && !quickMode,
                enter = fadeIn(Motion.effectsDefault()),
                exit = fadeOut(Motion.effectsFast()),
            ) {
                Column {
                    Spacer(Modifier.height(26.dp))
                    SectionHeader(
                        text = stringResource(R.string.engine_switcher_title),
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                    EngineRail(
                        engines = SearchEngine.ALL,
                        selected = settings.engine,
                        onSelect = onEngineChange,
                    )

                    if (settings.incognito) {
                        Spacer(Modifier.height(18.dp))
                        InlineNotice(
                            text = stringResource(R.string.history_incognito_note),
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }

                    RecentSearches(
                        history = history,
                        entranceProgress = { entrance.value },
                        onReplay = onReplay,
                        onTogglePin = onTogglePin,
                        onSeeAll = onOpenLibrary,
                    )

                    Spacer(Modifier.height(32.dp))
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding(),
        )
    }
}

@Composable
private fun HomeHeader(
    scrollOffset: () -> Float,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 18.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // The wordmark shrinks and fades as the page scrolls. Both values are read inside
        // graphicsLayer, so scrolling never triggers recomposition of the header.
        Column(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer {
                    val collapse = (scrollOffset() / 220f).coerceIn(0f, 1f)
                    alpha = 1f - collapse * 0.75f
                    scaleX = 1f - collapse * 0.14f
                    scaleY = 1f - collapse * 0.14f
                    translationY = -collapse * 10.dp.toPx()
                    transformOrigin = TransformOrigin(0f, 0.5f)
                },
        ) {
            Text(
                text = stringResource(R.string.app_wordmark_line1),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.app_wordmark_line2),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.width(8.dp))

        HeaderAction(
            icon = Icons.Rounded.History,
            contentDescription = stringResource(R.string.action_library),
            onClick = onOpenLibrary,
        )
        HeaderAction(
            icon = Icons.Rounded.Settings,
            contentDescription = stringResource(R.string.action_settings),
            onClick = onOpenSettings,
        )
    }
}

@Composable
private fun HeaderAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(21.dp),
        )
    }
}

/** The primary call to action, naming the engine that will answer. */
@Composable
private fun SearchButton(
    engine: SearchEngine,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.primary)
            .pressable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "${stringResource(R.string.search_action)} · ${engine.displayName}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun SuggestionList(
    suggestions: List<Suggestion>,
    onPick: (String) -> Unit,
    onFill: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(vertical = 6.dp),
    ) {
        suggestions.forEach { suggestion ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pressable(onClick = { onPick(suggestion.text) })
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (suggestion.fromHistory) {
                        Icons.Rounded.History
                    } else {
                        Icons.Rounded.Search
                    },
                    contentDescription = if (suggestion.fromHistory) {
                        stringResource(R.string.suggestion_from_history)
                    } else {
                        null
                    },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = suggestion.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                // Lifts the suggestion into the field instead of searching, so it can be
                // refined further — the small arrow every search app has, and this one
                // was missing.
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .pressable(onClick = { onFill(suggestion.text) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NorthWest,
                        contentDescription = stringResource(R.string.suggestion_fill),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentSearches(
    history: List<HistoryEntry>,
    entranceProgress: () -> Float,
    onReplay: (HistoryEntry) -> Unit,
    onTogglePin: (HistoryEntry) -> Unit,
    onSeeAll: () -> Unit,
) {
    if (history.isEmpty()) return

    val preview = remember(history) {
        (history.filter { it.pinned } + history.filterNot { it.pinned })
            .take(RECENT_PREVIEW_COUNT)
    }

    Spacer(Modifier.height(22.dp))
    SectionHeader(
        text = stringResource(R.string.history_title),
        modifier = Modifier.padding(horizontal = 24.dp),
        trailing = {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .pressable(onClick = onSeeAll)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.library_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
            }
        },
    )

    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        preview.forEachIndexed { index, entry ->
            HistoryRow(
                entry = entry,
                onClick = { onReplay(entry) },
                onTogglePin = { onTogglePin(entry) },
                modifier = Modifier.staggeredEntrance(entranceProgress, index, preview.size),
            )
        }
    }
}
