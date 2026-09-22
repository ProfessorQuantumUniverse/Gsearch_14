package com.olafsapp.gsearch14.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.olafsapp.gsearch14.data.repo.AccentPalette
import com.olafsapp.gsearch14.data.repo.ThemeMode

/**
 * Expressive shapes: noticeably rounder than the Material baseline, which is what makes
 * the stacked cards read as soft slabs rather than boxes.
 */
val GsearchShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

/** Whether haptic feedback is on, so leaf components don't each need the settings. */
val LocalHapticsEnabled = staticCompositionLocalOf { true }

@Composable
fun Gsearch14Theme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accent: AccentPalette = AccentPalette.SIGNAL,
    dynamicColor: Boolean = true,
    pureBlackDark: Boolean = false,
    hapticsEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val target = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val scheme =
                if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            // Dynamic colour still honours the pure-black preference.
            if (dark && pureBlackDark) scheme.flattenedToBlack() else scheme
        }

        else -> gsearchColorScheme(accent = accent, dark = dark, pureBlack = pureBlackDark)
    }

    // Switching theme or accent crossfades every role instead of cutting, which turns a
    // settings toggle into something worth watching.
    MaterialTheme(
        colorScheme = target.animated(),
        typography = GsearchTypography,
        shapes = GsearchShapes,
    ) {
        CompositionLocalProvider(LocalHapticsEnabled provides hapticsEnabled, content = content)
    }
}

private fun ColorScheme.flattenedToBlack(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0A0C),
    surfaceContainer = Color(0xFF121214),
    surfaceContainerHigh = Color(0xFF1C1C1F),
    surfaceContainerHighest = Color(0xFF262629),
)

@Composable
private fun ColorScheme.animated(): ColorScheme {
    @Composable
    fun Color.anim(label: String): State<Color> =
        animateColorAsState(this, Motion.effectsDefault(), label = label)

    return copy(
        primary = primary.anim("primary").value,
        onPrimary = onPrimary.anim("onPrimary").value,
        primaryContainer = primaryContainer.anim("primaryContainer").value,
        onPrimaryContainer = onPrimaryContainer.anim("onPrimaryContainer").value,
        secondary = secondary.anim("secondary").value,
        onSecondary = onSecondary.anim("onSecondary").value,
        secondaryContainer = secondaryContainer.anim("secondaryContainer").value,
        onSecondaryContainer = onSecondaryContainer.anim("onSecondaryContainer").value,
        tertiary = tertiary.anim("tertiary").value,
        onTertiary = onTertiary.anim("onTertiary").value,
        tertiaryContainer = tertiaryContainer.anim("tertiaryContainer").value,
        onTertiaryContainer = onTertiaryContainer.anim("onTertiaryContainer").value,
        background = background.anim("background").value,
        onBackground = onBackground.anim("onBackground").value,
        surface = surface.anim("surface").value,
        onSurface = onSurface.anim("onSurface").value,
        surfaceVariant = surfaceVariant.anim("surfaceVariant").value,
        onSurfaceVariant = onSurfaceVariant.anim("onSurfaceVariant").value,
        surfaceContainerLowest = surfaceContainerLowest.anim("containerLowest").value,
        surfaceContainerLow = surfaceContainerLow.anim("containerLow").value,
        surfaceContainer = surfaceContainer.anim("container").value,
        surfaceContainerHigh = surfaceContainerHigh.anim("containerHigh").value,
        surfaceContainerHighest = surfaceContainerHighest.anim("containerHighest").value,
        outline = outline.anim("outline").value,
        outlineVariant = outlineVariant.anim("outlineVariant").value,
    )
}
