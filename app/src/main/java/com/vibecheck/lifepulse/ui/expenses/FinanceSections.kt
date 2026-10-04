package com.vibecheck.lifepulse.ui.expenses

import com.vibecheck.lifepulse.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import com.vibecheck.lifepulse.core.*
import com.vibecheck.lifepulse.domain.model.*
import com.vibecheck.lifepulse.ui.components.ColorDot
import com.vibecheck.lifepulse.ui.components.toComposeColor
import com.vibecheck.lifepulse.ui.neobrutalism.*
import java.time.format.DateTimeFormatter

internal fun LazyListScope.spendingSection(
    state: ExpenseUiState, filter: String, onFilter: (String) -> Unit,
    onCalendar: () -> Unit, onBreakdown: () -> Unit,
    onAccount: (Long?) -> Unit, onEdit: (Expense) -> Unit
) {
    item { SpendingSummary(state, onCalendar, onBreakdown) }
    item {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.finance_your_entries), style = NeoTypography.titleLarge)
            EntryFilterMenu(state, filter, onFilter, onAccount)
        }
        val accountName = state.accounts.firstOrNull { it.id == state.selectedAccountId }?.name
        if (filter != "All" || accountName != null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(listOfNotNull(if (filter != "All") entryFilterLabel(filter) else null, accountName).joinToString(" · "), modifier = Modifier.weight(1f), style = NeoTypography.bodySmall)
                TextButton(onClick = { onFilter("All"); onAccount(null) }) { Text(stringResource(R.string.action_clear), color = NeoColors.OnSurface) }
            }
        } else Text(stringResource(R.string.finance_edit_hint), style = NeoTypography.bodySmall)
    }
    val groups = state.expensesByDay.map { group -> group.copy(expenses = group.expenses.filter {
        (it.type.needsCategory || it.type == TransactionType.TRANSFER) &&
            (filter == "All" || (filter == "Expenses" && it.type == TransactionType.EXPENSE) ||
            (filter == "Income" && it.type == TransactionType.INCOME) || (filter == "Transfers" && it.type == TransactionType.TRANSFER))
    }) }.filter { it.expenses.isNotEmpty() }
    if (state.isLoading) item { Text(stringResource(R.string.finance_loading_entries)) }
    else if (groups.isEmpty()) item { FinanceEmpty(stringResource(R.string.finance_empty_title), stringResource(R.string.finance_empty_hint)) }
    groups.forEach { group ->
        item(key = "date_${group.date}") { Text(group.date.format(DateTimeFormatter.ofPattern("EEE, dd MMM")), style = NeoTypography.labelLarge) }
        items(group.expenses, key = { it.id }) { FinanceEntryRow(it, state.accounts, { onEdit(it) }) }
    }
}

@Composable
private fun SpendingSummary(state: ExpenseUiState, onCalendar: () -> Unit, onBreakdown: () -> Unit) {
    FinanceSummaryCard(stringResource(R.string.finance_monthly_spending), NeoColors.Yellow, NeoColors.Lime) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { SummaryAmount(state.spentMinor) }
                Box(Modifier.background(NeoColors.Cyan).border(2.dp, NeoColors.Border).clickable(onClick = onCalendar)
                    .heightIn(min = 48.dp).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                    Text(state.selectedMonth.format(DateTimeFormatter.ofPattern("MMM yyyy")) + " ▾",
                        style = NeoTypography.labelSmall, fontWeight = FontWeight.Black, color = NeoColors.OnSurface)
                }
            }
            HorizontalDivider(thickness = 3.dp, color = NeoColors.Border)
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                SummaryStat(stringResource(R.string.finance_spent_today), state.totalToday, Modifier.weight(1f).fillMaxHeight().background(NeoColors.Surface))
                Box(Modifier.width(3.dp).fillMaxHeight().background(NeoColors.Border))
                SummaryStat(stringResource(R.string.finance_month_income), state.incomeMinor, Modifier.weight(1f).fillMaxHeight().background(NeoColors.MintGreen))
            }
            HorizontalDivider(thickness = 3.dp, color = NeoColors.Border)
            Row(Modifier.fillMaxWidth().background(NeoColors.Surface).clickable(onClick = onBreakdown)
                .heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.finance_category_breakdown), style = NeoTypography.labelLarge, fontWeight = FontWeight.Black, color = NeoColors.OnSurface,
                    modifier = Modifier.weight(1f))
                Text("→", style = NeoTypography.titleLarge, fontWeight = FontWeight.Black, color = NeoColors.OnSurface)
            }
    }
}

