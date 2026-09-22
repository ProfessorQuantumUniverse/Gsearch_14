package com.olafsapp.gsearch14.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Motion tokens for the whole app.
 *
 * Anything that moves in space uses a spring, so an interrupted gesture continues from
 * wherever it was instead of snapping. Anything that only fades or recolours uses a short
 * tween, because springy alpha reads as a flicker.
 */
object Motion {

    /** Emphasised easing for fades and colour changes. */
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val StandardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    // --- Spatial: position, size, scale -------------------------------------------

    /** Snappy, for press feedback and small toggles. */
    fun <T> spatialFast(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.7f,
        stiffness = Spring.StiffnessMediumLow * 2f,
    )

    /** The default for cards, sheets and list movement. */
    fun <T> spatialDefault(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.8f,
        stiffness = Spring.StiffnessMediumLow,
    )

    /**
     * Critically damped: reaches the target without overshooting it.
     *
     * Required for any value that feeds a modifier with a validated range — paddings,
     * border widths, layout weights and fill fractions all reject negative or
     * out-of-range input, and a bouncy spring passes through exactly those values on its
     * way to rest.
     */
    fun <T> spatialSettle(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow * 2f,
    )

    /** Deliberately loose, for the one or two moments that should feel playful. */
    fun <T> spatialBouncy(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.5f,
        stiffness = Spring.StiffnessLow * 1.6f,
    )

    // Size and offset need their own visibility thresholds to avoid sub-pixel jitter.
    val sizeSpring: FiniteAnimationSpec<IntSize> = spring(
        dampingRatio = 0.85f,
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = IntSize(1, 1),
    )
    val offsetSpring: FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = 0.85f,
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = IntOffset(1, 1),
    )

    // --- Effects: alpha, colour ---------------------------------------------------

    fun <T> effectsFast(): FiniteAnimationSpec<T> = tween(150, easing = StandardEasing)
    fun <T> effectsDefault(): FiniteAnimationSpec<T> = tween(250, easing = EmphasizedEasing)
    fun <T> effectsSlow(): FiniteAnimationSpec<T> = tween(400, easing = EmphasizedEasing)

    /** Delay between neighbouring items in a staggered entrance, in milliseconds. */
    const val STAGGER_STEP_MS = 34

    /** Nothing staggers for longer than this, however long the list is. */
    const val STAGGER_MAX_MS = 320
}
