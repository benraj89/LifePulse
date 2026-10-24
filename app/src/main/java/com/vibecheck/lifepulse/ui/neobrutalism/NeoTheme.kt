package com.vibecheck.lifepulse.ui.neobrutalism

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.lightColorScheme

/**
 * Colors exposed via CompositionLocal so components can read the raw Neobrutalism
 * palette (not just the mapped Material3 ColorScheme).
 */
val LocalNeoColors = staticCompositionLocalOf { NeoColors }
val LocalNeoShapes = staticCompositionLocalOf { LocalNeoShapesDefault }

/** Convenience accessor object, e.g. `NeoTheme.colors.primary`, `NeoTheme.shapes.pill`. */
object NeoTheme {
    val colors: NeoColors
        @Composable get() = LocalNeoColors.current
    val shapes: NeoShapes
        @Composable get() = LocalNeoShapes.current
}

private val NeoMaterialColorScheme = lightColorScheme(
    // Material uses primary for text actions too; yellow is a surface accent, never text ink.
    primary = NeoColors.Ink,
    onPrimary = NeoColors.Surface,
    secondary = NeoColors.Secondary,
    onSecondary = NeoColors.OnPrimary,
    background = NeoColors.Background,
    onBackground = NeoColors.OnSurface,
    surface = NeoColors.Surface,
    onSurface = NeoColors.OnSurface,
    primaryContainer = NeoColors.Yellow,
    onPrimaryContainer = NeoColors.Ink,
    secondaryContainer = NeoColors.Habit,
    onSecondaryContainer = NeoColors.Ink,
    tertiary = NeoColors.Expense,
    onTertiary = NeoColors.Ink,
    tertiaryContainer = NeoColors.Expense,
    onTertiaryContainer = NeoColors.Ink,
    surfaceVariant = NeoColors.Concrete,
    onSurfaceVariant = NeoColors.SecondaryText,
    surfaceContainer = NeoColors.Surface,
    surfaceContainerLow = NeoColors.Surface,
    surfaceContainerHigh = NeoColors.Surface,
    surfaceContainerHighest = NeoColors.Concrete,
    outline = NeoColors.Ink,
    outlineVariant = NeoColors.Rule,
    surfaceTint = NeoColors.Surface,
    inverseSurface = NeoColors.Ink,
    inverseOnSurface = NeoColors.Surface,
    inversePrimary = NeoColors.Yellow,
    error = NeoColors.Danger,
    onError = NeoColors.White
)

/**
 * Root theme wrapper applying the Neobrutalism palette, typography, and shapes.
 * Wrap any screen/subtree in this to opt into the style:
 *
 * ```
 * NeoBrutalismTheme {
 *     NeoShowcaseScreen()
 * }
 * ```
 */
@Composable
fun NeoBrutalismTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalNeoColors provides NeoColors,
        LocalNeoShapes provides LocalNeoShapesDefault
    ) {
        MaterialTheme(
            colorScheme = NeoMaterialColorScheme,
            typography = NeoTypography,
            shapes = Shapes(
                extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape(4.dp),
                medium = RoundedCornerShape(8.dp), large = RoundedCornerShape(16.dp), extraLarge = RoundedCornerShape(16.dp)
            ),
            content = content
        )
    }
}