@Composable
private fun SummaryStat(label: String, amount: Long, modifier: Modifier) {
    Column(modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = NeoTypography.labelSmall, fontWeight = FontWeight.Black, color = NeoColors.OnSurface)
        Text(Money.format(amount), style = NeoTypography.titleMedium, fontWeight = FontWeight.Black, color = NeoColors.OnSurface)
    }
}

internal fun LazyListScope.accountsSection(state: ExpenseUiState, onAccount: (Account?) -> Unit,
                                          onCurrency: () -> Unit, onEdit: (Expense) -> Unit) {
    item {
        FinanceSummaryCard(stringResource(R.string.finance_total_balance), NeoColors.MintGreen, NeoColors.Cyan) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                SummaryAmount(state.balances.values.sum())
            }
            HorizontalDivider(thickness = 3.dp, color = NeoColors.Border)
            Text(stringResource(R.string.finance_balance_hint), style = NeoTypography.bodySmall,
                modifier = Modifier.fillMaxWidth().background(NeoColors.Surface).padding(horizontal = 12.dp, vertical = 8.dp))
        }
    }
    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.finance_your_accounts), style = NeoTypography.titleLarge)
            NeoIconButton(Icons.Default.Add, stringResource(R.string.account_add), { onAccount(null) }, size = 40.dp, shadowOffset = 3.dp)
        }
    }
    items(state.accounts, key = { "account_${it.id}" }) { account ->
        NeoCard(Modifier.fillMaxWidth().padding(end = 3.dp, bottom = 3.dp).clickable { onAccount(account) },
            backgroundColor = NeoColors.Surface, shape = RoundedCornerShape(4.dp), borderWidth = 3.dp,
            shadowOffsetX = 6.dp, shadowOffsetY = 6.dp, contentPadding = 3.dp) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(10.dp).fillMaxHeight().background(NeoColors.Cyan))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(account.name, style = NeoTypography.titleMedium, fontWeight = FontWeight.Black)
                    Text(Money.format(state.balances[account.id] ?: account.openingMinor), style = NeoTypography.titleLarge, fontWeight = FontWeight.Black)
                }
                Icon(Icons.Default.Edit, stringResource(R.string.account_edit), modifier = Modifier.padding(end = 16.dp).size(16.dp), tint = NeoColors.OnSurface)
            }
        }
    }
    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.currency_current_format, Money.currencyCode), style = NeoTypography.bodyMedium)
            TextButton(onClick = onCurrency) { Text(stringResource(R.string.action_change), color = NeoColors.OnSurface) }
        }
        Text(stringResource(R.string.finance_transfers), style = NeoTypography.titleLarge)
    }
    val transfers = state.transactions.filter { it.type == TransactionType.TRANSFER }.sortedByDescending { it.dateTimestamp }
    if (transfers.isEmpty()) item { Text(stringResource(R.string.finance_transfers_empty), style = NeoTypography.bodyMedium) }
    items(transfers, key = { "transfer_${it.id}" }) { FinanceEntryRow(it, state.accounts, { onEdit(it) }, showDate = true) }
}

