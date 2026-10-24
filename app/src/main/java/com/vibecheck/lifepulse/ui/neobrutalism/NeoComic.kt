package com.vibecheck.lifepulse.ui.neobrutalism

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/** Small print-label treatment: display lettering stays out of long names and amounts. */
@Composable
fun NeoSticker(text: String, modifier: Modifier = Modifier, color: Color = NeoColors.Yellow, tilted: Boolean = true) {
    val shape = RoundedCornerShape(2.dp)
    Text(text, color = NeoColors.Ink,
        style = NeoTypography.labelSmall.copy(fontFamily = NeoDisplayFont, fontSize = 11.sp, lineHeight = 16.sp),
        modifier = modifier.rotate(if (tilted) -2f else 0f)
            .neoHardShadow(shape, offsetX = 2.dp, offsetY = 2.dp)
            .background(color, shape).border(1.5.dp, NeoColors.Ink, shape)
            .padding(horizontal = 7.dp, vertical = 3.dp))
}

/** A purely decorative comic sparkle, drawn natively rather than added to accessibility output. */
@Composable
fun NeoBurst(modifier: Modifier = Modifier, color: Color = NeoColors.Yellow) {
    Canvas(modifier.size(26.dp)) {
        val path = Path()
        repeat(16) { point ->
            val angle = point * Math.PI / 8 - Math.PI / 2
            val radius = size.minDimension * if (point % 2 == 0) .46f else .25f
            val x = center.x + cos(angle).toFloat() * radius
            val y = center.y + sin(angle).toFloat() * radius
            if (point == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color)
        drawPath(path, NeoColors.Ink, style = Stroke(1.5.dp.toPx()))
    }
}

/** Visual only: the enclosing row owns checkbox semantics and the full touch target. */
@Composable
fun NeoCheck(checked: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.size(24.dp)) {
        val inset = 2.dp.toPx()
        val box = Size(size.width - inset, size.height - inset)
        val radius = CornerRadius(2.dp.toPx())
        drawRoundRect(NeoColors.Ink, Offset(inset, inset), box, radius)
        drawRoundRect(if (checked) NeoColors.Yellow else NeoColors.Surface, size = box, cornerRadius = radius)
        drawRoundRect(NeoColors.Ink, size = box, cornerRadius = radius, style = Stroke(2.dp.toPx()))
        if (checked) {
            val tick = Path().apply {
                moveTo(box.width * .22f, box.height * .52f)
                lineTo(box.width * .43f, box.height * .72f)
                lineTo(box.width * .80f, box.height * .28f)
            }
            drawPath(tick, NeoColors.Ink, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF6E7)
@Composable
private fun ComicDetailsPreview() {
    NeoBrutalismTheme {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            NeoSticker("Today")
            NeoSticker("This week", color = NeoColors.LightViolet)
            NeoBurst()
            NeoCheck(false)
            NeoCheck(true)
        }
    }
}
