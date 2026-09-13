package com.vibecheck.lifepulse.ui.neobrutalism

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A chunky hardware-style on/off toggle built from scratch (instead of Material's
 * [androidx.compose.material3.Switch]) to match the bold Neobrutalism kit: squared track,
 * thick black border, hard shadow, and a block thumb with clear ON/OFF states.
 *
 * Usage:
 * ```
 * NeoSwitch(checked = enabled, onCheckedChange = { enabled = it })
 * ```
 */
@Composable
fun NeoSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onColor: Color = NeoColors.Lime,
    offColor: Color = NeoColors.Muted,
    thumbColor: Color = NeoColors.Surface
) {
    val interactionSource = remember { MutableInteractionSource() }
    val trackWidth = 76.dp
    val trackHeight = 38.dp
    val thumbSize = 28.dp
    val switchShape = RoundedCornerShape(8.dp)
    val thumbShape = RoundedCornerShape(6.dp)
    val travel = trackWidth - thumbSize - 8.dp

    val trackColor by animateColorAsState(
        targetValue = if (checked) onColor else offColor,
        label = "neoSwitchTrack"
    )

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) travel else 0.dp,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 380f),
        label = "neoSwitchThumb"
    )

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .neoHardShadow(
                shape = switchShape,
                shadowColor = NeoColors.Shadow,
                offsetX = 4.dp,
                offsetY = 4.dp
            )
            .background(color = trackColor, shape = switchShape)
            .border(width = 3.dp, color = NeoColors.Border, shape = switchShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = if (checked) "ON" else "OFF",
            style = NeoTypography.labelSmall,
            fontWeight = FontWeight.Black,
            color = NeoColors.Graphite,
            modifier = Modifier
                .align(if (checked) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(horizontal = 8.dp)
        )
        Box(
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(thumbOffset.roundToPx(), 0) }
                .size(thumbSize)
                .background(color = thumbColor, shape = thumbShape)
                .border(width = 2.dp, color = NeoColors.Border, shape = thumbShape)
        )
    }
}

/**
 * A bold segmented toggle for exactly two (or more) options (e.g. AM/PM). Each segment uses
 * hard fills, thick borders, and squared geometry so it reads like a control panel.
 *
 * Usage:
 * ```
 * NeoSegmentedToggle(
 *     selectedIndex = if (isPm) 1 else 0,
 *     options = listOf("AM" to NeoColors.Lime, "PM" to NeoColors.Cyan),
 *     onSelectedIndexChange = { isPm = it == 1 }
 * )
 * ```
 */
@Composable
fun NeoSegmentedToggle(
    selectedIndex: Int,
    options: List<Pair<String, Color>>,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .neoHardShadow(
                shape = RoundedCornerShape(10.dp),
                shadowColor = NeoColors.Shadow,
                offsetX = 4.dp,
                offsetY = 4.dp
            )
            .background(color = NeoColors.Concrete, shape = RoundedCornerShape(10.dp))
            .border(width = 3.dp, color = NeoColors.Border, shape = RoundedCornerShape(10.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, (label, color) ->
            val selected = index == selectedIndex
            val animatedColor by animateColorAsState(
                targetValue = if (selected) color else Color.Transparent,
                label = "neoSegmentColor"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(color = animatedColor, shape = RoundedCornerShape(7.dp))
                    .then(
                        if (selected) {
                            Modifier.border(width = 2.5.dp, color = NeoColors.Border, shape = RoundedCornerShape(7.dp))
                        } else {
                            Modifier
                        }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelectedIndexChange(index) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = NeoTypography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = NeoColors.OnSurface
                )
            }
        }
    }
}