internal fun LazyListScope.owedSection(state: ExpenseUiState, showSettled: Boolean, onSettled: (Boolean) -> Unit,
                                      onEdit: (Expense) -> Unit, onRepay: (Expense) -> Unit) {
    item {
        FinanceSummaryCard(stringResource(R.string.finance_owed_heading), NeoColors.Cyan, NeoColors.Yellow) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                SummaryStat(stringResource(R.string.finance_people_owe_you), state.outstandingLoans.filter { it.type == TransactionType.LEND }
                    .sumOf { Ledger.outstanding(it, state.transactions) }, Modifier.weight(1f).fillMaxHeight())
                Box(Modifier.width(3.dp).fillMaxHeight().background(NeoColors.Border))
                SummaryStat(stringResource(R.string.finance_you_owe_people), state.outstandingLoans.filter { it.type == TransactionType.BORROW }
                    .sumOf { Ledger.outstanding(it, state.transactions) }, Modifier.weight(1f).fillMaxHeight().background(NeoColors.Yellow))
            }
        }
    }
    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.finance_show_repaid), style = NeoTypography.bodyMedium)
            Checkbox(showSettled, onSettled, colors = CheckboxDefaults.colors(checkedColor = NeoColors.OnSurface, checkmarkColor = NeoColors.Surface, uncheckedColor = NeoColors.Border))
        }
    }
    val loans = (if (showSettled) state.transactions.filter { it.type.isLoan } else state.outstandingLoans).sortedByDescending { it.dateTimestamp }
    if (loans.isEmpty()) item { FinanceEmpty(stringResource(R.string.finance_owed_empty_title), stringResource(R.string.finance_owed_empty_hint)) }
    items(loans, key = { "loan_${it.id}" }) { loan ->
        val remaining = Ledger.outstanding(loan, state.transactions)
        var showRepayments by remember { mutableStateOf(false) }
        NeoColumnCard(Modifier.fillMaxWidth().padding(end = 3.dp, bottom = 3.dp), backgroundColor = NeoColors.Surface,
            shape = RoundedCornerShape(4.dp), contentPadding = 10.dp) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(loan.person, style = NeoTypography.titleLarge, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                Text(if (remaining == 0L) stringResource(R.string.loan_status_repaid) else if (loan.type == TransactionType.LEND) stringResource(R.string.loan_status_lent) else stringResource(R.string.loan_status_borrowed),
                    style = NeoTypography.labelSmall, fontWeight = FontWeight.Black,
                    modifier = Modifier.background(if (remaining == 0L) NeoColors.MintGreen else if (loan.type == TransactionType.LEND) NeoColors.Cyan else NeoColors.Yellow)
                        .border(2.dp, NeoColors.Border).padding(horizontal = 8.dp, vertical = 6.dp))
            }
            Text(if (remaining == 0L) stringResource(R.string.loan_fully_repaid) else if (loan.type == TransactionType.LEND) stringResource(R.string.loan_owes_you_format, Money.format(remaining)) else stringResource(R.string.loan_you_owe_format, Money.format(remaining)), style = NeoTypography.titleMedium)
            Text(stringResource(if (loan.type == TransactionType.LEND) R.string.loan_lent_detail else R.string.loan_borrowed_detail, Money.format(loan.amountMinor), DateUtils.formatTimestamp(loan.dateTimestamp, "dd MMM yyyy")), style = NeoTypography.bodySmall)
            if (loan.note.isNotBlank()) Text(loan.note, style = NeoTypography.bodyMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onEdit(loan) }) { Text(stringResource(R.string.action_edit), color = NeoColors.OnSurface) }
                if (remaining > 0) TextButton(onClick = { onRepay(loan) }) { Text(stringResource(R.string.repayment_record), color = NeoColors.OnSurface) }
            }
            val repayments = state.transactions.filter { it.loanId == loan.id }.sortedByDescending { it.dateTimestamp }
            if (repayments.isNotEmpty()) TextButton(onClick = { showRepayments = !showRepayments }) {
                Text(if (showRepayments) stringResource(R.string.repayment_hide) else stringResource(R.string.repayment_count_format, repayments.size), color = NeoColors.OnSurface)
            }
            if (showRepayments) repayments.forEach { payment ->
                TextButton(onClick = { onEdit(payment) }) {
                    Text(stringResource(R.string.repayment_detail_format, Money.format(payment.amountMinor), DateUtils.formatTimestamp(payment.dateTimestamp, "dd MMM")), color = NeoColors.OnSurface)
                }
            }
        }
    }
}

@Composable
private fun EntryFilterMenu(state: ExpenseUiState, filter: String, onFilter: (String) -> Unit, onAccount: (Long?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val active = filter != "All" || state.selectedAccountId != null
    val shape = RoundedCornerShape(4.dp)
    Box {
        NeoButton(stringResource(R.string.finance_filter), { expanded = true }, backgroundColor = if (active) NeoColors.Cyan else NeoColors.Surface,
            contentColor = NeoColors.OnSurface, shape = shape, shadowOffset = 3.dp,
            horizontalPadding = 12.dp, verticalPadding = 8.dp,
            leadingIcon = { Icon(Icons.Default.FilterList, null, modifier = Modifier.size(16.dp)) })
        DropdownMenu(expanded, { expanded = false }, shape = shape, containerColor = NeoColors.Surface,
            tonalElevation = 0.dp, shadowElevation = 0.dp,
            modifier = Modifier.widthIn(min = 224.dp, max = 300.dp)
                .neoHardShadow(shape = shape, offsetX = 5.dp, offsetY = 5.dp)
                .background(NeoColors.Surface, shape)
                .border(3.dp, NeoColors.Border, shape)) {
            Text(stringResource(R.string.finance_entry_type), style = NeoTypography.labelSmall, fontWeight = FontWeight.Black,
                color = NeoColors.OnSurface, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            listOf("All", "Expenses", "Income", "Transfers").forEach { option ->
                FilterMenuOption(if (option == "All") stringResource(R.string.finance_all_entries) else entryFilterLabel(option), filter == option,
                    { onFilter(option); expanded = false })
            }
            if (state.accounts.size > 1) {
                HorizontalDivider(thickness = 3.dp, color = NeoColors.Border, modifier = Modifier.padding(vertical = 8.dp))
                Text(stringResource(R.string.finance_account_heading), style = NeoTypography.labelSmall, fontWeight = FontWeight.Black,
                    color = NeoColors.OnSurface, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                FilterMenuOption(stringResource(R.string.finance_all_accounts), state.selectedAccountId == null, { onAccount(null); expanded = false })
                state.accounts.forEach { account -> FilterMenuOption(account.name, state.selectedAccountId == account.id,
                    { onAccount(account.id); expanded = false }) }
            }
        }
    }
}

@Composable
private fun FilterMenuOption(label: String, isSelected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, style = NeoTypography.bodyMedium, color = NeoColors.OnSurface,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium) },
        leadingIcon = { if (isSelected) Icon(Icons.Default.Check, null, tint = NeoColors.OnSurface, modifier = Modifier.size(16.dp))
            else Spacer(Modifier.size(16.dp)) },
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            .background(if (isSelected) NeoColors.Cyan else NeoColors.Surface)
            .then(if (isSelected) Modifier.border(2.dp, NeoColors.Border) else Modifier)
            .semantics { selected = isSelected }
    )
}

