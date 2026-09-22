package com.olafsapp.gsearch14.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.olafsapp.gsearch14.data.repo.AccentPalette

/**
 * One accent identity, expressed as the Material 3 key colour roles.
 *
 * Only the accent roles live here; the neutral surfaces are shared across all palettes so
 * the app keeps one recognisable "paper" regardless of which accent is picked.
 */
private data class AccentSpec(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
)

// --- Light accents -----------------------------------------------------------------

private val SignalLight = AccentSpec(
    primary = Color(0xFF3B4CCA), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDFE0FF), onPrimaryContainer = Color(0xFF00105C),
    secondary = Color(0xFF5B5D72), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E1F9), onSecondaryContainer = Color(0xFF181A2C),
    tertiary = Color(0xFF77536D), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD7F1), onTertiaryContainer = Color(0xFF2D1228),
)

private val ForestLight = AccentSpec(
    primary = Color(0xFF1F6B47), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA6F2C4), onPrimaryContainer = Color(0xFF002112),
    secondary = Color(0xFF4E6355), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1E8D6), onSecondaryContainer = Color(0xFF0B1F14),
    tertiary = Color(0xFF3A6470), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBEEAF8), onTertiaryContainer = Color(0xFF001F27),
)

private val EmberLight = AccentSpec(
    primary = Color(0xFFA53722), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD3), onPrimaryContainer = Color(0xFF3B0900),
    secondary = Color(0xFF77574F), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDAD3), onSecondaryContainer = Color(0xFF2C1510),
    tertiary = Color(0xFF6C5D2F), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF6E1A7), onTertiaryContainer = Color(0xFF231B00),
)

private val InkLight = AccentSpec(
    primary = Color(0xFF2F3033), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9DBE0), onPrimaryContainer = Color(0xFF15171A),
    secondary = Color(0xFF45474B), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2E2E6), onSecondaryContainer = Color(0xFF1A1C1F),
    tertiary = Color(0xFF3C5F76), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC7E4FF), onTertiaryContainer = Color(0xFF001E2E),
)

private val VioletLight = AccentSpec(
    primary = Color(0xFF6A3FBF), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF), onPrimaryContainer = Color(0xFF23005B),
    secondary = Color(0xFF635B70), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE9DEF8), onSecondaryContainer = Color(0xFF1E192B),
    tertiary = Color(0xFF7E5260), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9E3), onTertiaryContainer = Color(0xFF31101D),
)

// --- Dark accents ------------------------------------------------------------------

private val SignalDark = AccentSpec(
    primary = Color(0xFFBBC3FF), onPrimary = Color(0xFF041A8F),
    primaryContainer = Color(0xFF2231B1), onPrimaryContainer = Color(0xFFDFE0FF),
    secondary = Color(0xFFC4C5DD), onSecondary = Color(0xFF2D2F42),
    secondaryContainer = Color(0xFF434559), onSecondaryContainer = Color(0xFFE0E1F9),
    tertiary = Color(0xFFE6BAD7), onTertiary = Color(0xFF45263D),
    tertiaryContainer = Color(0xFF5D3C55), onTertiaryContainer = Color(0xFFFFD7F1),
)

private val ForestDark = AccentSpec(
    primary = Color(0xFF8AD5A9), onPrimary = Color(0xFF003822),
    primaryContainer = Color(0xFF005233), onPrimaryContainer = Color(0xFFA6F2C4),
    secondary = Color(0xFFB5CCBA), onSecondary = Color(0xFF203528),
    secondaryContainer = Color(0xFF364B3E), onSecondaryContainer = Color(0xFFD1E8D6),
    tertiary = Color(0xFFA2CDDB), onTertiary = Color(0xFF023541),
    tertiaryContainer = Color(0xFF204C58), onTertiaryContainer = Color(0xFFBEEAF8),
)

private val EmberDark = AccentSpec(
    primary = Color(0xFFFFB4A4), onPrimary = Color(0xFF5F1500),
    primaryContainer = Color(0xFF85220E), onPrimaryContainer = Color(0xFFFFDAD3),
    secondary = Color(0xFFE7BDB4), onSecondary = Color(0xFF442A23),
    secondaryContainer = Color(0xFF5D3F39), onSecondaryContainer = Color(0xFFFFDAD3),
    tertiary = Color(0xFFD9C58D), onTertiary = Color(0xFF3B2F05),
    tertiaryContainer = Color(0xFF534519), onTertiaryContainer = Color(0xFFF6E1A7),
)

private val InkDark = AccentSpec(
    primary = Color(0xFFC6C7CC), onPrimary = Color(0xFF2F3033),
    primaryContainer = Color(0xFF45474B), onPrimaryContainer = Color(0xFFE2E2E6),
    secondary = Color(0xFFC6C7CB), onSecondary = Color(0xFF2F3134),
    secondaryContainer = Color(0xFF45474B), onSecondaryContainer = Color(0xFFE2E2E6),
    tertiary = Color(0xFFA5C9E3), onTertiary = Color(0xFF07344A),
    tertiaryContainer = Color(0xFF244B62), onTertiaryContainer = Color(0xFFC7E4FF),
)

