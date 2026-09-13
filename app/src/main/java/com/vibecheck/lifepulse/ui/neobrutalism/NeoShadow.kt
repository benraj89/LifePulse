package com.vibecheck.lifepulse.ui.neobrutalism

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws a solid, hard-edged (0% blur) offset "shadow" behind the composable using the given
 * [shape]. This emulates the classic Neobrutalism drop-shadow: a flat black silhouette offset
 * diagonally down-and-right from the component, with NO blur/soft falloff.
 *
 * Typically paired with a bold black border drawn on the component itself, and consumed via
 * [Modifier.neoHardShadow] for the common border+shadow combo.
 *
 * @param color Solid shadow color, defaults to pure black.
 * @param offsetX Horizontal shadow offset (right).
 * @param offsetY Vertical shadow offset (down).
 * @param shape Shape of the shadow silhouette; should generally match the component's own shape.
 */
fun Modifier.neobrutalismShadow(
    color: Color = NeoColors.Shadow,
    offsetX: Dp = 6.dp,
    offsetY: Dp = 6.dp,
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
): Modifier = this.drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    val path = when (outline) {
        is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
        is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
        is Outline.Generic -> outline.path
    }
    path.translate(Offset(offsetX.toPx(), offsetY.toPx()))
    drawPath(path = path, color = color)
}

/**
 * Combines a bold black border with a hard offset shadow in a single, reusable modifier chain.
 * This is the primary building block used by NeoCard/NeoButton/NeoChip/NeoTextField.
 *
 * Note: this only draws the shadow; the caller is still responsible for drawing the border
 * (typically via `Modifier.border(...)`), since border/background layering differs slightly
 * per-component. See NeoCard/NeoButton for full usage combining background, border and shadow.
 */
fun Modifier.neoHardShadow(
    shape: Shape,
    shadowColor: Color = NeoColors.Shadow,
    offsetX: Dp = 6.dp,
    offsetY: Dp = 6.dp
): Modifier = this.neobrutalismShadow(
    color = shadowColor,
    offsetX = offsetX,
    offsetY = offsetY,
    shape = shape
)






