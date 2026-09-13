package com.vibecheck.lifepulse.ui.neobrutalism

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A container card with a thick black border, hard offset shadow, squared geometry,
 * and solid fill — the core visual block of the Neobrutalism kit.
 *
 * Usage:
 * ```
 * NeoCard(backgroundColor = NeoColors.Surface) {
 *     Text("Hello", style = NeoTypography.titleLarge)
 * }
 * ```
 */
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = NeoColors.Surface,
    borderColor: Color = NeoColors.Border,
    shadowColor: Color = NeoColors.Shadow,
    shape: Shape = RoundedCornerShape(12.dp),
    borderWidth: Dp = 3.dp,
    shadowOffsetX: Dp = 6.dp,
    shadowOffsetY: Dp = 6.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .neoHardShadow(
                shape = shape,
                shadowColor = shadowColor,
                offsetX = shadowOffsetX,
                offsetY = shadowOffsetY
            )
            .background(color = backgroundColor, shape = shape)
            .border(width = borderWidth, color = borderColor, shape = shape)
            .padding(contentPadding)
    ) {
        content()
    }
}

/**
 * A dialog-scale stacked-poster effect: a heavy backing slab peeks from behind the
 * main panel, keeping the look bold, architectural, and high-contrast.
 *
 * Usage:
 * ```
 * NeoStackedDialogCard(backLayerColor = NeoColors.Accent, tapeText = "NEW") {
 *     Text("Content", style = NeoTypography.titleLarge)
 * }
 * ```
 */
@Composable
fun NeoStackedDialogCard(
    modifier: Modifier = Modifier,
    backLayerColor: Color = NeoColors.Accent,
    backRotation: Float = -3f,
    frontColor: Color = NeoColors.Surface,
    shape: Shape = RoundedCornerShape(16.dp),
    tapeText: String? = null,
    tapeColor: Color = NeoColors.Lime,
    contentPadding: Dp = 22.dp,
    verticalSpacing: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier.padding(start = 10.dp, top = 18.dp, end = 18.dp, bottom = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        // Back layer: a hard offset slab, closer to a poster print than a decorative sticker.
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 10.dp, y = 10.dp)
                .rotate(backRotation)
                .background(color = backLayerColor, shape = shape)
                .border(width = 4.dp, color = NeoColors.Border, shape = shape)
        )

        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = frontColor,
            borderWidth = 4.dp,
            shadowOffsetX = 5.dp,
            shadowOffsetY = 5.dp,
            shape = shape,
            contentPadding = contentPadding
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(verticalSpacing), content = content)
        }

        if (tapeText != null) {
            NeoTapeSticker(
                text = tapeText,
                color = tapeColor,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 22.dp)
                    .rotate(-2f)
            )
        }
    }
}

/**
 * A small industrial label strip that can overlap a card edge for poster-style emphasis.
 */
@Composable
fun NeoTapeSticker(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = NeoColors.Lime
) {
    Box(
        modifier = modifier
            .background(color = color, shape = RoundedCornerShape(2.dp))
            .border(width = 3.dp, color = NeoColors.Border, shape = RoundedCornerShape(2.dp))
            .padding(horizontal = 18.dp, vertical = 7.dp)
    ) {
        Text(
            text = text.uppercase(),
            style = NeoTypography.labelLarge,
            fontWeight = FontWeight.Black,
            color = NeoColors.OnSurface
        )
    }
}

