package com.vibecheck.lifepulse.ui.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecheck.lifepulse.R
import com.vibecheck.lifepulse.core.DateUtils
import com.vibecheck.lifepulse.domain.model.Expense
import com.vibecheck.lifepulse.ui.components.AddExpenseSheet
import com.vibecheck.lifepulse.ui.components.ColorDot
import com.vibecheck.lifepulse.ui.components.EmptyState
import com.vibecheck.lifepulse.ui.dashboard.asCurrency
import com.vibecheck.lifepulse.ui.neobrutalism.NeoCalendarDialog
import com.vibecheck.lifepulse.ui.neobrutalism.NeoCard
import com.vibecheck.lifepulse.ui.neobrutalism.NeoColors
import com.vibecheck.lifepulse.ui.neobrutalism.NeoFab
import com.vibecheck.lifepulse.ui.neobrutalism.NeoIconButton
import com.vibecheck.lifepulse.ui.neobrutalism.NeoTypography
import com.vibecheck.lifepulse.ui.neobrutalism.neoHardShadow
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    modifier: Modifier = Modifier,
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSheet by remember { mutableStateOf(false) }
    var showCalendar by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = NeoColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.expense_title),
                        style = NeoTypography.headlineMedium,
                        color = NeoColors.OnSurface
                    )
                },
                actions = {
                    Row(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .neoHardShadow(
                                shape = RoundedCornerShape(10.dp),
                                shadowColor = NeoColors.Shadow,
                                offsetX = 3.dp,
                                offsetY = 3.dp
                            )
                            .background(NeoColors.Accent, RoundedCornerShape(10.dp))
                            .border(width = 2.dp, color = NeoColors.Border, shape = RoundedCornerShape(10.dp))
                            .clickable { showCalendar = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = stringResource(R.string.expense_pick_month_content_description),
                            tint = NeoColors.OnSurface,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = "${state.selectedMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${state.selectedMonth.year}",
                            style = NeoTypography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = NeoColors.OnSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NeoColors.Background,
                    titleContentColor = NeoColors.OnSurface
                )
            )
        },
        floatingActionButton = {
            NeoFab(
                icon = Icons.Default.Add,
                contentDescription = stringResource(R.string.expense_add_content_description),
                onClick = { showSheet = true }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(NeoColors.Background)
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                NeoCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = NeoColors.Accent,
                    contentPadding = 20.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                stringResource(R.string.expense_today_label),
                                style = NeoTypography.labelLarge,
                                color = NeoColors.OnSurface
                            )
                            Text(
                                state.totalToday.asCurrency(),
                                style = NeoTypography.headlineMedium,
                                color = NeoColors.OnSurface
                            )
                        }
                        Column {
                            Text(
                                stringResource(R.string.expense_this_month_label),
                                style = NeoTypography.labelLarge,
                                color = NeoColors.OnSurface
                            )
                            Text(
                                state.totalSelectedMonth.asCurrency(),
                                style = NeoTypography.headlineMedium,
                                color = NeoColors.OnSurface
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    stringResource(R.string.expense_recent_transactions),
                    style = NeoTypography.titleLarge,
                    color = NeoColors.OnSurface,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }

            if (state.expensesByDay.isEmpty()) {
                item {
                    EmptyState(
                        stringResource(R.string.expense_empty_title),
                        stringResource(R.string.expense_empty_subtitle),
                        imageRes = R.drawable.empty_expense,
                        imageSize = 350.dp
                    )
                }
            } else {
                state.expensesByDay.forEach { dayGroup ->
                    item(key = "header_${dayGroup.date}") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = DateUtils.formatTimestamp(dayGroup.date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(), pattern = "EEE, dd MMM"),
                                style = NeoTypography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = NeoColors.OnSurface
                            )
                            Text(
                                text = dayGroup.total.asCurrency(),
                                style = NeoTypography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = NeoColors.OnSurface
                            )
                        }
                    }
                    items(dayGroup.expenses, key = { it.id }) { expense ->
                        ExpenseListItem(expense) { viewModel.deleteExpense(expense.id) }
                    }
                }
            }
        }
    }

    if (showSheet) {
        AddExpenseSheet(
            categories = state.categories,
            onDismiss = { showSheet = false },
            onSave = { categoryId, amount, note, timestamp ->
                viewModel.addExpense(categoryId, amount, note, timestamp)
            },
            onAddCategory = { name, colorHex -> viewModel.addCategory(name, colorHex) },
            onDeleteCategory = { category -> viewModel.deleteCategory(category.id) }
        )
    }

    if (showCalendar) {
        NeoCalendarDialog(
            selectedMonth = state.selectedMonth,
            onMonthSelected = { viewModel.selectMonth(it) },
            onDismiss = { showCalendar = false }
        )
    }
}

@Composable
private fun ExpenseListItem(expense: Expense, onDelete: () -> Unit) {
    NeoCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = NeoColors.Surface,
        shape = RoundedCornerShape(12.dp),
        borderWidth = 2.dp,
        shadowOffsetX = 4.dp,
        shadowOffsetY = 4.dp,
        contentPadding = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ColorDot(expense.categoryColorHex, size = 16)
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.categoryName, style = NeoTypography.titleMedium, color = NeoColors.OnSurface)
                Text(
                    listOfNotNull(
                        expense.note.takeIf { it.isNotBlank() },
                        DateUtils.formatTimestamp(expense.dateTimestamp)
                    ).joinToString(" • "),
                    style = NeoTypography.bodyMedium,
                    color = NeoColors.OnSurface.copy(alpha = 0.7f)
                )
            }
            Text(
                expense.amount.asCurrency(),
                style = NeoTypography.titleMedium,
                fontWeight = FontWeight.Black,
                color = NeoColors.OnSurface
            )
            NeoIconButton(
                icon = Icons.Default.Delete,
                contentDescription = stringResource(R.string.expense_delete_content_description),
                onClick = onDelete,
                backgroundColor = NeoColors.Danger,
                iconTint = NeoColors.White,
                size = 30.dp,
                borderWidth = 1.5.dp,
                shadowOffset = 2.dp
            )
        }
    }
}

