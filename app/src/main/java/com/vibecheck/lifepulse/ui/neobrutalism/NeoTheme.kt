package com.vibecheck.lifepulse.ui.neobrutalism

import androidx.compose.material3.MaterialTheme
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
    primary = NeoColors.Primary,
    onPrimary = NeoColors.OnPrimary,
    secondary = NeoColors.Secondary,
    onSecondary = NeoColors.OnPrimary,
    background = NeoColors.Background,
    onBackground = NeoColors.OnSurface,
    surface = NeoColors.Surface,
    onSurface = NeoColors.OnSurface,
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
            content = content
        )
    }
}

