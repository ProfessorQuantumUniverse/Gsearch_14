package com.olafsapp.gsearch14.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * Expressive typography: display and headline sizes are pulled up and tightened, so a short
 * label like the app's wordmark carries real weight, while body and label styles stay at
 * comfortable reading sizes.
 */
private val lineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun expressive(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    tracking: Double = 0.0,
) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
    lineHeightStyle = lineHeightStyle,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

val GsearchTypography = Typography(
    displayLarge = expressive(57, 62, FontWeight.Bold, -1.0),
    displayMedium = expressive(45, 50, FontWeight.Bold, -0.75),
    displaySmall = expressive(36, 42, FontWeight.Bold, -0.5),

    headlineLarge = expressive(32, 38, FontWeight.Bold, -0.4),
    headlineMedium = expressive(28, 34, FontWeight.Bold, -0.3),
    headlineSmall = expressive(24, 30, FontWeight.SemiBold, -0.2),

    titleLarge = expressive(22, 28, FontWeight.SemiBold, -0.1),
    titleMedium = expressive(16, 22, FontWeight.SemiBold, 0.1),
    titleSmall = expressive(14, 20, FontWeight.SemiBold, 0.1),

    bodyLarge = expressive(16, 24, FontWeight.Normal, 0.15),
    bodyMedium = expressive(14, 20, FontWeight.Normal, 0.2),
    bodySmall = expressive(12, 16, FontWeight.Normal, 0.3),

    labelLarge = expressive(14, 20, FontWeight.SemiBold, 0.1),
    labelMedium = expressive(12, 16, FontWeight.SemiBold, 0.4),
    labelSmall = expressive(11, 16, FontWeight.Medium, 0.5),
)
