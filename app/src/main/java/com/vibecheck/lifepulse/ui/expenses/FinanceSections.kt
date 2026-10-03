package com.vibecheck.lifepulse.ui.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    item {
        NeoColumnCard(Modifier.fillMaxWidth(), backgroundColor = NeoColors.PastelYellow) {
            TextButton(onClick = onCalendar, contentPadding = PaddingValues(0.dp)) {
                Text(state.selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")) + " ▾", style = NeoTypography.titleMedium, color = NeoColors.OnSurface)
            }
            Text("You spent", style = NeoTypography.bodyMedium)
            Text(Money.format(state.spentMinor), style = NeoTypography.headlineMedium, fontWeight = FontWeight.Black)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Today ${Money.format(state.totalToday)}", style = NeoTypography.bodySmall)
                Text("Income ${Money.format(state.incomeMinor)}", style = NeoTypography.bodySmall)
            }
            TextButton(onClick = onBreakdown) { Text("See category breakdown →", style = NeoTypography.labelLarge, color = NeoColors.OnSurface) }
        }
    }
    item {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Your entries", style = NeoTypography.titleLarge)
            EntryFilterMenu(state, filter, onFilter, onAccount)
        }
        val accountName = state.accounts.firstOrNull { it.id == state.selectedAccountId }?.name
        if (filter != "All" || accountName != null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(listOfNotNull(if (filter != "All") filter else null, accountName).joinToString(" · "), modifier = Modifier.weight(1f), style = NeoTypography.bodySmall)
                TextButton(onClick = { onFilter("All"); onAccount(null) }) { Text("Clear", color = NeoColors.OnSurface) }
            }
        } else Text("Tap an entry to edit", style = NeoTypography.bodySmall)
    }
    val groups = state.expensesByDay.map { group -> group.copy(expenses = group.expenses.filter {
        (it.type.needsCategory || it.type == TransactionType.TRANSFER) &&
            (filter == "All" || (filter == "Expenses" && it.type == TransactionType.EXPENSE) ||
            (filter == "Income" && it.type == TransactionType.INCOME) || (filter == "Transfers" && it.type == TransactionType.TRANSFER))
    }) }.filter { it.expenses.isNotEmpty() }
    if (state.isLoading) item { Text("Loading your entries…") }
    else if (groups.isEmpty()) item { FinanceEmpty("A fresh start", "No entries here yet. Tap Add entry to get started.") }
    groups.forEach { group ->
        item(key = "date_${group.date}") { Text(group.date.format(DateTimeFormatter.ofPattern("EEE, dd MMM")), style = NeoTypography.labelLarge) }
        items(group.expenses, key = { it.id }) { FinanceEntryRow(it, state.accounts, { onEdit(it) }) }
    }
}

internal fun LazyListScope.accountsSection(state: ExpenseUiState, onAccount: (Account?) -> Unit,
                                          onCurrency: () -> Unit, onEdit: (Expense) -> Unit) {
    item {
        NeoColumnCard(Modifier.fillMaxWidth(), backgroundColor = NeoColors.MintGreen) {
            MoneyStat("Money in your accounts", state.balances.values.sum())
            Text("Based on the entries you've recorded", style = NeoTypography.bodySmall)
        }
    }
    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Your accounts", style = NeoTypography.titleLarge)
            NeoIconButton(Icons.Default.Add, "Add account", { onAccount(null) }, size = 40.dp, shadowOffset = 3.dp)
        }
    }
    items(state.accounts, key = { "account_${it.id}" }) { account ->
        NeoColumnCard(Modifier.fillMaxWidth().clickable { onAccount(account) }, backgroundColor = NeoColors.Surface) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(account.name, style = NeoTypography.titleMedium)
                    Text(Money.format(state.balances[account.id] ?: account.openingMinor), style = NeoTypography.titleLarge, fontWeight = FontWeight.Black)
                }
                Icon(Icons.Default.Edit, "Edit account", modifier = Modifier.size(16.dp), tint = NeoColors.OnSurface)
            }
        }
    }
    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Currency · ${Money.currencyCode}", style = NeoTypography.bodyMedium)
            TextButton(onClick = onCurrency) { Text("Change", color = NeoColors.OnSurface) }
        }
        Text("Transfers", style = NeoTypography.titleLarge)
    }
    val transfers = state.transactions.filter { it.type == TransactionType.TRANSFER }.sortedByDescending { it.dateTimestamp }
    if (transfers.isEmpty()) item { Text("Transfers will appear here. They don't count as spending.", style = NeoTypography.bodyMedium) }
    items(transfers, key = { "transfer_${it.id}" }) { FinanceEntryRow(it, state.accounts, { onEdit(it) }, showDate = true) }
}

