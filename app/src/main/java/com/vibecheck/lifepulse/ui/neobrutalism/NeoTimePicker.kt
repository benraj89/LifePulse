package com.vibecheck.lifepulse.ui.neobrutalism

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * A Neobrutalism-styled time picker: two chunky steppers (hour/minute) with up/down arrow
 * buttons, wrapped in a bold stacked dialog with hard industrial contrast.
 *
 * Usage:
 * ```
 * NeoTimePickerDialog(
 *     initialHour = 9, initialMinute = 0,
 *     onConfirm = { h, m -> hour = h; minute = m; showPicker = false },
 *     onDismiss = { showPicker = false }
 * )
 * ```
 */
@Composable
fun NeoTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    title: String = "Set reminder time",
    confirmText: String = "OK",
    cancelText: String = "Cancel",
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val initialHour24 = initialHour.coerceIn(0, 23)
    var hour12 by remember {
        mutableIntStateOf(
            when (val h = initialHour24 % 12) {
                0 -> 12
                else -> h
            }
        )
    }
    var isPm by remember { mutableStateOf(initialHour24 >= 12) }
    var minute by remember { mutableIntStateOf(initialMinute.coerceIn(0, 59)) }

    Dialog(onDismissRequest = onDismiss) {
        NeoStackedDialogCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            backLayerColor = NeoColors.Orange,
            backRotation = 0f,
            tapeText = "SET TIME",
            tapeColor = NeoColors.Cyan,
            contentPadding = 20.dp,
            verticalSpacing = 16.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title.uppercase(),
                    style = NeoTypography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = NeoColors.OnSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(NeoColors.Cyan, RoundedCornerShape(2.dp))
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeoNumberStepper(
                    value = hour12,
                    range = 1..12,
                    valueColor = NeoColors.Orange,
                    onValueChange = { hour12 = it }
                )
                Text(
                    text = ":",
                    style = NeoTypography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = NeoColors.OnSurface,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                NeoNumberStepper(
                    value = minute,
                    range = 0..59,
                    step = 5,
                    valueColor = NeoColors.Cyan,
                    onValueChange = { minute = it }
                )
            }

            NeoSegmentedToggle(
                selectedIndex = if (isPm) 1 else 0,
                options = listOf("AM" to NeoColors.Lime, "PM" to NeoColors.Cyan),
                onSelectedIndexChange = { isPm = it == 1 }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NeoButton(
                    text = cancelText.uppercase(),
                    onClick = onDismiss,
                    backgroundColor = NeoColors.Muted,
                    modifier = Modifier.weight(1f)
                )
                NeoButton(
                    text = confirmText.uppercase(),
                    onClick = {
                        val hour24 = when {
                            !isPm && hour12 == 12 -> 0
                            isPm && hour12 != 12 -> hour12 + 12
                            else -> hour12
                        }
                        onConfirm(hour24, minute)
                    },
                    backgroundColor = NeoColors.Lime,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * A reusable chunky number stepper: up-arrow, a bordered value box, down-arrow. Wraps around
 * the given [range]; [step] controls the increment (defaults to 1).
 */
@Composable
fun NeoNumberStepper(
    value: Int,
    range: IntRange,
    modifier: Modifier = Modifier,
    step: Int = 1,
    valueColor: androidx.compose.ui.graphics.Color = NeoColors.Background,
    onValueChange: (Int) -> Unit
) {
    val safeStep = step.coerceAtLeast(1)
    val wrappedLast = range.last - ((range.last - range.first) % safeStep)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        NeoIconButton(
            icon = Icons.Default.KeyboardArrowUp,
            contentDescription = "Increase",
            onClick = {
                val next = value + safeStep
                onValueChange(if (next > range.last) range.first else next)
            },
            backgroundColor = valueColor,
            iconTint = NeoColors.OnSurface,
            size = 36.dp,
            borderWidth = 2.dp,
            shadowOffset = 3.dp
        )
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(width = 72.dp, height = 56.dp)
                .neoHardShadow(
                    shape = RoundedCornerShape(10.dp),
                    shadowColor = NeoColors.Shadow,
                    offsetX = 3.dp,
                    offsetY = 3.dp
                )
                .background(valueColor, RoundedCornerShape(10.dp))
                .border(3.dp, NeoColors.Border, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value.toString().padStart(2, '0'),
                style = NeoTypography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = NeoColors.OnSurface
            )
        }
        NeoIconButton(
            icon = Icons.Default.KeyboardArrowDown,
            contentDescription = "Decrease",
            onClick = {
                val next = value - safeStep
                onValueChange(if (next < range.first) wrappedLast else next)
            },
            backgroundColor = valueColor,
            iconTint = NeoColors.OnSurface,
            size = 36.dp,
            borderWidth = 2.dp,
            shadowOffset = 3.dp
        )
    }
}



