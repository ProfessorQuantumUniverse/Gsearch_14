package com.olafsapp.gsearch14.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.olafsapp.gsearch14.R
import com.olafsapp.gsearch14.core.pressable
import com.olafsapp.gsearch14.data.AiFreeSupport
import com.olafsapp.gsearch14.data.SearchEngine
import com.olafsapp.gsearch14.data.SearchVertical
import com.olafsapp.gsearch14.ui.theme.Motion

/**
 * The app's main input.
 *
 * Built on [BasicTextField] rather than `TextField` so the container can be shaped and
 * animated freely: it lifts, deepens its border and swaps its leading glyph for the active
 * engine badge the moment it takes focus.
 */
@Composable
fun ExpressiveSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    engine: SearchEngine,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onFocusChanged: (Boolean) -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(focused) { onFocusChanged(focused) }

    // Settling spring plus a clamp: Modifier.border rejects a negative width, and an
    // underdamped spring passes below its target on the way to rest.
    val borderWidth by animateDpAsState(
        targetValue = if (focused) 2.dp else 1.dp,
        animationSpec = Motion.spatialSettle(),
        label = "fieldBorder",
    )
    val borderColor by animateColorAsState(
        targetValue = if (focused) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = Motion.effectsDefault(),
        label = "fieldBorderColor",
    )
    val containerColor by animateColorAsState(
        targetValue = if (focused) {
            MaterialTheme.colorScheme.surfaceContainerLowest
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = Motion.effectsDefault(),
        label = "fieldContainer",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(containerColor)
            .border(borderWidth.coerceAtLeast(0.dp), borderColor, RoundedCornerShape(28.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Leading glyph: a generic search icon at rest, the engine's badge once focused,
        // so the field always says which engine will answer.
        AnimatedContent(
            targetState = focused,
            transitionSpec = {
                (fadeIn(Motion.effectsFast()) + scaleIn(Motion.spatialFast(), initialScale = 0.7f))
                    .togetherWith(
                        fadeOut(Motion.effectsFast()) +
                            scaleOut(Motion.spatialFast(), targetScale = 0.7f),
                    )
            },
            label = "leadingGlyph",
        ) { isFocused ->
            if (isFocused) {
                EngineBadge(engine = engine, size = 38.dp)
            } else {
                Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.width(10.dp))

        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = stringResource(R.string.search_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                interactionSource = interactionSource,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboard?.hide()
                        onSubmit()
                    },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        }

        AnimatedVisibility(
            visible = value.isNotEmpty(),
            enter = fadeIn(Motion.effectsFast()) +
                scaleIn(Motion.spatialFast(), initialScale = 0.6f),
            exit = fadeOut(Motion.effectsFast()) +
                scaleOut(Motion.spatialFast(), targetScale = 0.6f),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .pressable(onClick = { onValueChange("") }),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.search_clear),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * Segmented selector for the result type.
 *
 * The highlight is a single element that slides and resizes between options rather than
 * one background per option fading in and out, which is what makes it read as one moving
 * object.
 */
@Composable
fun VerticalSelector(
    verticals: List<SearchVertical>,
    selected: SearchVertical,
    onSelect: (SearchVertical) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (verticals.size <= 1) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        verticals.forEach { vertical ->
            val isSelected = vertical == selected
            val container by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.surfaceContainerLowest
                } else {
                    Color.Transparent
                },
                animationSpec = Motion.effectsDefault(),
                label = "verticalContainer",
            )
            val labelColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = Motion.effectsDefault(),
                label = "verticalLabel",
            )
            // Must stay strictly positive: RowScope.weight rejects zero or less.
            val weight by animateFloatAsState(
                targetValue = if (isSelected) 1.25f else 1f,
                animationSpec = Motion.spatialSettle(),
                label = "verticalWeight",
            )

            Box(
                modifier = Modifier
                    .weight(weight.coerceAtLeast(0.1f))
                    .height(40.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(container)
                    .pressable(onClick = { onSelect(vertical) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(vertical.labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = labelColor,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * The AI-free switch, reworked into a statement rather than a toggle.
 *
 * Version 3 showed two competing labels around a hidden-state switch and it was genuinely
 * hard to tell which mode was active. This is one surface that changes colour and copy, so
 * the current mode is the thing you read.
 */
@Composable
fun AiFreeControl(
    aiFree: Boolean,
    engine: SearchEngine,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locked = engine.aiFreeSupport != AiFreeSupport.VIA_URL
    val effective = aiFree || engine.aiFreeSupport ==
        AiFreeSupport.ALWAYS_AI_FREE

    val container by animateColorAsState(
        targetValue = if (effective) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = Motion.effectsDefault(),
        label = "aiContainer",
    )
    val onContainer by animateColorAsState(
        targetValue = if (effective) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = Motion.effectsDefault(),
        label = "aiOnContainer",
    )

    val detail = when (engine.aiFreeSupport) {
        AiFreeSupport.ALWAYS_AI_FREE ->
            stringResource(R.string.ai_free_unsupported, engine.displayName)

        AiFreeSupport.ENGINE_SETTING_ONLY ->
            stringResource(R.string.ai_free_engine_setting, engine.displayName)

        AiFreeSupport.VIA_URL ->
            if (aiFree) {
                stringResource(R.string.ai_free_on_detail)
            } else {
                stringResource(R.string.ai_free_off_detail)
            }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(container)
            .then(
                if (locked) Modifier else Modifier.pressable(onClick = { onToggle(!aiFree) }),
            )
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(
                    if (effective) R.string.ai_free_on else R.string.ai_free_off,
                ),
                style = MaterialTheme.typography.titleSmall,
                color = onContainer,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = onContainer.copy(alpha = 0.78f),
            )
        }

        if (!locked) {
            Spacer(Modifier.width(12.dp))
            AiFreeIndicator(active = aiFree, tint = onContainer)
        }
    }
}

/**
 * A two-state track whose knob springs across and swells slightly as it lands.
 */
@Composable
private fun AiFreeIndicator(
    active: Boolean,
    tint: Color,
) {
    val offset by animateDpAsState(
        targetValue = if (active) 22.dp else 0.dp,
        animationSpec = Motion.spatialBouncy(),
        label = "aiKnobOffset",
    )
    val knobScale by animateFloatAsState(
        targetValue = if (active) 1f else 0.82f,
        animationSpec = Motion.spatialBouncy(),
        label = "aiKnobScale",
    )

    Box(
        modifier = Modifier
            .width(52.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(start = 4.dp)
                .offset { IntOffset(offset.roundToPx(), 0) }
                .size(22.dp)
                .scale(knobScale)
                .clip(RoundedCornerShape(11.dp))
                .background(tint),
        )
    }
}