private val VioletDark = AccentSpec(
    primary = Color(0xFFD3BBFF), onPrimary = Color(0xFF3A0A8C),
    primaryContainer = Color(0xFF5227A5), onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCEC2DB), onSecondary = Color(0xFF342D40),
    secondaryContainer = Color(0xFF4A4458), onSecondaryContainer = Color(0xFFE9DEF8),
    tertiary = Color(0xFFF1B8C8), onTertiary = Color(0xFF4A2532),
    tertiaryContainer = Color(0xFF633B49), onTertiaryContainer = Color(0xFFFFD9E3),
)

private fun accentSpec(accent: AccentPalette, dark: Boolean): AccentSpec = when (accent) {
    AccentPalette.SIGNAL -> if (dark) SignalDark else SignalLight
    AccentPalette.FOREST -> if (dark) ForestDark else ForestLight
    AccentPalette.EMBER -> if (dark) EmberDark else EmberLight
    AccentPalette.INK -> if (dark) InkDark else InkLight
    AccentPalette.VIOLET -> if (dark) VioletDark else VioletLight
}

/** A swatch for the accent picker, without having to build a whole scheme. */
fun accentSwatch(accent: AccentPalette, dark: Boolean): Color = accentSpec(accent, dark).primary

/**
 * Builds the full colour scheme.
 *
 * @param pureBlack collapses the dark surfaces to true black, which saves power on OLED
 *   panels and gives the dark theme a sharper, more editorial feel.
 */
fun gsearchColorScheme(
    accent: AccentPalette,
    dark: Boolean,
    pureBlack: Boolean = false,
): ColorScheme {
    val spec = accentSpec(accent, dark)
    return if (dark) {
        val background = if (pureBlack) Color(0xFF000000) else Color(0xFF121317)
        val surface = background
        darkColorScheme(
            primary = spec.primary,
            onPrimary = spec.onPrimary,
            primaryContainer = spec.primaryContainer,
            onPrimaryContainer = spec.onPrimaryContainer,
            secondary = spec.secondary,
            onSecondary = spec.onSecondary,
            secondaryContainer = spec.secondaryContainer,
            onSecondaryContainer = spec.onSecondaryContainer,
            tertiary = spec.tertiary,
            onTertiary = spec.onTertiary,
            tertiaryContainer = spec.tertiaryContainer,
            onTertiaryContainer = spec.onTertiaryContainer,
            background = background,
            onBackground = Color(0xFFE4E1E9),
            surface = surface,
            onSurface = Color(0xFFE4E1E9),
            surfaceVariant = Color(0xFF46464F),
            onSurfaceVariant = Color(0xFFC7C5D0),
            surfaceTint = spec.primary,
            inverseSurface = Color(0xFFE4E1E9),
            inverseOnSurface = Color(0xFF303036),
            inversePrimary = spec.primaryContainer,
            surfaceDim = if (pureBlack) Color(0xFF000000) else Color(0xFF121317),
            surfaceBright = if (pureBlack) Color(0xFF1B1B1F) else Color(0xFF38383E),
            surfaceContainerLowest = if (pureBlack) Color(0xFF000000) else Color(0xFF0D0E11),
            surfaceContainerLow = if (pureBlack) Color(0xFF0A0A0C) else Color(0xFF1A1B1F),
            surfaceContainer = if (pureBlack) Color(0xFF121214) else Color(0xFF1E1F23),
            surfaceContainerHigh = if (pureBlack) Color(0xFF1C1C1F) else Color(0xFF292A2E),
            surfaceContainerHighest = if (pureBlack) Color(0xFF262629) else Color(0xFF343439),
            outline = Color(0xFF918F9A),
            outlineVariant = Color(0xFF46464F),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            scrim = Color(0xFF000000),
        )
    } else {
        lightColorScheme(
            primary = spec.primary,
            onPrimary = spec.onPrimary,
            primaryContainer = spec.primaryContainer,
            onPrimaryContainer = spec.onPrimaryContainer,
            secondary = spec.secondary,
            onSecondary = spec.onSecondary,
            secondaryContainer = spec.secondaryContainer,
            onSecondaryContainer = spec.onSecondaryContainer,
            tertiary = spec.tertiary,
            onTertiary = spec.onTertiary,
            tertiaryContainer = spec.tertiaryContainer,
            onTertiaryContainer = spec.onTertiaryContainer,
            background = Color(0xFFFBF8FF),
            onBackground = Color(0xFF1B1B21),
            surface = Color(0xFFFBF8FF),
            onSurface = Color(0xFF1B1B21),
            surfaceVariant = Color(0xFFE3E1EC),
            onSurfaceVariant = Color(0xFF46464F),
            surfaceTint = spec.primary,
            inverseSurface = Color(0xFF303036),
            inverseOnSurface = Color(0xFFF2EFF7),
            inversePrimary = spec.primaryContainer,
            surfaceDim = Color(0xFFDBD9E0),
            surfaceBright = Color(0xFFFBF8FF),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFF5F2FA),
            surfaceContainer = Color(0xFFEFEDF4),
            surfaceContainerHigh = Color(0xFFE9E7EF),
            surfaceContainerHighest = Color(0xFFE3E1E9),
            outline = Color(0xFF777680),
            outlineVariant = Color(0xFFC7C5D0),
            error = Color(0xFFBA1A1A),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            scrim = Color(0xFF000000),
        )
    }
}
