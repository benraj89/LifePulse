package com.vibecheck.lifepulse.ui.dashboard

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vibecheck.lifepulse.ui.neobrutalism.NeoColors
import com.vibecheck.lifepulse.ui.neobrutalism.NeoTypography
import com.vibecheck.lifepulse.ui.neobrutalism.NeoNumberFont

/** Dashboard aliases share the same compact comic roles as the other screens. */
internal object DashboardDesign {
    val Canvas = NeoColors.Background
    val Paper = NeoColors.Surface
    val Ink = NeoColors.Ink
    val SecondaryInk = NeoColors.SecondaryText
    val Habit = NeoColors.SkyBlue
    val Expense = NeoColors.Coral
    val Action = NeoColors.Yellow
    val Completed = NeoColors.Success
    val Disabled = NeoColors.Concrete
    val Rule = Color(0xFFC8C3B5)
    val Shape = RoundedCornerShape(8.dp)
    val ControlShape = RoundedCornerShape(4.dp)
    val Gutter = 12.dp
    val Gap = 8.dp
    val SmallGap = 4.dp
    val SectionGap = 16.dp
    val TwoColumnWidth = 720.dp
    val MaxContentWidth = 1120.dp

    val Heading = NeoTypography.headlineMedium
    val Metric = NeoTypography.headlineLarge
    val Title = NeoTypography.titleLarge
    val Body = NeoTypography.bodyMedium
    val Label = NeoTypography.labelLarge
    val Meta = NeoTypography.bodySmall
    val Amount = NeoTypography.titleMedium.copy(fontFamily = NeoNumberFont)
}
