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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
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
import com.vibecheck.lifepulse.ui.neobrutalism.*
import java.time.format.DateTimeFormatter

internal enum class EntryFilter(@androidx.annotation.StringRes val labelRes: Int, val type: TransactionType? = null) {
    ALL(R.string.finance_all_entries),
    EXPENSES(R.string.expense_title, TransactionType.EXPENSE),
    INCOME(R.string.transaction_type_income, TransactionType.INCOME),
    TRANSFERS(R.string.finance_transfers, TransactionType.TRANSFER)
}

internal fun LazyListScope.spendingSection(
    state: ExpenseUiState, filter: EntryFilter, onFilter: (EntryFilter) -> Unit,
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
        if (filter != EntryFilter.ALL || accountName != null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(listOfNotNull(if (filter != EntryFilter.ALL) stringResource(filter.labelRes) else null, accountName).joinToString(" · "), modifier = Modifier.weight(1f), style = NeoTypography.bodySmall)
                TextButton(onClick = { onFilter(EntryFilter.ALL); onAccount(null) }) { Text(stringResource(R.string.action_clear), color = NeoColors.OnSurface) }
            }
        } else Text(stringResource(R.string.finance_edit_hint), style = NeoTypography.bodySmall)
    }
    val groups = state.expensesByDay.map { group -> group.copy(expenses = group.expenses.filter {
        (it.type.needsCategory || it.type == TransactionType.TRANSFER) &&
            (filter == EntryFilter.ALL || it.type == filter.type)
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
    NeoFeatureCard(stringResource(R.string.finance_monthly_spending), NeoColors.Expense, headerAction = {
        TextButton(onClick = onCalendar, contentPadding = PaddingValues(4.dp)) {
            Icon(Icons.Default.CalendarMonth, null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(state.selectedMonth.format(DateTimeFormatter.ofPattern("MMM yyyy")), style = NeoTypography.labelMedium)
        }
    }) {
        val amount = Money.format(state.spentMinor)
        val insightLabel = stringResource(R.string.insights_title)
        val measurer = rememberTextMeasurer()
        val actionWidth = measurer.measure(AnnotatedString(insightLabel), NeoTypography.labelLarge, softWrap = false).size.width
        val amountWidth = measurer.measure(AnnotatedString(amount), NeoTypography.headlineLarge, softWrap = false).size.width
        val gap = with(LocalDensity.current) { 40.dp.roundToPx() }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (amountWidth + actionWidth + gap <= constraints.maxWidth) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(amount, style = NeoTypography.headlineLarge, modifier = Modifier.weight(1f))
                    InsightAction(insightLabel, onBreakdown)
                }
            } else Column {
                SummaryAmount(state.spentMinor)
                InsightAction(insightLabel, onBreakdown)
            }
        }
        HorizontalDivider(color = NeoColors.Ink)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.finance_spent_today), style = NeoTypography.bodySmall)
                Text(Money.format(state.totalToday), style = NeoTypography.titleMedium)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.finance_month_income), style = NeoTypography.bodySmall)
                Text(Money.format(state.incomeMinor), style = NeoTypography.titleMedium)
            }
        }
    }
}

@Composable
private fun InsightAction(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 0.dp)) {
        Text(label, style = NeoTypography.labelLarge)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
    }
}

