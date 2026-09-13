package com.vibecheck.lifepulse.ui.neobrutalism

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val WEEKDAY_LABELS = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")

/**
 * A Neobrutalism-styled day picker: a month/year header with chunky left/right arrow
 * buttons to page between months, weekday labels, and a 7-column grid of day tiles.
 * Tapping a day invokes [onDateSelected] with the resulting [LocalDate] and closes the dialog.
 *
 * Usage:
 * ```
 * NeoDatePickerDialog(
 *     selectedDate = selectedDate,
 *     onDateSelected = { selectedDate = it },
 *     onDismiss = { showDatePicker = false }
 * )
 * ```
 */
@Composable
fun NeoDatePickerDialog(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var displayedMonth by remember(selectedDate) {
        mutableStateOf(YearMonth.from(selectedDate))
    }
    val today = remember { LocalDate.now() }

    Dialog(onDismissRequest = onDismiss) {
        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = NeoColors.Surface,
            contentPadding = 16.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Month/year header with prev/next arrows
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeoIconButton(
                        icon = Icons.Default.ChevronLeft,
                        contentDescription = "Previous month",
                        onClick = { displayedMonth = displayedMonth.minusMonths(1) },
                        backgroundColor = NeoColors.Secondary,
                        size = 36.dp,
                        borderWidth = 2.dp,
                        shadowOffset = 3.dp
                    )
                    Text(
                        text = "${displayedMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${displayedMonth.year}",
                        style = NeoTypography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = NeoColors.OnSurface
                    )
                    NeoIconButton(
                        icon = Icons.Default.ChevronRight,
                        contentDescription = "Next month",
                        onClick = { displayedMonth = displayedMonth.plusMonths(1) },
                        backgroundColor = NeoColors.Secondary,
                        size = 36.dp,
                        borderWidth = 2.dp,
                        shadowOffset = 3.dp
                    )
                }

                // Weekday labels
                Row(modifier = Modifier.fillMaxWidth()) {
                    WEEKDAY_LABELS.forEach { label ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .size(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = NeoTypography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = NeoColors.OnSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // Day grid: Monday-first weeks covering the whole month
                val firstDay = displayedMonth.atDay(1)
                val daysInMonth = displayedMonth.lengthOfMonth()
                val leadingBlanks = firstDay.dayOfWeek.value - 1 // Monday=0..Sunday=6
                val totalCells = leadingBlanks + daysInMonth
                val rows = (totalCells + 6) / 7

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (row in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            for (col in 0..6) {
                                val cellIndex = row * 7 + col
                                val dayNumber = cellIndex - leadingBlanks + 1
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(2.dp)
                                        .size(34.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (dayNumber in 1..daysInMonth) {
                                        val cellDate = displayedMonth.atDay(dayNumber)
                                        val isSelected = cellDate == selectedDate
                                        val isToday = cellDate == today
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .background(
                                                    color = if (isSelected) NeoColors.Primary else NeoColors.Background,
                                                    shape = CircleShape
                                                )
                                                .border(
                                                    width = if (isSelected || isToday) 2.dp else 0.dp,
                                                    color = NeoColors.Border,
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    onDateSelected(cellDate)
                                                    onDismiss()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dayNumber.toString(),
                                                style = NeoTypography.labelLarge,
                                                fontWeight = if (isSelected || isToday) FontWeight.Black else FontWeight.Normal,
                                                color = NeoColors.OnSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}



