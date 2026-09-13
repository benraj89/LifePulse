package com.vibecheck.lifepulse.ui.neobrutalism

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A small pill-shaped (or rounded-rect) filter tag with a thick outline. Supports a
 * "selected" state that swaps in a bold accent fill, common for filter bars.
 *
 * Usage:
 * ```
 * NeoChip(text = "Health", selected = true, onClick = { ... }, selectedColor = NeoColors.Lime)
 * ```
 */
@Composable
fun NeoChip(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    selectedColor: Color = NeoColors.Primary,
    unselectedColor: Color = NeoColors.Surface,
    borderColor: Color = NeoColors.Border,
    contentColor: Color = NeoColors.OnSurface,
    shape: Shape = RoundedCornerShape(50), // pill shape
    borderWidth: Dp = 2.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .background(
                color = if (selected) selectedColor else unselectedColor,
                shape = shape
            )
            .border(width = borderWidth, color = borderColor, shape = shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = NeoTypography.labelLarge,
            color = contentColor
        )
    }
}

