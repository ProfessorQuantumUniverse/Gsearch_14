package com.olafsapp.gsearch14.core

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.olafsapp.gsearch14.ui.theme.LocalHapticsEnabled
import com.olafsapp.gsearch14.ui.theme.Motion

/**
 * Haptics that respect the user's setting.
 *
 * Every call site goes through this rather than [LocalHapticFeedback] directly, so turning
 * haptics off in settings actually silences the whole app.
 */
@Composable
fun rememberHaptics(): Haptics {
    val feedback = LocalHapticFeedback.current
    val enabled = LocalHapticsEnabled.current
    return remember(feedback, enabled) { Haptics(feedback, enabled) }
}

class Haptics(
    private val feedback: androidx.compose.ui.hapticfeedback.HapticFeedback,
    private val enabled: Boolean,
) {
    /** A light tick for selection changes: chips, toggles, engine switches. */
    fun tick() {
        if (enabled) feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    /** A firmer bump for committing something: running a search, deleting an entry. */
    fun confirm() {
        if (enabled) feedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

/**
 * Clickable with a spring-loaded press scale.
 *
 * This is the app's single most repeated micro-interaction, so it lives in one place: the
 * element dips slightly while held and springs back on release. Because it is driven by the
 * interaction source rather than a fire-and-forget animator, an interrupted press resolves
 * correctly instead of getting stuck mid-animation — the flaw the old
 * `animate().withEndAction { ... }` chains had.
 */
fun Modifier.pressable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    pressedScale: Float = 0.955f,
    role: Role? = Role.Button,
    hapticOnClick: Boolean = true,
    onLongClick: (() -> Unit)? = null,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val haptics = rememberHaptics()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = Motion.spatialFast(),
        label = "pressScale",
    )

    this
        .scale(scale)
        // One accessibility node per pressable element, matching what Material's own
        // Surface and Button do: a screen reader reads the whole row as a single item
        // instead of stopping on each piece of text inside it.
        .semantics(mergeDescendants = true) {}
        .indication(interactionSource, ripple())
        .then(
            Modifier.combinedClickableCompat(
                interactionSource = interactionSource,
                enabled = enabled,
                role = role,
                onClick = {
                    if (hapticOnClick) haptics.tick()
                    onClick()
                },
                onLongClick = onLongClick?.let {
                    {
                        haptics.confirm()
                        it()
                    }
                },
            ),
        )
}

/**
 * Wraps `combinedClickable` so [pressable] can stay readable. Indication is null here
 * because [pressable] draws the ripple itself, above the scale transform.
 */
private fun Modifier.combinedClickableCompat(
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
    role: Role?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
): Modifier = this.combinedClickable(
    interactionSource = interactionSource,
    indication = null,
    enabled = enabled,
    role = role,
    onLongClick = onLongClick,
    onClick = onClick,
)

/**
 * Staggered entrance transform for list items.
 *
 * [progress] is expected to run 0..1; items further down the list start later but all
 * finish together, which keeps the whole list from feeling slow on long histories.
 */
fun Modifier.staggeredEntrance(
    progress: () -> Float,
    index: Int,
    itemCount: Int,
): Modifier {
    val stepFraction = if (itemCount <= 1) 0f else {
        val totalDelay =
            (Motion.STAGGER_STEP_MS * (itemCount - 1)).coerceAtMost(Motion.STAGGER_MAX_MS)
        val perItem = totalDelay.toFloat() / (itemCount - 1)
        (perItem * index) / (totalDelay + 300f)
    }
    return this.graphicsLayer {
        val local = ((progress() - stepFraction) / (1f - stepFraction).coerceAtLeast(0.001f))
            .coerceIn(0f, 1f)
        alpha = local
        translationY = (1f - local) * ENTRANCE_OFFSET.toPx()
        scaleX = 0.96f + 0.04f * local
        scaleY = 0.96f + 0.04f * local
    }
}

private val ENTRANCE_OFFSET = 28.dp