internal fun LazyListScope.owedSection(state: ExpenseUiState, showSettled: Boolean, onSettled: (Boolean) -> Unit,
                                      onEdit: (Expense) -> Unit, onRepay: (Expense) -> Unit) {
    item {
        NeoColumnCard(Modifier.fillMaxWidth(), backgroundColor = NeoColors.PaleCyan) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) { MoneyStat("People owe you", state.outstandingLoans.filter { it.type == TransactionType.LEND }.sumOf { Ledger.outstanding(it, state.transactions) }) }
                Column(Modifier.weight(1f)) { MoneyStat("You owe people", state.outstandingLoans.filter { it.type == TransactionType.BORROW }.sumOf { Ledger.outstanding(it, state.transactions) }) }
            }
        }
    }
    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Show repaid loans", style = NeoTypography.bodyMedium)
            Checkbox(showSettled, onSettled, colors = CheckboxDefaults.colors(checkedColor = NeoColors.OnSurface, checkmarkColor = NeoColors.Surface, uncheckedColor = NeoColors.Border))
        }
    }
    val loans = (if (showSettled) state.transactions.filter { it.type.isLoan } else state.outstandingLoans).sortedByDescending { it.dateTimestamp }
    if (loans.isEmpty()) item { FinanceEmpty("All clear", "Record money you lend or borrow, then track repayments here.") }
    items(loans, key = { "loan_${it.id}" }) { loan ->
        val remaining = Ledger.outstanding(loan, state.transactions)
        var showRepayments by remember { mutableStateOf(false) }
        NeoColumnCard(Modifier.fillMaxWidth(), backgroundColor = NeoColors.Surface) {
            Text(loan.person, style = NeoTypography.titleLarge)
            Text(if (remaining == 0L) "Fully repaid" else if (loan.type == TransactionType.LEND) "Owes you ${Money.format(remaining)}" else "You owe ${Money.format(remaining)}", style = NeoTypography.titleMedium)
            Text("${Money.format(loan.amountMinor)} ${if (loan.type == TransactionType.LEND) "lent" else "borrowed"} · ${DateUtils.formatTimestamp(loan.dateTimestamp, "dd MMM yyyy")}", style = NeoTypography.bodySmall)
            if (loan.note.isNotBlank()) Text(loan.note, style = NeoTypography.bodyMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onEdit(loan) }) { Text("Edit", color = NeoColors.OnSurface) }
                if (remaining > 0) TextButton(onClick = { onRepay(loan) }) { Text("Record repayment", color = NeoColors.OnSurface) }
            }
            val repayments = state.transactions.filter { it.loanId == loan.id }.sortedByDescending { it.dateTimestamp }
            if (repayments.isNotEmpty()) TextButton(onClick = { showRepayments = !showRepayments }) {
                Text(if (showRepayments) "Hide repayments" else "Repayments (${repayments.size})", color = NeoColors.OnSurface)
            }
            if (showRepayments) repayments.forEach { payment ->
                TextButton(onClick = { onEdit(payment) }) {
                    Text("${Money.format(payment.amountMinor)} repaid · ${DateUtils.formatTimestamp(payment.dateTimestamp, "dd MMM")} · Edit", color = NeoColors.OnSurface)
                }
            }
        }
    }
}

@Composable
private fun EntryFilterMenu(state: ExpenseUiState, filter: String, onFilter: (String) -> Unit, onAccount: (Long?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Icon(Icons.Default.FilterList, null, modifier = Modifier.size(16.dp), tint = NeoColors.OnSurface)
            Spacer(Modifier.width(6.dp))
            Text("Filter", color = NeoColors.OnSurface)
        }
        DropdownMenu(expanded, { expanded = false }, modifier = Modifier.background(NeoColors.Surface).border(2.dp, NeoColors.Border)) {
            listOf("All", "Expenses", "Income", "Transfers").forEach { option ->
                DropdownMenuItem(text = { Text((if (filter == option) "✓ " else "") + if (option == "All") "All entries" else option) },
                    onClick = { onFilter(option); expanded = false })
            }
            if (state.accounts.size > 1) {
                HorizontalDivider(color = NeoColors.Border)
                DropdownMenuItem(text = { Text("All accounts") }, onClick = { onAccount(null); expanded = false })
                state.accounts.forEach { account -> DropdownMenuItem(text = { Text(account.name) }, onClick = { onAccount(account.id); expanded = false }) }
            }
        }
    }
}

@Composable
private fun MoneyStat(label: String, amount: Long) {
    Text(label, style = NeoTypography.bodyMedium)
    Text(Money.format(amount), style = NeoTypography.titleLarge, fontWeight = FontWeight.Black)
}

@Composable
private fun FinanceEmpty(title: String, hint: String) {
    NeoColumnCard(Modifier.fillMaxWidth(), backgroundColor = NeoColors.Surface) {
        Text(title, style = NeoTypography.titleLarge)
        Text(hint, style = NeoTypography.bodyMedium)
    }
}

@Composable
internal fun CategorySpendingRow(category: CategoryBreakdown, total: Long) {
    val fraction = if (total > 0) (category.totalMinor.toDouble() / total).toFloat().coerceIn(0f, 1f) else 0f
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(category.name, style = NeoTypography.titleMedium, modifier = Modifier.weight(1f))
            Text("${Money.format(category.totalMinor)} · ${(fraction * 100).toInt()}%", style = NeoTypography.bodyMedium)
        }
        Box(Modifier.fillMaxWidth().height(12.dp).background(NeoColors.Concrete).border(2.dp, NeoColors.Border)) {
            if (fraction > 0) Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(category.colorHex.toComposeColor()))
        }
    }
}

@Composable
private fun FinanceEntryRow(entry: Expense, accounts: List<Account>, onEdit: () -> Unit, showDate: Boolean = false) {
    val account = accounts.firstOrNull { it.id == entry.accountId }?.name ?: "Account"
    val title = if (entry.type == TransactionType.TRANSFER) "$account → ${accounts.firstOrNull { it.id == entry.toAccountId }?.name ?: "Account"}" else entry.categoryName
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
