package com.vibecheck.lifepulse.ui.neobrutalism

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * A Neobrutalism-styled month picker: a year header with chunky left/right arrow buttons
 * to page between years, and a 3x4 grid of month tiles below (Jan-Dec) for the given year.
 * Tapping a month invokes [onMonthSelected] with the resulting [YearMonth].
 *
 * Usage:
 * ```
 * NeoCalendarDialog(
 *     selectedMonth = state.selectedMonth,
 *     onMonthSelected = { viewModel.selectMonth(it) },
 *     onDismiss = { showCalendar = false }
 * )
 * ```
 */
@Composable
fun NeoCalendarDialog(
    selectedMonth: YearMonth,
    onMonthSelected: (YearMonth) -> Unit,
    onDismiss: () -> Unit
) {
    var displayedYear by remember(selectedMonth) {
        mutableStateOf(selectedMonth.year)
    }

    Dialog(onDismissRequest = onDismiss) {
        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = NeoColors.Surface,
            contentPadding = 16.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Year header with prev/next arrows: < 2026 >
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeoIconButton(
                        icon = Icons.Default.ChevronLeft,
                        contentDescription = "Previous year",
                        onClick = { displayedYear -= 1 },
                        backgroundColor = NeoColors.Secondary,
                        size = 36.dp,
                        borderWidth = 2.dp,
                        shadowOffset = 3.dp
                    )
                    Text(
                        text = displayedYear.toString(),
                        style = NeoTypography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = NeoColors.OnSurface,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    NeoIconButton(
                        icon = Icons.Default.ChevronRight,
                        contentDescription = "Next year",
                        onClick = { displayedYear += 1 },
                        backgroundColor = NeoColors.Secondary,
                        size = 36.dp,
                        borderWidth = 2.dp,
                        shadowOffset = 3.dp
                    )
                }

                // Jan..Dec grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(12) { index ->
                        val month = java.time.Month.of(index + 1)
                        val candidate = YearMonth.of(displayedYear, month)
                        val isSelected = candidate == selectedMonth
                        NeoChip(
                            text = month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                            selected = isSelected,
                            selectedColor = NeoColors.Primary,
                            unselectedColor = NeoColors.Background,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                onMonthSelected(candidate)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}



