package com.vibecheck.lifepulse.ui.neobrutalism

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A bold Neobrutalism button: solid fill, thick black border, and a hard offset shadow.
 * On press, the button visually shifts down-and-right to "overlap" its shadow, simulating a
 * physical push into the surface, then springs back on release.
 *
 * Usage:
 * ```
 * NeoButton(text = "Add Expense", onClick = { ... }, backgroundColor = NeoColors.Primary)
 * NeoButton(text = "Delete", onClick = { ... }, backgroundColor = NeoColors.Danger, contentColor = Color.White)
 * ```
 */
@Composable
fun NeoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = NeoColors.Primary,
    contentColor: Color = NeoColors.OnPrimary,
    borderColor: Color = NeoColors.Border,
    shadowColor: Color = NeoColors.Shadow,
    shape: Shape = RoundedCornerShape(12.dp),
    borderWidth: Dp = 3.dp,
    shadowOffset: Dp = 6.dp,
    horizontalPadding: Dp = 20.dp,
    verticalPadding: Dp = 14.dp,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Press-down: button travels toward its shadow, shrinking the visible offset to ~0.
    val pressOffset by animateDpAsState(
        targetValue = if (isPressed && enabled) shadowOffset else 0.dp,
        animationSpec = tween(durationMillis = 90),
        label = "neoButtonPressOffset"
    )
    val remainingShadow = shadowOffset - pressOffset

    Box(
        modifier = modifier
            .neoHardShadow(
                shape = shape,
                shadowColor = shadowColor,
                offsetX = remainingShadow,
                offsetY = remainingShadow
            )
            .offset(x = pressOffset, y = pressOffset)
            .background(
                color = if (enabled) backgroundColor else NeoColors.Muted,
                shape = shape
            )
            .border(width = borderWidth, color = borderColor, shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .semantics { contentDescription = text }
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                androidx.compose.foundation.layout.Spacer(Modifier.padding(start = 6.dp))
            }
            androidx.compose.material3.Text(
                text = text,
                color = if (enabled) contentColor else NeoColors.Black.copy(alpha = 0.4f),
                style = NeoTypography.labelLarge
            )
        }
    }
}