@Composable
private fun FinanceSummaryCard(title: String, background: Color, accent: Color, content: @Composable ColumnScope.() -> Unit) {
    NeoCard(Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 4.dp), backgroundColor = background,
        shape = RoundedCornerShape(4.dp), borderWidth = 3.dp, shadowOffsetX = 6.dp, shadowOffsetY = 6.dp, contentPadding = 3.dp) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().background(NeoColors.Ink).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = NeoTypography.labelSmall, fontWeight = FontWeight.Black, letterSpacing = 1.sp,
                    color = NeoColors.White, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(3) { Box(Modifier.size(width = 4.dp, height = 16.dp).rotate(20f).background(accent)) }
                }
            }
            content()
        }
    }
}

@Composable
private fun SummaryAmount(value: Long) {
    val amount = Money.format(value)
    Text(amount, style = NeoTypography.headlineMedium, fontSize = if (amount.length <= 12) 28.sp else 22.sp,
        lineHeight = if (amount.length <= 12) 34.sp else 28.sp, fontWeight = FontWeight.Black, color = NeoColors.OnSurface)
}

@Composable
private fun FinanceEmpty(title: String, hint: String) {
    NeoColumnCard(Modifier.fillMaxWidth(), backgroundColor = NeoColors.Surface, contentPadding = 12.dp) {
        Text(title, style = NeoTypography.titleMedium)
        Text(hint, style = NeoTypography.bodyMedium)
    }
}

@Composable
internal fun CategorySpendingRow(category: CategoryBreakdown, total: Long) {
    val fraction = if (total > 0) (category.totalMinor.toDouble() / total).toFloat().coerceIn(0f, 1f) else 0f
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(category.name, style = NeoTypography.titleMedium, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.finance_category_amount_percent, Money.format(category.totalMinor), (fraction * 100).toInt()), style = NeoTypography.bodyMedium)
        }
        Box(Modifier.fillMaxWidth().height(12.dp).background(NeoColors.Concrete).border(2.dp, NeoColors.Border)) {
            if (fraction > 0) Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(category.colorHex.toComposeColor()))
        }
    }
}

@Composable
private fun FinanceEntryRow(entry: Expense, accounts: List<Account>, onEdit: () -> Unit, showDate: Boolean = false) {
    val account = accounts.firstOrNull { it.id == entry.accountId }?.name ?: stringResource(R.string.account_label)
    val title = if (entry.type == TransactionType.TRANSFER) stringResource(R.string.transaction_transfer_accounts, account, accounts.firstOrNull { it.id == entry.toAccountId }?.name ?: stringResource(R.string.account_label)) else entry.categoryName
    NeoColumnCard(Modifier.fillMaxWidth().testTag("transaction_${entry.id}").clickable(onClick = onEdit), backgroundColor = NeoColors.Surface, contentPadding = 12.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ColorDot(entry.categoryColorHex, size = 12)
            Column(Modifier.weight(1f)) {
                Text(title, style = NeoTypography.titleMedium)
                Text(if (showDate) DateUtils.formatTimestamp(entry.dateTimestamp, "dd MMM yyyy") else account, style = NeoTypography.bodySmall)
                if (entry.note.isNotBlank()) Text(entry.note, style = NeoTypography.bodyMedium)
            }
            Text((if (entry.type == TransactionType.INCOME) "+" else if (entry.type == TransactionType.EXPENSE) "−" else "") + Money.format(entry.amountMinor),
                style = NeoTypography.titleMedium, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun entryFilterLabel(key: String): String = stringResource(when (key) {
    "Expenses" -> R.string.expense_title
    "Income" -> R.string.transaction_type_income
    "Transfers" -> R.string.finance_transfers
    else -> R.string.finance_all_entries
})