internal fun LazyListScope.accountsSection(state: ExpenseUiState, onAccount: (Account?) -> Unit,
                                          onCurrency: () -> Unit, onEdit: (Expense) -> Unit) {
    item {
        NeoFeatureCard(stringResource(R.string.finance_total_balance), NeoColors.Lime) {
            SummaryAmount(state.balances.values.sum())
            Text(stringResource(R.string.finance_balance_hint), style = NeoTypography.bodySmall,
                modifier = Modifier.fillMaxWidth())
        }
    }
    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.finance_your_accounts), style = NeoTypography.titleLarge)
            NeoIconButton(Icons.Default.Add, stringResource(R.string.account_add), { onAccount(null) }, size = 40.dp, shadowOffset = 3.dp)
        }
    }
    items(state.accounts, key = { "account_${it.id}" }) { account ->
        NeoCard(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onAccount(account) },
            backgroundColor = NeoColors.PaleCyan, shape = RoundedCornerShape(8.dp), borderWidth = 2.dp,
            shadowOffsetX = 0.dp, shadowOffsetY = 0.dp, contentPadding = 4.dp) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    NeoValueRow(account.name, Money.format(state.balances[account.id] ?: account.openingMinor))
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
        NeoFeatureCard(stringResource(R.string.finance_owed_heading), NeoColors.LightViolet) {
            NeoValueRow(stringResource(R.string.finance_people_owe_you), Money.format(state.outstandingLoans.filter { it.type == TransactionType.LEND }
                .sumOf { Ledger.outstanding(it, state.transactions) }))
            NeoValueRow(stringResource(R.string.finance_you_owe_people), Money.format(state.outstandingLoans.filter { it.type == TransactionType.BORROW }
                .sumOf { Ledger.outstanding(it, state.transactions) }))
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
                    modifier = Modifier.background(if (remaining == 0L) NeoColors.Success else NeoColors.BubblegumPink)
                        .border(2.dp, NeoColors.Border).padding(horizontal = 8.dp, vertical = 6.dp))
            }
            Text(if (remaining == 0L) stringResource(R.string.loan_fully_repaid) else stringResource(
                if (loan.type == TransactionType.LEND) R.string.loan_owes_you_format else R.string.loan_you_owe_format,
                Money.format(remaining)), style = NeoTypography.titleMedium)
            Text(stringResource(if (loan.type == TransactionType.LEND) R.string.loan_lent_detail else R.string.loan_borrowed_detail,
                Money.format(loan.amountMinor), DateUtils.formatTimestamp(loan.dateTimestamp, "dd MMM yyyy")), style = NeoTypography.bodySmall)
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
private fun EntryFilterMenu(state: ExpenseUiState, filter: EntryFilter, onFilter: (EntryFilter) -> Unit, onAccount: (Long?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val active = filter != EntryFilter.ALL || state.selectedAccountId != null
    val shape = RoundedCornerShape(4.dp)
    Box {
        NeoButton(stringResource(R.string.finance_filter), { expanded = true }, backgroundColor = if (active) NeoColors.Primary else NeoColors.Surface,
            contentColor = NeoColors.OnSurface, shape = shape, shadowOffset = 3.dp,
            horizontalPadding = 12.dp, verticalPadding = 8.dp,
            leadingIcon = { Icon(Icons.Default.FilterList, null, modifier = Modifier.size(16.dp)) })
        DropdownMenu(expanded, { expanded = false }, shape = shape, containerColor = NeoColors.Surface,
            tonalElevation = 0.dp, shadowElevation = 0.dp,
            modifier = Modifier.widthIn(min = 224.dp, max = 300.dp)
                .neoHardShadow(shape = shape, offsetX = 5.dp, offsetY = 5.dp)
                .background(NeoColors.Surface, shape)
                .border(2.dp, NeoColors.Border, shape)) {
            Text(stringResource(R.string.finance_entry_type), style = NeoTypography.labelSmall, fontWeight = FontWeight.Black,
                color = NeoColors.OnSurface, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            EntryFilter.entries.forEach { option ->
                FilterMenuOption(stringResource(option.labelRes), filter == option,
                    { onFilter(option); expanded = false })
            }
            if (state.accounts.size > 1) {
                HorizontalDivider(thickness = 1.dp, color = NeoColors.Border, modifier = Modifier.padding(vertical = 8.dp))
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
            .background(if (isSelected) NeoColors.Primary else NeoColors.Surface)
            .then(if (isSelected) Modifier.border(2.dp, NeoColors.Border) else Modifier)
            .semantics { selected = isSelected }
    )
}

@Composable
private fun SummaryAmount(value: Long) {
    Text(Money.format(value), style = NeoTypography.headlineLarge, color = NeoColors.OnSurface, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun FinanceEmpty(title: String, hint: String) {
    NeoCard(Modifier.fillMaxWidth(), backgroundColor = NeoColors.Lavender) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            NeoBurst(color = NeoColors.BubblegumPink)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = NeoTypography.titleMedium)
                Text(hint, style = NeoTypography.bodyMedium)
            }
        }
    }
}

@Composable
private fun FinanceEntryRow(entry: Expense, accounts: List<Account>, onEdit: () -> Unit, showDate: Boolean = false) {
    val account = accounts.firstOrNull { it.id == entry.accountId }?.name ?: stringResource(R.string.account_label)
    val title = if (entry.type == TransactionType.TRANSFER) stringResource(R.string.transaction_transfer_accounts,
        account, accounts.firstOrNull { it.id == entry.toAccountId }?.name ?: stringResource(R.string.account_label)) else entry.categoryName
    NeoColumnCard(Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("transaction_${entry.id}").clickable(onClick = onEdit),
        backgroundColor = if (entry.type == TransactionType.INCOME) NeoColors.PaleCyan else NeoColors.Surface, contentPadding = 10.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ColorDot(entry.categoryColorHex, size = 12)
            Column(Modifier.weight(1f)) {
                NeoValueRow(title, (if (entry.type == TransactionType.INCOME) "+" else if (entry.type == TransactionType.EXPENSE) "−" else "") + Money.format(entry.amountMinor))
                Text(if (showDate) DateUtils.formatTimestamp(entry.dateTimestamp, "dd MMM yyyy") else account, style = NeoTypography.bodySmall)
                if (entry.note.isNotBlank()) Text(entry.note, style = NeoTypography.bodyMedium)
            }
        }
    }
}
