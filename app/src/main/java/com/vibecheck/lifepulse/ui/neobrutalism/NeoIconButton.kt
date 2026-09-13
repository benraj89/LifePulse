package com.vibecheck.lifepulse.ui.neobrutalism

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A small square icon-only button styled to match the Neobrutalism kit: solid fill, thick
 * black border, and a hard offset shadow that collapses on press (the button travels toward
 * its shadow, simulating a physical push), then springs back on release. Use this in place of
 * a plain Material [androidx.compose.material3.IconButton] wherever an icon action needs to
 * match the bold, chunky aesthetic instead of looking like a native/system control.
 *
 * Usage:
 * ```
 * NeoIconButton(
 *     icon = Icons.Default.Delete,
 *     contentDescription = "Delete",
 *     onClick = onDelete,
 *     backgroundColor = NeoColors.Danger,
 *     iconTint = NeoColors.White
 * )
 * ```
 */
@Composable
fun NeoIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = NeoColors.Surface,
    borderColor: Color = NeoColors.Border,
    shadowColor: Color = NeoColors.Shadow,
    iconTint: Color = NeoColors.OnSurface,
    shape: Shape = RoundedCornerShape(10.dp),
    size: Dp = 40.dp,
    borderWidth: Dp = 2.dp,
    shadowOffset: Dp = 4.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressOffset by animateDpAsState(
        targetValue = if (isPressed && enabled) shadowOffset else 0.dp,
        animationSpec = tween(durationMillis = 90),
        label = "neoIconButtonPressOffset"
    )
    val remainingShadow = shadowOffset - pressOffset

    Box(
        modifier = modifier
            .size(size)
            .neoHardShadow(
                shape = shape,
                shadowColor = shadowColor,
                offsetX = remainingShadow,
                offsetY = remainingShadow
            )
            .offset { androidx.compose.ui.unit.IntOffset(pressOffset.roundToPx(), pressOffset.roundToPx()) }
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
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}



