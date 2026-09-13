package com.vibecheck.lifepulse.ui.neobrutalism

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A bold, y2k/neobrutalism "sticker" section header used to separate groups of content
 * (e.g. habits by frequency). Rendered as a slightly rotated banner with a thick black
 * outline, hard offset shadow, a chunky emoji/icon badge, an uppercase label and a
 * pill-shaped count badge — designed to pop off the page.
 *
 * Usage:
 * ```
 * NeoSectionHeader(title = "Daily", color = NeoColors.Lime, icon = "☀️", count = 3)
 * ```
 */
@Composable
fun NeoSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = NeoColors.Primary,
    icon: String? = null,
    count: Int? = null,
    rotationDegrees: Float = -1.5f
) {
    Box(
        modifier = modifier
            .rotate(rotationDegrees)
            .neoHardShadow(
                shape = RoundedCornerShape(14.dp),
                shadowColor = NeoColors.Shadow,
                offsetX = 5.dp,
                offsetY = 5.dp
            )
            .background(color = color, shape = RoundedCornerShape(14.dp))
            .border(width = 3.dp, color = NeoColors.Border, shape = RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(color = NeoColors.White, shape = RoundedCornerShape(8.dp))
                        .border(width = 2.dp, color = NeoColors.Border, shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = icon, style = NeoTypography.labelLarge)
                }
            }
            Text(
                text = title.uppercase(),
                style = NeoTypography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = NeoColors.OnSurface,
                modifier = Modifier.padding(start = if (icon != null) 10.dp else 0.dp)
            )
            if (count != null) {
                Box(
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .background(color = NeoColors.OnSurface, shape = RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = count.toString(),
                        style = NeoTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = color
                    )
                }
            }
        }
    }
}

