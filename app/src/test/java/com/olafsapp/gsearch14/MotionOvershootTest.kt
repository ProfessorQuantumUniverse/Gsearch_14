package com.olafsapp.gsearch14

import androidx.compose.animation.core.FloatSpringSpec
import androidx.compose.animation.core.Spring
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression cover for the crash when the accent colour was changed.
 *
 * The accent picker animated a `Modifier.padding` from 5dp down to 0dp with an underdamped
 * spring. Such a spring does not approach its target from one side — it passes through it
 * and swings back. For a target of zero that means negative values, and `Modifier.padding`,
 * `Modifier.border` and `RowScope.weight` all reject those with an `IllegalArgumentException`.
 *
 * These tests assert the property that made the crash inevitable, and that the settling
 * spec the app now uses for such values does not have it.
 */
class MotionOvershootTest {

    private fun samples(dampingRatio: Float, from: Float, to: Float): List<Float> {
        val spec = FloatSpringSpec(dampingRatio = dampingRatio, stiffness = Spring.StiffnessMedium)
        val durationNanos = spec.getDurationNanos(from, to, 0f)
        return (0..400).map { step ->
            spec.getValueFromNanos(durationNanos * step / 400, from, to, 0f)
        }
    }

    @Test
    fun `an underdamped spring settling on zero produces negative values`() {
        val minimum = samples(dampingRatio = 0.5f, from = 5f, to = 0f).min()
        assertTrue(
            "expected a bouncy spring to undershoot below zero, lowest value was $minimum",
            minimum < 0f,
        )
    }

    @Test
    fun `the settling spec never crosses its target`() {
        val minimum = samples(
            dampingRatio = Spring.DampingRatioNoBouncy,
            from = 5f,
            to = 0f,
        ).min()
        assertTrue("critically damped spring undershot to $minimum", minimum >= 0f)
    }

    @Test
    fun `the settling spec stays inside the range when growing too`() {
        val values = samples(dampingRatio = Spring.DampingRatioNoBouncy, from = 1f, to = 1.25f)
        assertTrue("undershot below the start: ${values.min()}", values.min() >= 1f - 0.001f)
        assertTrue("overshot past the target: ${values.max()}", values.max() <= 1.25f + 0.001f)
    }
}
