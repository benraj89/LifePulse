package com.vibecheck.lifepulse.ui.habits

import com.vibecheck.lifepulse.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vibecheck.lifepulse.domain.model.HabitFrequency
import com.vibecheck.lifepulse.ui.neobrutalism.NeoColors
import com.vibecheck.lifepulse.ui.neobrutalism.NeoTypography

/**
 * Shared small helpers/composables used across the habit dialog pieces.
 */
internal fun Modifier.clickableNoRippleShared(onClick: () -> Unit): Modifier = composed {
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}

@Composable
internal fun NeoLabel(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(width = 18.dp, height = 8.dp)
                .background(NeoColors.Cyan, RoundedCornerShape(2.dp))
        )
        Text(
            text = text.uppercase(),
            style = NeoTypography.labelLarge,
            fontWeight = FontWeight.Black,
            color = NeoColors.OnSurface
        )
    }
}

internal fun frequencyColor(frequency: HabitFrequency): Color = when (frequency) {
    HabitFrequency.DAILY -> NeoColors.Lime
    HabitFrequency.WEEKLY -> NeoColors.Cyan
    HabitFrequency.MONTHLY -> NeoColors.Orange
}

@Composable
internal fun frequencyIcon(frequency: HabitFrequency): String = when (frequency) {
    HabitFrequency.DAILY -> stringResource(R.string.frequency_daily_code)
    HabitFrequency.WEEKLY -> stringResource(R.string.frequency_weekly_code)
    HabitFrequency.MONTHLY -> stringResource(R.string.frequency_monthly_code)
}

